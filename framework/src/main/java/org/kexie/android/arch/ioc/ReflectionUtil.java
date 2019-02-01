package org.kexie.android.arch.ioc;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ReflectionUtil
{

    private ReflectionUtil()
    {
        throw new AssertionError();
    }

    private interface CastOf<T>
    {
        Object cast(T obj);
    }

    private interface Filter<T>
    {
        boolean filter(T item);
    }

    private static final List<Class<?>> SUPPORT_TYPES
            = Collections.unmodifiableList(new LinkedList<Class<?>>()
    {
        {
            add(Application.class);
            add(AppCompatActivity.class);
            add(Fragment.class);
            add(LiteService.class);
        }
    });

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

    static final Filter<Field> SUPPORT_FIELD_FILTER = new Filter<Field>()
    {
        @Override
        public boolean filter(Field item)
        {
            return !Modifier.isFinal(item.getModifiers())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    static final Filter<Method> SUPPORT_PROPERTY_FILTER = new Filter<Method>()
    {
        @Override
        public boolean filter(Method item)
        {
            return void.class.equals(item.getReturnType())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    static final Filter<Method> SUPPORT_FACTORY_FILTER = new Filter<Method>()
    {
        @Override
        public boolean filter(Method item)
        {
            return Modifier.isStatic(item.getModifiers())
                    && !void.class
                    .equals(item.getReturnType());
        }
    };

    private static boolean isAssignTo(Class<?> objClass, Class<?> targetClass)
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
    private static Object castTo(Object obj, Class<?> targetClass)
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

    static Setter newSetter(final Method method, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, Dependency dependency)
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
            public void set(Object target, Dependency dependency)
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

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return constructor.getDeclaringClass();
            }

        };
    }

    @SuppressWarnings("unchecked")
    static Factory newConstantFactory(Object object)
    {
        final Object notNull = Objects.requireNonNull(object);
        return new Factory()
        {
            @NonNull
            @Override
            public <T> T newInstance(Dependency dependency)
            {
                return (T) notNull;
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return notNull.getClass();
            }

        };
    }

    static Method
    findSupportMethod(Class<?> clazz,
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

    static Constructor<?>
    findSupportConstructor(Class<?> clazz,
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

    static Field
    findSupportField(Class<?> clazz,
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

    static int[] getResIds(Object owner)
    {
        Using using = owner.getClass().getAnnotation(Using.class);
        return using == null ? null : using.value();
    }

    static void compatToSupportTypes(Class<?> type)
    {
        for (Class<?> clazz : SUPPORT_TYPES)
        {
            if (clazz.isAssignableFrom(type))
            {
                return;
            }
        }
        throw new IllegalStateException("no support type " + type);
    }

    private static boolean equalsToSupportTypes(Class<?> clazz)
    {
        for (Class<?> type : SUPPORT_TYPES)
        {
            if (type.equals(clazz))
            {
                return true;
            }
        }
        return false;
    }

    static void inject(Object object, Dependency dependency)
    {
        Class<?> type = object.getClass();
        while (type != null && !equalsToSupportTypes(type))
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
                            field.set(object, dependency.get(reference.value()));
                        } catch (IllegalAccessException e)
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
                            property.invoke(object, dependency.get(reference.value()));
                        } catch (IllegalAccessException | InvocationTargetException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            type = type.getSuperclass();
        }
    }

}
