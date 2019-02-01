package org.kexie.android.liteporj;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class Reflections
{
    private Reflections()
    {
        throw new AssertionError();
    }

    private interface CastTo
    {
        Object castTo(Object obj);
    }

    private final static Map<Pair<Class<?>, Class<?>>, CastTo> sCastTable = initCastTable();

    private static boolean isFloatType(Class<?> type)
    {
        return Float.class.equals(type) || Double.class.equals(type);
    }

    private static Map<Pair<Class<?>, Class<?>>, CastTo> initCastTable()
    {
        CastTo castToThis = new CastTo()
        {
            @Override
            public Object castTo(Object obj)
            {
                return obj;
            }
        };
        ArrayMap<Pair<Class<?>, Class<?>>, CastTo> result = new ArrayMap<>();
        result.put(Pair.<Class<?>, Class<?>>
                create(Boolean.class, Boolean.TYPE), castToThis);
        result.put(Pair.<Class<?>, Class<?>>
                create(Character.class, Character.TYPE), castToThis);
        List<Class<?>> numberTypes = new LinkedList<>();
        numberTypes.add(Byte.class);
        numberTypes.add(Short.class);
        numberTypes.add(Integer.class);
        numberTypes.add(Long.class);
        numberTypes.add(Float.class);
        numberTypes.add(Double.class);
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
                                new CastTo()
                                {
                                    @Override
                                    public Object castTo(Object obj)
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

    @SuppressWarnings("unchecked")
    static <T> T castTo(Object obj, Class<T> targetClass)
    {
        //处理引用类型和可赋值类型
        Class<?> objClass = obj.getClass();
        if (targetClass.isAssignableFrom(objClass))
        {
            return (T) obj;
        }
        CastTo castTo = sCastTable.get(Pair.create(objClass, targetClass));
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
        return sCastTable.get(Pair.create(objClass, targetClass)) != null;
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
                } catch (Exception e)
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

    @SuppressWarnings("unchecked")
    static Singleton newSingleton(Object object)
    {
        return new Singleton(Objects.requireNonNull(object, ""));
    }

    static Method findMethod(Class<?> clazz,
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

    static Constructor<?> findConstructor(Class<?> clazz,
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

    static Field findField(Class<?> clazz,
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
}
