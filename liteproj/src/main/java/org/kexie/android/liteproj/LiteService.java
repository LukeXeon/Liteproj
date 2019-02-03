package org.kexie.android.liteproj;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

public class LiteService extends Service
{
    @CallSuper
    @Override
    public void onCreate()
    {
        super.onCreate();
        LifecycleManager.onAttach(this);
    }

    @CallSuper
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LifecycleManager.onDetach(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
