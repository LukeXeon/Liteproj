package org.kexie.android.arch.automatic.dependency;

import android.text.TextUtils;

import java.util.regex.Pattern;

public final class Texts
{
    private Texts()
    {
        throw new AssertionError();
    }

    private static final Pattern NAME_PATTERN
            = Pattern.compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");

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

    public enum NameType
    {
        Illegal,
        Constant,
        Reference
    }
}
