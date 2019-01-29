package org.kexie.android.arch.automatic.dependency;

import android.support.v4.util.ArrayMap;
import java.util.Map;

public final class Types
{
    private Types()
    {
        throw new AssertionError();
    }

    private final static Map<Class<?>, Map<Class<?>, CastOf>> CAST_OF
            = new ArrayMap<Class<?>, Map<Class<?>, CastOf>>()
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
    };

    private interface CastOf<T>
    {
        Object cast(T obj);
    }

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
    static Object castTo(Object obj, Class<?> targetClass)
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
}
