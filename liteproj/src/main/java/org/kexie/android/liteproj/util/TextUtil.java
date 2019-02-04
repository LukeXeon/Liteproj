package org.kexie.android.liteproj.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.regex.Pattern;

public final class TextUtil
{

    private static final Pattern sNamePattern = Pattern
            .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");

    private TextUtil()
    {
        throw new AssertionError();
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