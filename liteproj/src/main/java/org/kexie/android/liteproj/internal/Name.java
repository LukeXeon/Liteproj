package org.kexie.android.liteproj.internal;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Name
{
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public enum Type
    {
        ILLEGAL,
        CONSTANT,
        REFERENCE,
    }

    private interface ValueOf
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    private static final List<ValueOf> sConverters
            = getValueOfSequence();

    public final Type type;

    public final String value;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Name(String value)
    {
        this.value = value == null ? "" : value;
        this.type = getTextType(this.value);
    }

    @Override
    public String toString()
    {
        return String.format("name = %s , type = %s", value, type);
    }

    @NonNull
    static Object valueOfName(@NonNull String val)
    {
        String value = val.substring(1, val.length());
        if (val.charAt(0) == '@')
        {
            return value;
        } else
        {
            for (ValueOf valueOf : sConverters)
            {
                try
                {
                    return valueOf.valueOf(val);
                } catch (NumberFormatException ignored)
                {
                }
            }
            throw new NumberFormatException(
                    String.format(
                            "The name %s does not match the rule of val"
                            , val));
        }
    }

    @NonNull
    private static Type getTextType(@NonNull String text)
    {
        if (TextUtils.isEmpty(text))
        {
            return Type.ILLEGAL;
        } else
        {
            if ((text.charAt(0) == '@'
                    && text.length() > 1) || isNumber(text))
            {
                return Type.CONSTANT;
            } else if (Pattern
                    .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*")
                    .matcher(text)
                    .matches())
            {
                return Type.REFERENCE;
            } else
            {
                return Type.ILLEGAL;
            }
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Name)
        {
            return ((Name) obj).type.equals(type)
                    && ((Name) obj).value.equals(value);
        }
        return false;
    }

    private static boolean isNumber(String str)
    {
        boolean isInt = Pattern.compile("^-?[1-9]\\d*$").matcher(str).find();
        boolean isDouble = Pattern
                .compile("^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$")
                .matcher(str)
                .find();
        return isInt || isDouble;
    }

    @NonNull
    private static List<ValueOf> getValueOfSequence()
    {
        List<ValueOf> result = new LinkedList<>();
        result.add(new ValueOf()
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
            result.add(new ValueOf()
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
}
