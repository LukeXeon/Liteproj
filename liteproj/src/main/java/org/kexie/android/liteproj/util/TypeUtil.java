package org.kexie.android.liteproj.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

public final class TypeUtil
{

    private interface TypeConverter
    {
        @NonNull
        Object castTo(@NonNull Object obj);
    }

    private static final Map<Pair<Class<?>, Class<?>>, TypeConverter>
            sTypeConverters = newTypeConverters();

    private static final Filter<Method> sPropertyFilter = new Filter<Method>()
    {
        @Override
        public boolean filter(@NonNull Method item)
        {
            return void.class.equals(item.getReturnType())
                    && sInstanceMethodFilter.filter(item);
        }
    };

    private static final Filter<Method> sFactoryFilter = new Filter<Method>()
    {
        @Override
        public boolean filter(@NonNull Method item)
        {
            return Modifier.isStatic(item.getModifiers())
                    && !void.class
                    .equals(item.getReturnType());
        }
    };

    private static final Filter<Field> sFieldFilter = new Filter<Field>()
    {
        @Override
        public boolean filter(@NonNull Field item)
        {
            return !Modifier.isFinal(item.getModifiers())
                    && !Modifier.isStatic(item.getModifiers());
        }
    };

    private static final Filter<Method> sInstanceMethodFilter
            = new Filter<Method>()
    {
        @Override
        public boolean filter(@NonNull Method item)
        {
            int modifiers = item.getModifiers();
            return !Modifier.isStatic(modifiers)
                    && !Modifier.isAbstract(modifiers);
        }
    };

    private TypeUtil()
    {
        throw new AssertionError();
    }

    @NonNull
    public static Method getTypeFactory(@NonNull Class<?> clazz,
                                        @NonNull String name,
                                        @Nullable Class<?>[] sClasses)
    {
        return getTypeMethod(clazz, name, sClasses, sFactoryFilter);
    }

    @NonNull
    public static Method getTypeProperty(@NonNull Class<?> clazz,
                                         @NonNull String name,
                                         @Nullable Class<?> sClass)
    {
        return getTypeMethod(clazz, "set"
                        + Character.toUpperCase(name.charAt(0))
                        + name.substring(1), new Class<?>[]{sClass},
                sPropertyFilter);
    }

    @NonNull
    public static Field getTypeField(@NonNull Class<?> clazz,
                                     @NonNull String name,
                                     @NonNull Class<?> sClass)
    {
        return getTypeField(clazz, name, sClass, sFieldFilter);
    }

    @NonNull
    public static Constructor<?> getTypeConstructor(@NonNull Class<?> clazz,
                                                    @Nullable Class<?>[] classes)
    {
        return getTypeConstructor(clazz, classes, null);
    }


    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static Field getTypeField(@NonNull Class<?> clazz,
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
            throw new RuntimeException(e);
        }
        if (isAssignToType(sClass, field.getType())
                && (filter == null || filter.filter(field)))
        {
            return field;
        } else
        {
            throw new RuntimeException(String.format(
                    "Can't found field name by %s , can't cast %s to %s",
                    name,
                    sClass,
                    field.getType()));
        }
    }

    @NonNull
    public static Method getTypeInstanceMethod(@NonNull Class<?> clazz,
                                               @NonNull String name,
                                               @Nullable Class<?>[] classes)
    {
        return getTypeMethod(clazz, name, classes, sInstanceMethodFilter);
    }

    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static Method getTypeMethod(@NonNull Class<?> clazz,
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
                throw new RuntimeException(e);
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
                    if (!isAssignToType(sClasses[i], pram[i]))
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
        throw new RuntimeException(String.format(
                "method whose name %s is not found in %s and requires %s",
                name,
                clazz,
                Arrays.toString(sClasses)));
    }

    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static Constructor<?> getTypeConstructor(@NonNull Class<?> clazz,
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
                throw new RuntimeException(e);
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
                    if (!isAssignToType(sClasses[i], pram[i]))
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
        throw new RuntimeException(
                String.format("Can't find constructor for %s in %s",
                        Arrays.toString(sClasses),
                        clazz));
    }

    @Nullable
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static <T> T castToType(@Nullable Object obj,
                                   @NonNull Class<T> targetClass)
    {
        if (obj == null)
        {
            return null;
        }
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

    public static boolean isAssignToType(@NonNull Class<?> objClass,
                                         @NonNull Class<?> targetClass)
    {
        if (targetClass.isAssignableFrom(objClass))
        {
            return true;
        }
        return sTypeConverters.get(Pair.create(objClass, targetClass)) != null;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isFloatType(@NonNull Class<?> type)
    {
        return Float.class.equals(type) || Double.class.equals(type);
    }

    @NonNull
    private static Map<Pair<Class<?>, Class<?>>, TypeConverter>
    newTypeConverters()
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
        Map<Pair<Class<?>, Class<?>>, TypeConverter> result = new ArrayMap<>();
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
}