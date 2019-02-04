package org.kexie.android.liteproj.sample;

import com.orhanobut.logger.Logger;

public class Factory
{
    public static Bean test(String text)
    {
        Logger.d(text);
        return new Bean();
    }

}
