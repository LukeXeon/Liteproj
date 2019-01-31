package org.kexie.android.arch.automatic.dependency;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import org.kexie.android.arch.automatic.app.EmptyActivityLifecycleCallbacks;

public final class DependenciesManager
{
    private DependenciesManager()
    {
        throw new AssertionError();
    }

    private static Dependency appGlobal;

    @Nullable
    @SuppressWarnings("WeakerAccess")
    public static Dependency of(Object object)
    {
        if (object instanceof Application)
        {
            return appGlobal;
        } else if (object instanceof AppCompatActivity
                || object instanceof Fragment)
        {
            return HolderFragment.getDependency(object);
        }
        return null;
    }

    public static void init(Application application)
    {
        final FragmentManager.FragmentLifecycleCallbacks
                fragmentLifecycleCallbacks
                = new FragmentManager.FragmentLifecycleCallbacks()
        {
            @Override
            public void onFragmentAttached(@NonNull FragmentManager fm,
                                           @NonNull Fragment fragment,
                                           @NonNull Context context)
            {
                if (!(fragment instanceof HolderFragment))
                {
                    fragment.getChildFragmentManager()
                            .registerFragmentLifecycleCallbacks(
                                    this,
                                    false
                            );
                    inject(fragment);
                }
            }
        };
        application.registerActivityLifecycleCallbacks(
                new EmptyActivityLifecycleCallbacks()
                {
                    @Override
                    public void onActivityCreated(Activity activity,
                                                  Bundle savedInstanceState)
                    {
                        if (activity instanceof AppCompatActivity)
                        {
                            AppCompatActivity appCompatActivity
                                    = (AppCompatActivity) activity;
                            appCompatActivity.getSupportFragmentManager()
                                    .registerFragmentLifecycleCallbacks(
                                            fragmentLifecycleCallbacks,
                                            false
                                    );
                            inject(appCompatActivity);
                        }
                    }
                });
        appGlobal = DependencyAnalyzer.analysis(application, application);
        inject(application);
    }

    @SuppressWarnings("WeakerAccess")
    public static void inject(Object object)
    {
        Dependency dependency = of(object);

    }
}
