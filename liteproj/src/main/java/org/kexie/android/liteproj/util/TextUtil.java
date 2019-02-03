package org.kexie.android.liteproj.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import fr.xgouchet.axml.Attribute;
import fr.xgouchet.axml.CompressedXmlParser;
import fr.xgouchet.axml.CompressedXmlParserListener;

public final class TextUtil
{
    private interface TextConverter
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    private static final class AXmlParserHandler
            implements CompressedXmlParserListener
    {
        private final List<Branch> mStack = new ArrayList<>();
        private Document mDocument;

        @Override
        public void startDocument()
        {
            mDocument = DocumentFactory.getInstance().createDocument();
            mStack.add(mDocument);
        }

        @Override
        public void endDocument()
        {

        }

        @Override
        public void startPrefixMapping(String prefix,
                                       String uri)
        {

        }

        @Override
        public void endPrefixMapping(String prefix,
                                     String uri)
        {

        }

        @Override
        public void startElement(String uri,
                                 String localName,
                                 String qName,
                                 Attribute[] attributes)
        {
            Element element = TextUtils.isEmpty(uri)
                    ? mStack.get(mStack.size() - 1).addElement(localName)
                    : mStack.get(mStack.size() - 1).addElement(qName, uri);
            for (Attribute attribute : attributes)
            {
                if (TextUtils.isEmpty(attribute.getNamespace()))
                {
                    element.addAttribute(attribute.getName(),
                            attribute.getValue());
                } else
                {
                    element.addAttribute(new QName(attribute.getName(),
                                    new Namespace(attribute.getPrefix(),
                                            attribute.getNamespace())),
                            attribute.getValue());
                }
            }
            mStack.add(element);
        }

        @Override
        public void endElement(String uri,
                               String localName,
                               String qName)
        {
            mStack.remove(mStack.size() - 1);
        }

        @Override
        public void characterData(String data)
        {
            Branch branch = mStack.get(mStack.size() - 1);
            if (branch instanceof Element)
            {
                ((Element) branch).addCDATA(data);
            }
        }

        @Override
        public void processingInstruction(String target,
                                          String data)
        {

        }

        @Override
        public void text(String data)
        {
            Branch branch = mStack.get(mStack.size() - 1);
            if (branch instanceof Element)
            {
                ((Element) branch).addText(data);
            }
        }
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
    public static Object getConstantByText(@NonNull String let)
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
    public static Document getDocument(@NonNull InputStream stream,
                                       boolean isCompressed)
    {
        if (!isCompressed)
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
                AXmlParserHandler handler = new AXmlParserHandler();
                new CompressedXmlParser().parse(stream, handler);
                return handler.mDocument;
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @NonNull
    public static TextType getTextType(@NonNull String text)
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