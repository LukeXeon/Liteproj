package org.kexie.android.liteproj;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

final class LifecycleManager
{
    private LifecycleManager()
    {
        throw new AssertionError();
    }

    private static final String TAG = "LifecycleManager";

    private final static FragmentManager.FragmentLifecycleCallbacks
            sSupportFragmentCallbacks
            = new FragmentManager.FragmentLifecycleCallbacks()
    {
        @Override
        public void onFragmentAttached(@NonNull FragmentManager fm,
                                       @NonNull Fragment f,
                                       @NonNull Context context)
        {
            attachTo(f);
        }

        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm,
                                        @NonNull Fragment f)
        {
            detachFrom(f);
        }
    };

    private final static Application.ActivityLifecycleCallbacks
            sActivityCallbacks = new ActivityLifecycleCallbacks()
    {
        @Override
        public void onActivityCreated(@NonNull Activity activity,
                                      @NonNull Bundle savedInstanceState)
        {
            attachTo(activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity)
        {
            detachFrom(activity);
        }
    };

    private static final AtomicBoolean sInitialized
            = new AtomicBoolean(false);

    static void init(@NonNull Context context)
    {
        if (sInitialized.getAndSet(true))
        {
            return;
        }
        Application application = (Application) context
                .getApplicationContext();
        application.registerActivityLifecycleCallbacks(
                sActivityCallbacks);
        attachTo(application);
    }

    static void attachTo(@NonNull Object owner)
    {
        if (!DependencyManager.sHolderTable.containsKey(owner))
        {
            if (owner instanceof FragmentActivity)
            {
                ((FragmentActivity) owner).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(
                                sSupportFragmentCallbacks,
                                true);
            } else if (owner instanceof Activity)
            {
                Log.w(TAG, String.format(
                        "Using this %s type will cause fragments below to fail" +
                                " to receive injection events," +
                                " so you'd better use FragmentActivity",
                        owner.getClass()));
            }
            DependencyManager.sHolderTable.put(owner,
                    DependencyInjector.injectTo(owner));
        }
    }

    static void detachFrom(@NonNull Object owner)
    {
        DependencyManager manager = DependencyManager
                .sHolderTable.remove(owner);
        if (manager != null)
        {
            manager.onDestroy();
        }
    }
}