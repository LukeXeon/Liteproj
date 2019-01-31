package org.kexie.android.arch.sample;

import com.orhanobut.logger.Logger;

public class Factory
{
    public static Bean test(String text)
    {
        return new Bean();
    }

}
