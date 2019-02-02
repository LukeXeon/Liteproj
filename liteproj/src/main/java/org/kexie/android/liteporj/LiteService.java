package org.kexie.android.liteporj;

import android.arch.lifecycle.LifecycleService;
import android.support.annotation.CallSuper;

public class LiteService extends LifecycleService
{
    @CallSuper
    @Override
    public void onCreate()
    {
        super.onCreate();
        getLifecycle().addObserver(LiteprojInitializer.sLifecycleHandler);
    }

    @CallSuper
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getLifecycle().removeObserver(LiteprojInitializer.sLifecycleHandler);
    }
}
