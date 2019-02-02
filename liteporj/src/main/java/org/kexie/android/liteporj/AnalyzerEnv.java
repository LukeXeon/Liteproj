package org.kexie.android.liteporj;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    private final Class<?> mOwnerType;

    private final Map<String, Provider> mProviders = new ArrayMap<>();

    private final Map<Pair<Class<?>, Class<?>>, TypeConverter> mTypeConverters;

    private final List<TextConverter> mTextConverters;

    private final Pattern mNamePattern;

    @NonNull
    static AnalyzerEnv newEnv(@NonNull Object owner)
    {
        return new AnalyzerEnv(owner.getClass(),
                newTypeConverter(),
                newTextConverter(),
                newNamePattern());
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
            return new Provider(
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
                    return new Provider(
                            DependencyType.SINGLETON,
                            newSingleton(valueOf.valueOf(value)),
                            null
                    );
                } catch (NumberFormatException ignored)
                {

                }
            }
            throw new IllegalStateException("no type match to let = " + let);
        }
    }


    Setter newSetter(final Method method, final String name)
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

    Setter newSetter(final Field field, final String name)
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
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    Factory newFactory(final Method method,
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
                } catch (Exception e)
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
    Factory newFactory(final Constructor<?> constructor,
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
                } catch (Exception e)
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

    Method findMethod(Class<?> clazz,
                      String name,
                      Class<?>[] sClasses,
                      Filter<Method> filter)
            throws NoSuchMethodException
    {
        if (sClasses == null)
        {
            return clazz.getMethod(name);
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
        throw new NoSuchMethodException("name by " + name);
    }

    Constructor<?> findConstructor(Class<?> clazz,
                                   Class<?>[] sClasses,
                                   Filter<Constructor<?>> filter)
            throws NoSuchMethodException
    {
        if (sClasses == null)
        {
            return clazz.getConstructor();
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
        throw new NoSuchMethodException("no constructor match");
    }

    Field findField(Class<?> clazz,
                    String name,
                    Class<?> sClass,
                    Filter<Field> filter)
            throws NoSuchFieldException
    {
        Field field = clazz.getField(name);
        if (isAssignTo(sClass, field.getType())
                && (filter == null || filter.filter(field)))
        {
            return field;
        } else
        {
            throw new NoSuchFieldException("can't found field name by "
                    + name
                    + " ,can not cast " + sClass + " to " + field.getType());
        }
    }

    Dependency makeResult()
    {
        return new Dependency(mOwnerType, mProviders);
    }

    private AnalyzerEnv(Class<?> ownerType,
                        Map<Pair<Class<?>, Class<?>>, TypeConverter> casts,
                        List<TextConverter> valueOfList,
                        Pattern namePattern)
    {
        this.mOwnerType = ownerType;
        this.mTypeConverters = casts;
        this.mTextConverters = valueOfList;
        this.mNamePattern = namePattern;
    }

    private static boolean isFloatType(@NonNull Class<?> type)
    {
        return Float.class.equals(type) || Double.class.equals(type);
    }

    @NonNull
    private static Map<Pair<Class<?>, Class<?>>, TypeConverter> newTypeConverter()
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

    private static List<TextConverter> newTextConverter()
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

    private static Pattern newNamePattern()
    {
        return Pattern.compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");
    }

    @NonNull
    private Object[] getReferences(List<String> refs,
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
    private <T> T castTo(Object obj, Class<T> targetClass)
    {
        //处理引用类型和可赋值类型
        Class<?> objClass = obj.getClass();
        if (targetClass.isAssignableFrom(objClass))
        {
            return (T) obj;
        }
        TypeConverter castTo = mTypeConverters.get(Pair.create(objClass, targetClass));
        if (castTo != null)
        {
            return (T) castTo.castTo(obj);
        }
        throw new ClassCastException("Cannot cast "
                + objClass.getName()
                + " to "
                + targetClass.getName());
    }

    private boolean isAssignTo(Class<?> objClass, Class<?> targetClass)
    {
        if (targetClass.isAssignableFrom(objClass))
        {
            return true;
        }
        return mTypeConverters.get(Pair.create(objClass, targetClass)) != null;
    }

    @SuppressWarnings("unchecked")
    private Factory newSingleton(Object object)
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
