package org.kexie.android.liteproj;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;

abstract class LifecycleHandler
        implements Application.ActivityLifecycleCallbacks
{
    @Override
    public void onActivityCreated(@NonNull Activity activity,
                                  @NonNull Bundle savedInstanceState)
    {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity)
    {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity)
    {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity)
    {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity)
    {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity,
                                            @NonNull Bundle outState)
    {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity)
    {

    }
}
