package org.kexie.android.arch.sample;

import com.orhanobut.logger.Logger;

public class Factory
{
    public static Object test(String text)
    {
        Logger.d(text);
        return new Object();
    }

}
