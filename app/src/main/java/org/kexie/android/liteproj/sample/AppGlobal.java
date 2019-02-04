package org.kexie.android.liteproj.sample;

import android.app.Application;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;

@Using(R.xml.all_test)
public class AppGlobal extends Application
{
    @Reference("holder")
    AppHolderTest appHolderTest;

    private static final String TAG = "AppGlobal";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Logger.d(appHolderTest.context);
    }
}
