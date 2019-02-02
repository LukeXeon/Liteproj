package org.kexie.android.liteporj;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

final class AnalyzerEnv
{
    private interface TypeConverter
    {
        @NonNull
        Object castTo(@NonNull Object obj);
    }

    private interface Filter<T>
    {
        boolean filter(@NonNull T item);
    }

    private interface TextConverter
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    private static final Map<Pair<Class<?>, Class<?>>, TypeConverter>
            sTypeConverters = newTypeConverters();

    private Node mCurrent;

    private final Provider mProxyProvider;

    private final Map<String, Provider> mProviders = new ArrayMap<>();

    private final List<TextConverter> mTextConverters;

    private final Pattern mNamePattern = Pattern
            .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");

    private final Filter<Method> mPropertyFilter = new Filter<Method>()
    {
        @Override
        public boolean filter(@NonNull Method item)
        {
            return void.class.equals(item.getReturnType())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    private final Filter<Method> mFactoryFilter = new Filter<Method>()
    {
        @Override
        public boolean filter(@NonNull Method item)
        {
            return Modifier.isStatic(item.getModifiers())
                    && !void.class
                    .equals(item.getReturnType());
        }
    };

    private final Filter<Field> mFieldFilter = new Filter<Field>()
    {
        @Override
        public boolean filter(@NonNull Field item)
        {
            return !Modifier.isFinal(item.getModifiers())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    //实例包内

    Dependency makeResult()
    {
        return new Dependency(mProxyProvider.getResultType(), mProviders);
    }

    AnalyzerEnv(@NonNull final Class<?> ownerType, @NonNull Node node)
    {
        this.mProxyProvider = new Provider()
        {
            @NonNull
            @Override
            public <T> T provide(@NonNull DependencyManager dependencyManager)
            {
                throw new IllegalStateException(
                        "Objects cannot be generated " +
                                "from proxy providers");
            }

            @NonNull
            @Override
            public DependencyType getType()
            {
                return DependencyType.SINGLETON;
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return ownerType;
            }
        };
        this.mTextConverters = newTextConverters();
        mark(node);
    }

    void mark(@NonNull Node currentNode)
    {
        this.mCurrent = currentNode;
    }


    @NonNull
    Class<?> getResultTypeIfNullThrow(@NonNull String name)
    {
        Provider provider = mProviders.get(name);
        if (provider != null)
        {
            return provider.getResultType();
        } else
        {
            throw fromMessageThrow(String.format("no found name by %s provider", name));
        }
    }

    @NonNull
    TextType getTextType(@NonNull String text)
    {
        if (TextUtils.isEmpty(text))
        {
            return TextType.ILLEGAL;
        } else
        {
            if ((text.charAt(0) == '@')
                    && !TextUtils.isEmpty(text.substring(1, text.length())))
            {
                return TextType.CONSTANT;
            } else if (mNamePattern
                    .matcher(text)
                    .matches())
            {
                return TextType.REFERENCE;
            } else
            {
                return TextType.ILLEGAL;
            }
        }
    }

    @NonNull
    Provider newConstantProvider(@NonNull String let)
    {
        String value = let.substring(1, let.length());
        if (let.charAt(0) == '@')
        {
            return new DependencyProvider(
                    DependencyType.SINGLETON,
                    newSingleton(value),
                    Collections.<Setter>emptyList()
            );
        } else
        {
            for (TextConverter valueOf : mTextConverters)
            {
                try
                {
                    return new DependencyProvider(
                            DependencyType.SINGLETON,
                            newSingleton(valueOf.valueOf(value)),
                            Collections.<Setter>emptyList()
                    );
                } catch (NumberFormatException ignored)
                {

                }
            }
            throw new IllegalStateException(
                    String.format(
                            "The name %s does not match the rule of let"
                            , let));
        }
    }

    @NonNull
    Method findProperty(@NonNull Class<?> clazz,
                        @NonNull String name,
                        @Nullable Class<?> sClass)
    {
        return findMethod(clazz, "set"
                        + Character.toUpperCase(name.charAt(0))
                        + name.substring(1), new Class<?>[]{sClass},
                mPropertyFilter);
    }

    @NonNull
    Method findFactory(@NonNull Class<?> clazz,
                       @NonNull String name,
                       @Nullable Class<?>[] sClasses)
    {
        return findMethod(clazz, name, sClasses, mFactoryFilter);
    }

    @NonNull
    Field findField(@NonNull Class<?> clazz,
                    @NonNull String name,
                    @NonNull Class<?> sClass)
    {
        return findField(clazz, name, sClass, mFieldFilter);
    }

    @Nullable
    Provider getProvider(@NonNull String name)
    {
        if (DependencyManager.OWNER.equals(name))
        {
            return mProxyProvider;
        }
        return mProviders.get(name);
    }


    void addProvider(@NonNull String name, @NonNull Provider provider)
    {
        if (!DependencyManager.OWNER.equals(name)
                && !mProviders.containsKey(name))
        {
            mProviders.put(name, provider);
        } else
        {
            throw fromMessageThrow(
                    String.format("The provider named %s already exists",
                            name)
            );
        }
    }

    @Nullable
    String getAttrNoThrow(@NonNull Element element,
                          @NonNull String attr)
    {
        mark(element);
        if (element.attributeCount() != 0)
        {
            Attribute attribute = element.attribute(attr);
            if (attribute != null)
            {
                return attribute.getValue();
            }
        }
        return null;
    }

    @NonNull
    Constructor<?> findConstructor(@NonNull Class<?> clazz,
                                   @Nullable Class<?>[] classes)
    {
        return findConstructor(clazz, classes, null);
    }

    @NonNull
    String getAttrIfEmptyThrow(@NonNull Element element,
                               @NonNull String attr)
    {
        mark(element);
        String name = getAttrNoThrow(element, attr);
        if (!TextUtils.isEmpty(name))
        {
            return name;
        }
        throw fromMessageThrow(String.format("Attr %s no found", name));
    }

    @NonNull
    RuntimeException fromMessageThrow(@NonNull String massage)
    {
        return new GenerateDepartmentException(String.format(
                "Error in %s ", mCurrent.asXML())
                + (TextUtils.isEmpty(massage)
                ? ""
                : String.format(", message = %s", massage)));
    }

    @NonNull
    RuntimeException
    formExceptionThrow(@NonNull Throwable e)
    {
        return new GenerateDepartmentException(
                String.format("Error in %s\n ",
                        mCurrent.asXML()), e);
    }

    //静态包内

    static void inject(@NonNull Object object, @NonNull DependencyManager dependency)
    {
        Class<?> type = object.getClass();
        Set<Class<?>> baseTypes = new ArraySet<>();
        baseTypes.add(FragmentActivity.class);
        baseTypes.add(Fragment.class);
        baseTypes.add(Application.class);
        baseTypes.add(LiteService.class);
        baseTypes.add(LiteViewModel.class);
        while (type != null && !baseTypes.contains(type))
        {
            for (Field field : type.getDeclaredFields())
            {
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null)
                {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isFinal(modifiers)
                            && !Modifier.isStatic(modifiers)
                            && isAssignTo(
                            dependency.getResultType(reference.value()),
                            field.getType()))
                    {
                        field.setAccessible(true);
                        try
                        {
                            field.set(object, castTo(
                                    dependency.get(reference.value()),
                                    field.getType()));
                        } catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            for (Method property : type.getDeclaredMethods())
            {
                Reference reference = property.getAnnotation(Reference.class);
                if (reference != null)
                {
                    int modifiers = property.getModifiers();
                    String name = property.getName();
                    Class<?>[] parameterTypes = property.getParameterTypes();
                    if (name.length() >= 3
                            && "set".equals(name.substring(0, 2))
                            && parameterTypes.length == 1
                            && !Modifier.isStatic(modifiers)
                            && !Modifier.isAbstract(modifiers)
                            && isAssignTo(dependency
                                    .getResultType(reference.value()),
                            parameterTypes[0]))
                    {
                        property.setAccessible(true);
                        try
                        {
                            property.invoke(object,
                                    castTo(dependency.get(reference.value()),
                                            parameterTypes[0]));
                        } catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            type = type.getSuperclass();
        }
    }

    @Nullable
    static int[] getResIds(@NonNull Object owner)
    {
        Using using = owner.getClass().getAnnotation(Using.class);
        return using == null ? null : using.value();
    }

    static boolean listNoEmpty(@Nullable List<?> list)
    {
        return list != null && list.size() != 0;
    }

    @NonNull
    static Setter newSetter(@NonNull final Method method, @NonNull final String name)
    {
        return new Setter()
        {
            @Override
            public void set(@NonNull Object target, @NonNull DependencyManager dependency)
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(target, castTo(dependency.get(name),
                            method.getParameterTypes()[0]));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NonNull
    static Setter newSetter(@NonNull final Field field, @NonNull final String name)
    {
        return new Setter()
        {
            @Override
            public void set(@NonNull Object target, @NonNull DependencyManager dependency)
            {
                field.setAccessible(true);
                try
                {
                    field.set(target, castTo(dependency.get(name),
                            field.getType()));
                } catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NonNull
    @SuppressWarnings("unchecked")
    static Factory newFactory(@NonNull final Method method,
                              @NonNull final List<String> references)
    {
        return new Factory()
        {
            @NonNull
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependency)
            {
                method.setAccessible(true);
                try
                {
                    return (T) method.invoke(null,
                            getReferences(references,
                                    method.getParameterTypes(),
                                    dependency
                            )
                    );
                } catch (IllegalAccessException
                        | InvocationTargetException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return method.getReturnType();
            }
        };
    }

    @NonNull
    static Factory newFactory(@NonNull final Constructor<?> constructor,
                              @NonNull final List<String> references)
    {
        return new Factory()
        {
            @SuppressWarnings("unchecked")
            @NonNull
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependency)
            {
                constructor.setAccessible(true);
                try
                {
                    return (T) constructor.newInstance(
                            getReferences(
                                    references,
                                    constructor.getParameterTypes(),
                                    dependency
                            )
                    );
                } catch (IllegalAccessException
                        | InvocationTargetException
                        | InstantiationException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return constructor.getDeclaringClass();
            }

        };
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private static <T> T castTo(@NonNull Object obj,
                                @NonNull Class<T> targetClass)
    {
        //处理引用类型和可赋值类型
        Class<?> objClass = obj.getClass();
        if (targetClass.isAssignableFrom(objClass))
        {
            return (T) obj;
        }
        TypeConverter castTo = sTypeConverters.get(Pair.create(objClass, targetClass));
        if (castTo != null)
        {
            return (T) castTo.castTo(obj);
        }
        throw new ClassCastException(String.format("Can not cast %s to %s",
                objClass.getName(),
                targetClass.getName()));
    }

    private static boolean isAssignTo(@NonNull Class<?> objClass,
                                      @NonNull Class<?> targetClass)
    {
        if (targetClass.isAssignableFrom(objClass))
        {
            return true;
        }
        return sTypeConverters.get(Pair.create(objClass, targetClass)) != null;
    }

    //实例私有

    @NonNull
    private Field findField(@NonNull Class<?> clazz,
                            @NonNull String name,
                            @NonNull Class<?> sClass,
                            @Nullable Filter<Field> filter)
    {
        Field field;
        try
        {
            field = clazz.getField(name);
        } catch (NoSuchFieldException e)
        {
            throw formExceptionThrow(e);
        }
        if (isAssignTo(sClass, field.getType())
                && (filter == null || filter.filter(field)))
        {
            return field;
        } else
        {
            throw fromMessageThrow(String.format(
                    "Can't found field name by %s , can't cast %s to %s",
                    name,
                    sClass,
                    field.getType()));
        }
    }

    @NonNull
    private Method findMethod(@NonNull Class<?> clazz,
                              @NonNull String name,
                              @Nullable Class<?>[] sClasses,
                              @Nullable Filter<Method> filter)
    {
        if (sClasses == null)
        {
            try
            {
                return clazz.getMethod(name);
            } catch (NoSuchMethodException e)
            {
                throw formExceptionThrow(e);
            }
        }
        for (Method method : clazz.getMethods())
        {
            boolean match = true;
            Class<?>[] pram = method.getParameterTypes();
            if (method.getName().equals(name)
                    && pram.length == sClasses.length
                    && (filter == null || filter.filter(method)))
            {
                for (int i = 0; i < pram.length; i++)
                {
                    if (!isAssignTo(sClasses[i], pram[i]))
                    {
                        match = false;
                        break;
                    }
                }
            } else
            {
                match = false;
            }
            if (match)
            {
                return method;
            }
        }
        throw fromMessageThrow(String.format(
                "method whose name %s is not found in %s and requires %s",
                name,
                clazz,
                Arrays.toString(sClasses)));
    }

    @NonNull
    private Constructor<?> findConstructor(@NonNull Class<?> clazz,
                                           @Nullable Class<?>[] sClasses,
                                           @Nullable Filter<Constructor<?>> filter)
    {
        if (sClasses == null)
        {
            try
            {
                return clazz.getConstructor();
            } catch (NoSuchMethodException e)
            {
                throw formExceptionThrow(e);
            }
        }
        for (Constructor<?> constructor : clazz.getConstructors())
        {
            boolean match = true;
            Class<?>[] pram = constructor.getParameterTypes();
            if (pram.length == sClasses.length
                    && (filter == null || filter.filter(constructor)))
            {
                for (int i = 0; i < pram.length; i++)
                {
                    if (!isAssignTo(sClasses[i], pram[i]))
                    {
                        match = false;
                        break;
                    }
                }
            } else
            {
                match = false;
            }
            if (match)
            {
                return constructor;
            }
        }
        throw fromMessageThrow(String.format("Can't find constructor for %s in %s",
                Arrays.toString(sClasses),
                clazz));
    }


    //静态私有

    private static boolean isFloatType(@NonNull Class<?> type)
    {
        return Float.class.equals(type) || Double.class.equals(type);
    }

    @NonNull
    private static Map<Pair<Class<?>, Class<?>>, TypeConverter> newTypeConverters()
    {
        TypeConverter castToThis = new TypeConverter()
        {
            @NonNull
            @Override
            public Object castTo(@NonNull Object obj)
            {
                return obj;
            }
        };
        ArrayMap<Pair<Class<?>, Class<?>>, TypeConverter> result = new ArrayMap<>();
        result.put(Pair.<Class<?>, Class<?>>
                create(Boolean.class, Boolean.TYPE), castToThis);
        result.put(Pair.<Class<?>, Class<?>>
                create(Character.class, Character.TYPE), castToThis);
        Class<?>[] numberTypes = new Class<?>[]{
                Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class
        };
        for (Class<?> sourceType : numberTypes)
        {
            for (Class<?> targetType : numberTypes)
            {
                try
                {
                    final Class<?> rawTargetType = (Class<?>) targetType
                            .getField("TYPE")
                            .get(null);
                    if (sourceType.equals(targetType))
                    {
                        result.put(Pair.<Class<?>, Class<?>>
                                create(sourceType, rawTargetType), castToThis);
                        continue;
                    }
                    int sourceSize = sourceType.getField("SIZE").getInt(null);
                    int targetSize = targetType.getField("SIZE").getInt(null);
                    if (sourceSize < targetSize
                            && !(isFloatType(sourceType)
                            && !isFloatType(targetType)))
                    {
                        result.put(Pair.<Class<?>, Class<?>>
                                        create(sourceType, rawTargetType),
                                new TypeConverter()
                                {
                                    @NonNull
                                    @Override
                                    public Object castTo(@NonNull Object obj)
                                    {
                                        try
                                        {
                                            return obj.getClass().getMethod(
                                                    rawTargetType.getName()
                                                            + "Value").invoke(obj);
                                        } catch (Exception e)
                                        {
                                            throw new AssertionError(e);
                                        }
                                    }
                                });
                    }
                } catch (Exception e)
                {
                    throw new AssertionError(e);
                }
            }
        }
        return result;
    }

    @NonNull
    private static List<TextConverter> newTextConverters()
    {
        List<TextConverter> result = new LinkedList<>();
        result.add(new TextConverter()
        {
            @NonNull
            @Override
            public Object valueOf(@NonNull String value)
            {
                if (!Character.isDigit(value.charAt(0)))
                {
                    if (value.length() == 1)
                    {
                        return value.charAt(0);
                    } else if ("true".equals(value) || "false".equals(value))
                    {
                        return Boolean.valueOf(value);
                    }
                }
                throw new NumberFormatException();
            }
        });
        for (final Class<?> type : new Class<?>[]{Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class})
        {
            result.add(new TextConverter()
            {
                @NonNull
                @Override
                public Object valueOf(@NonNull String value)
                {
                    try
                    {
                        return type.getMethod("valueOf", String.class)
                                .invoke(null, value);
                    } catch (IllegalAccessException
                            | NoSuchMethodException e)
                    {
                        throw new AssertionError(e);
                    } catch (InvocationTargetException e)
                    {
                        if (e.getCause() instanceof NumberFormatException)
                        {
                            throw (NumberFormatException) e.getCause();
                        } else
                        {
                            throw new AssertionError(e);
                        }
                    }
                }
            });
        }
        return result;
    }

    @NonNull
    private static Object[] getReferences(@NonNull List<String> refs,
                                          @NonNull Class<?>[] targetClasses,
                                          @NonNull DependencyManager dependency)
    {
        Object[] args = new Object[refs.size()];
        for (int i = 0; i < refs.size(); i++)
        {
            String name = refs.get(i);
            args[i] = castTo(dependency.get(name), targetClasses[i]);
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    private static Factory newSingleton(Object object)
    {
        final Object nonNull = Objects.requireNonNull(object);
        return new Factory()
        {
            @NonNull
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependencyManager)
            {
                return (T) nonNull;
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return nonNull.getClass();
            }
        };
    }
}