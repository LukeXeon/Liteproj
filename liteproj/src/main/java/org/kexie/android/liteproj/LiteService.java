package org.kexie.android.liteproj;

import android.app.Service;
import android.support.annotation.CallSuper;

public abstract class LiteService extends Service
{
    @CallSuper
    @Override
    public void onCreate()
    {
        super.onCreate();
        LifecycleManager.attachTo(this);
    }

    @CallSuper
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        LifecycleManager.detachFrom(this);
    }

}
