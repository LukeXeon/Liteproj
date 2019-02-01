package org.kexie.android.arch.ioc;

import android.arch.lifecycle.LifecycleService;

public class LiteService extends LifecycleService
{
    Dependency dependency;

    @Override
    public void onCreate()
    {
        super.onCreate();
        dependency = DependencyAnalyzer.analysis(this, this);
        if (dependency != null)
        {
            ReflectionUtil.inject(this, dependency);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        dependency = null;
    }
}
