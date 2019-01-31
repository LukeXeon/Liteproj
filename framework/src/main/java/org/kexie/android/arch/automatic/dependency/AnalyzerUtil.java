package org.kexie.android.arch.automatic.dependency;

import android.app.Application;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public final class AnalyzerUtil
{
    private AnalyzerUtil()
    {
        throw new AssertionError();
    }

    private static final List<Class<?>> SUPPORT_TYPES
            = Collections.unmodifiableList(new LinkedList<Class<?>>()
    {
        {
            add(Application.class);
            add(AppCompatActivity.class);
            add(Fragment.class);
        }
    });

    private final static Pattern NAME_PATTERN;

    static
    {
        NAME_PATTERN = Pattern
                .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");
    }

    public static String getAttr(Element element,
                                 String attr,
                                 String error)
    {
        if (element.attributeCount() != 0)
        {
            Attribute attribute = element.attribute(attr);
            if (attribute != null)
            {
                return attribute.getValue();
            }
        }
        if (error != null)
        {
            throw runtimeException(element, "[" + attr + "]" + " no found," + error);
        }
        return null;
    }


    public static RuntimeException runtimeException(Element element, String message)
    {
        return new RuntimeException("in [" + element.toString() + "] " + message);
    }

    public static RuntimeException runtimeException(Element element, Throwable e)
    {
        return new RuntimeException("in [" + element.toString() + "]", e);
    }

    public static NameType getNameType(String text)
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

    public static boolean checkSupportType(Class<?> type)
    {
        for (Class<?> clazz : SUPPORT_TYPES)
        {
            if (clazz.isAssignableFrom(type))
            {
                return true;
            }
        }
        throw new IllegalStateException("no support type " + type);
    }
}
