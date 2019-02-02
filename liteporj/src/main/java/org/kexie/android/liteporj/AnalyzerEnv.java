package org.kexie.android.liteporj;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

final class AnalyzerEnv
{
    private interface TypeConverter
    {
        @NonNull
        Object castTo(@NonNull Object obj);
    }

    private interface TextConverter
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    private static final Map<Pair<Class<?>, Class<?>>, TypeConverter>
            sTypeConverters = newTypeConverters();

    private Node mCurrentNode;

    private final Provider mOwnerProxy;

    private final Map<String, Provider> mProviders = new ArrayMap<>();

    private final List<TextConverter> mTextConverters;

    private final Pattern mNamePattern = Pattern
            .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");

    private final Filter<Method> mPropertyFilter = new Filter<Method>()
    {
        @Override
        public boolean filter(Method item)
        {
            return void.class.equals(item.getReturnType())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    private final Filter<Method> mFactoryFilter = new Filter<Method>()
    {
        @Override
        public boolean filter(Method item)
        {
            return Modifier.isStatic(item.getModifiers())
                    && !void.class
                    .equals(item.getReturnType());
        }
    };

    private final Filter<Field> mFieldFilter = new Filter<Field>()
    {
        @Override
        public boolean filter(Field item)
        {
            return !Modifier.isFinal(item.getModifiers())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    //实例包内

    Dependency makeResult()
    {
        return new Dependency(mOwnerProxy.getResultType(), mProviders);
    }

    AnalyzerEnv(final Class<?> ownerType, Node node)
    {
        this.mOwnerProxy = new Provider()
        {
            @NonNull
            @Override
            public <T> T provide(DependencyManager dependencyManager)
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
        this.mCurrentNode = node;
    }

    void mark(Node currentNode)
    {
        this.mCurrentNode = currentNode;
    }

    TextType getTextType(String text)
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

    Provider newConstantProvider(String let)
    {
        String value = let.substring(1, let.length());
        if (let.charAt(0) == '@')
        {
            return new DependencyProvider(
                    DependencyType.SINGLETON,
                    newSingleton(value),
                    null
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
                            null
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

    Method findProperty(Class<?> clazz,
                        String name,
                        Class<?>[] sClasses)
    {
        return findMethod(clazz, "set"
                + Character.toUpperCase(name.charAt(0))
                + name.substring(1), sClasses, mPropertyFilter);
    }

    Method findFactory(Class<?> clazz,
                       String name,
                       Class<?>[] sClasses)
    {
        return findMethod(clazz, name, sClasses, mFactoryFilter);
    }

    Field findField(Class<?> clazz,
                    String name,
                    Class<?> sClass)
    {
        return findField(clazz, name, sClass, mFieldFilter);
    }

    Provider getProvider(String name)
    {
        if (DependencyManager.OWNER.equals(name))
        {
            return mOwnerProxy;
        }
        return mProviders.get(name);
    }

    void addProvider(String name, Provider provider)
    {
        if (DependencyManager.OWNER.equals(name)
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
    String getAttrNoThrow(Element element,
                          String attr)
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

    Constructor<?> findConstructor(Class<?> clazz,
                                   Class<?>[] classes)
    {
        return findConstructor(clazz, classes, null);
    }

    @NonNull
    String getAttrIfEmptyThrow(Element element,
                               String attr)
    {
        mark(element);
        String name = getAttrNoThrow(element, attr);
        if (!TextUtils.isEmpty(name))
        {
            return name;
        }
        throw fromMessageThrow(String.format("Attr %s no found", name));
    }

    RuntimeException fromMessageThrow(String massage)
    {
        return new GenerateDepartmentException(String.format(
                "Error in %s ", mCurrentNode.asXML())
                + (TextUtils.isEmpty(massage)
                ? ""
                : String.format(", message = %s", massage)));
    }

    //静态包内

    static int[] getResIds(Object owner)
    {
        Using using = owner.getClass().getAnnotation(Using.class);
        return using == null ? null : using.value();
    }

    static boolean listNoEmpty(List<?> list)
    {
        return list != null && list.size() != 0;
    }

    static Setter newSetter(final Method method, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, DependencyManager dependency)
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

    static Setter newSetter(final Field field, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, DependencyManager dependency)
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

    @SuppressWarnings("unchecked")
    static Factory newFactory(final Method method,
                              final List<String> references)
    {
        return new Factory()
        {
            @NonNull
            @Override
            public <T> T newInstance(DependencyManager dependency)
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

    @SuppressWarnings("unchecked")
    static Factory newFactory(final Constructor<?> constructor,
                              final List<String> references)
    {
        return new Factory()
        {
            @NonNull
            @Override
            public <T> T newInstance(DependencyManager dependency)
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

    @SuppressWarnings("unchecked")
    static <T> T castTo(Object obj, Class<T> targetClass)
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
        throw new ClassCastException("Cannot cast "
                + objClass.getName()
                + " to "
                + targetClass.getName());
    }

    static boolean isAssignTo(Class<?> objClass, Class<?> targetClass)
    {
        if (targetClass.isAssignableFrom(objClass))
        {
            return true;
        }
        return sTypeConverters.get(Pair.create(objClass, targetClass)) != null;
    }

    //实例私有

    private Field findField(Class<?> clazz,
                            String name,
                            Class<?> sClass,
                            Filter<Field> filter)
    {
        Field field = null;
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

    private Method findMethod(Class<?> clazz,
                              String name,
                              Class<?>[] sClasses,
                              Filter<Method> filter)
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

    private Constructor<?> findConstructor(Class<?> clazz,
                                           Class<?>[] sClasses,
                                           Filter<Constructor<?>> filter)
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

    private RuntimeException
    formExceptionThrow(Throwable e)
    {
        return new GenerateDepartmentException(
                String.format("Error in %s", mCurrentNode.asXML()), e);
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
                            | InvocationTargetException
                            | NoSuchMethodException e)
                    {
                        throw new AssertionError(e);
                    }
                }
            });
        }
        return result;
    }

    @NonNull
    private static Object[] getReferences(List<String> refs,
                                          Class<?>[] targetClasses,
                                          DependencyManager dependency)
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
            public <T> T newInstance(DependencyManager dependencyManager)
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