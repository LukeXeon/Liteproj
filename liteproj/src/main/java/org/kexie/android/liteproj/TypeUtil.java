package org.kexie.android.liteproj;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;

import java.util.Map;

final class TypeUtil
{
    private TypeUtil()
    {
        throw new AssertionError();
    }

    @Nullable
    static int[] getResIds(@NonNull Class<?> ownerType)
    {
        Using using = ownerType.getClass().getAnnotation(Using.class);
        return using == null ? null : using.value();
    }

    private interface TypeConverter
    {
        @NonNull
        Object castTo(@NonNull Object obj);
    }

    private static final Map<Pair<Class<?>, Class<?>>, TypeConverter>
            sTypeConverters = newTypeConverters();

    @NonNull
    @SuppressWarnings("unchecked")
    static <T> T castTo(@NonNull Object obj,
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

    static boolean isAssignTo(@NonNull Class<?> objClass,
                              @NonNull Class<?> targetClass)
    {
        if (targetClass.isAssignableFrom(objClass))
        {
            return true;
        }
        return sTypeConverters.get(Pair.create(objClass, targetClass)) != null;
    }

    private static boolean isFloatType(@NonNull Class<?> type)
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
}
