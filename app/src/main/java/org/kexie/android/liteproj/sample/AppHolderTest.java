package org.kexie.android.liteproj.sample;

import android.content.Context;

public class AppHolderTest
{
    final Context context;

    public AppHolderTest(Context context)
    {
        this.context = context;
    }

    @Override
    public String toString()
    {
        return super.toString() + context;
    }
}
