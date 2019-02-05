package org.kexie.android.liteproj.sample;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.LiteService;
import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;


public class ServiceTest extends LiteService
{
    @Reference("holder")
    AppHolderTest test;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Logger.d("Service " + test);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
