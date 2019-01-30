package org.kexie.android.arch.automatic.dependency;

import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class InternalUtil
{
    private InternalUtil()
    {
        throw new AssertionError();
    }

    private interface CastOf<T>
    {
        Object cast(T obj);
    }

    private final static Map<Class<?>, Map<Class<?>, CastOf>> CAST_OF;

    private final static Pattern NAME_PATTERN;

    static
    {
        CAST_OF = new ArrayMap<Class<?>, Map<Class<?>, CastOf>>()
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

        NAME_PATTERN = Pattern.compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");
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

    static NameType getNameType(String text)
    {
        if (TextUtils.isEmpty(text))
        {
            return NameType.Illegal;
        } else
        {
            if ((text.charAt(0) == '@')
                    && !TextUtils.isEmpty(text.substring(1, text.length())))
            {
                return NameType.Constant;
            } else if (NAME_PATTERN
                    .matcher(text)
                    .matches())
            {
                return NameType.Reference;
            } else
            {
                return NameType.Illegal;
            }
        }
    }

    public static boolean isEmptyList(List<?> list)
    {
        return list == null || list.size() == 0;
    }
}
