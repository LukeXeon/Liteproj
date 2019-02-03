package org.kexie.android.liteproj;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import fr.xgouchet.axml.CompressedXmlParser;

final class TextUtil
{
    private interface TextConverter
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    enum TextType
    {
        ILLEGAL,
        CONSTANT,
        REFERENCE,
    }

    private static final SAXReader sSAXReader = new SAXReader();

    private static final Pattern sNamePattern = Pattern
            .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");

    private TextUtil()
    {
        throw new AssertionError();
    }

    private static final List<TextConverter> sTextConverters = newTextConverters();

    @NonNull
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

    @NonNull
    static Object getConstantByText(@NonNull String let)
    {
        String value = let.substring(1, let.length());
        if (let.charAt(0) == '@')
        {
            return value;
        } else
        {
            for (TextConverter valueOf : sTextConverters)
            {
                try
                {
                    return valueOf.valueOf(value);
                } catch (NumberFormatException ignored)
                {

                }
            }
            throw new NumberFormatException(
                    String.format(
                            "The name %s does not match the rule of let"
                            , let));
        }
    }

    @NonNull
    static Document getDocument(@NonNull InputStream stream,
                                boolean compressed)
    {
        if (!compressed)
        {
            try
            {
                return sSAXReader.read(stream);
            } catch (DocumentException e)
            {
                throw new RuntimeException(e);
            }
        } else
        {
            try
            {
                AXmlParserHandler listener = new AXmlParserHandler();
                new CompressedXmlParser().parse(stream, listener);
                return listener.getDocument();
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @NonNull
    static TextType getTextType(@NonNull String text)
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
            } else if (sNamePattern
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
}
