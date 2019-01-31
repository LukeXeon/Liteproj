package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReflectionUtil
{
    private ReflectionUtil()
    {
        throw new AssertionError();
    }

    private final static Map<Class<?>, Map<Class<?>, CastOf>> CAST_OF
            = Collections.unmodifiableMap(
            new ArrayMap<Class<?>, Map<Class<?>, CastOf>>()
            {
                {
                    CastOf toThis = new CastOf<Object>()
                    {
                        @Override
                        public Object cast(Object obj)
                        {
                            return obj;
                        }
                    };
                    CastOf toShort = new CastOf<Number>()
                    {
                        @Override
                        public Object cast(Number number)
                        {
                            return number.shortValue();
                        }
                    };
                    CastOf toInt = new CastOf<Number>()
                    {
                        @Override
                        public Object cast(Number number)
                        {
                            return number.intValue();
                        }
                    };
                    CastOf toLong = new CastOf<Number>()
                    {
                        @Override
                        public Object cast(Number number)
                        {
                            return number.longValue();
                        }
                    };
                    CastOf toDouble = new CastOf<Number>()
                    {
                        @Override
                        public Object cast(Number number)
                        {
                            return number.doubleValue();
                        }
                    };
                    Map<Class<?>, CastOf> castOf = new ArrayMap<>();
                    castOf.put(Character.class, toThis);
                    put(char.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Boolean.class, toThis);
                    put(boolean.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Byte.class, toThis);
                    put(byte.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Byte.class, toShort);
                    castOf.put(Short.class, toThis);
                    put(short.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Byte.class, toInt);
                    castOf.put(Short.class, toInt);
                    castOf.put(Integer.class, toThis);
                    put(int.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Byte.class, toLong);
                    castOf.put(Short.class, toLong);
                    castOf.put(Integer.class, toLong);
                    castOf.put(Long.class, toThis);
                    put(long.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Float.class, toThis);
                    put(float.class, castOf);
                    castOf = new ArrayMap<>();
                    castOf.put(Float.class, toDouble);
                    castOf.put(Double.class, toThis);
                    put(double.class, castOf);
                }
            });

    static boolean isAssignTo(Class<?> objClass, Class<?> targetClass)
    {
        if (targetClass.isAssignableFrom(objClass))
        {
            return true;
        }
        Map<Class<?>, CastOf> classSet = CAST_OF.get(targetClass);
        if (classSet != null)
        {
            return classSet.containsKey(objClass);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Object castTo(Object obj, Class<?> targetClass)
    {
        //处理引用类型和可赋值类型
        Class<?> objClass = obj.getClass();
        if (targetClass.isAssignableFrom(objClass))
        {
            return obj;
        }
        Map<Class<?>, CastOf> classSet = CAST_OF.get(targetClass);
        if (classSet != null)
        {
            CastOf castOf = classSet.get(objClass);
            if (castOf != null)
            {
                return castOf.cast(obj);
            }
        }
        throw new ClassCastException("Cannot cast "
                + objClass.getName()
                + " to "
                + targetClass.getName());
    }

    @NonNull
    private static Object[] getReferences(List<String> refs,
                                          Class<?>[] targetClasses,
                                          Dependency dependency)
    {
        Object[] args = new Object[refs.size()];
        for (int i = 0; i < refs.size(); i++)
        {
            String name = refs.get(i);
            args[i] = castTo(dependency.get(name), targetClasses[i]);
        }
        return args;
    }

    public static Setter newSetter(final Method method, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, Dependency dependency)
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(target, dependency.get(name));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Setter newSetter(final Field field, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, Dependency dependency)
            {
                field.setAccessible(true);
                try
                {
                    field.set(target, dependency.get(name));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Factory newFactory(final Method method,
                                     final List<String> references)
    {
        return new Factory()
        {
            @Override
            public <T> T newInstance(Dependency dependency)
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

            @Override
            public Class<?> getResultType()
            {
                return method.getReturnType();
            }

            @Override
            public Class<?>[] getParameterTypes()
            {
                return method.getParameterTypes();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Factory newFactory(final Constructor<?> constructor,
                                     final List<String> references)
    {
        return new Factory()
        {
            @Override
            public <T> T newInstance(Dependency dependency)
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

            @Override
            public Class<?> getResultType()
            {
                return constructor.getDeclaringClass();
            }

            @Override
            public Class<?>[] getParameterTypes()
            {
                return constructor.getParameterTypes();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Factory newConstantFactory(Object object)
    {
        final Object notNull = Objects.requireNonNull(object);
        return new Factory()
        {
            @Override
            public <T> T newInstance(Dependency dependency)
            {
                return (T) notNull;
            }

            @Override
            public Class<?> getResultType()
            {
                return notNull.getClass();
            }

            @Override
            public Class<?>[] getParameterTypes()
            {
                return new Class<?>[0];
            }
        };
    }

    public static Method
    findSupportMethod(Class<?> clazz,
                      String name,
                      Class<?>[] classes,
                      Filter<Method> filter)
            throws NoSuchMethodException
    {
        if (classes == null)
        {
            return clazz.getMethod(name);
        }
        for (Method method : clazz.getMethods())
        {
            boolean match = true;
            Class<?>[] pram = method.getParameterTypes();
            if (method.getName().equals(name)
                    && pram.length == classes.length
                    && (filter == null || filter.filter(method)))
            {
                for (int i = 0; i < pram.length; i++)
                {
                    if (!isAssignTo(classes[i], pram[i]))
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

    public static Constructor<?>
    findSupportConstructor(Class<?> tClass,
                           Class<?>[] classes,
                           Filter<Constructor<?>> filter)
            throws NoSuchMethodException
    {
        if (classes == null)
        {
            return tClass.getConstructor();
        }
        for (Constructor<?> constructor : tClass.getConstructors())
        {
            boolean match = true;
            Class<?>[] pram = constructor.getParameterTypes();
            if (pram.length == classes.length
                    && (filter == null || filter.filter(constructor)))
            {
                for (int i = 0; i < pram.length; i++)
                {
                    if (!isAssignTo(classes[i], pram[i]))
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
}
