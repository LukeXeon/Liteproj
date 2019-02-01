package org.kexie.android.arch.ioc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public final class DependenciesManager
{
    private DependenciesManager()
    {
        throw new AssertionError();
    }

    private static Dependency appGlobal;

    @Nullable
    public static Dependency of(Object object)
    {
        if (object instanceof Application)
        {
            return appGlobal;
        } else if (object instanceof AppCompatActivity
                || object instanceof Fragment)
        {
            return HolderFragment.getDependency(object);
        } else if (object instanceof LiteService)
        {
            return ((LiteService) object).dependency;
        }
        return null;
    }

    static void init(Application application)
    {
        initLifecycleCallbacks(application);
        injectToApplication(application);
    }

    private synchronized static void initLifecycleCallbacks(Application application)
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
                    FragmentManager fragmentManager
                            = fragment.getChildFragmentManager();
                    fragmentManager.registerFragmentLifecycleCallbacks(
                            this,
                            false
                    );
                    if (ReflectionUtil.getResIds(fragment) != null)
                    {
                        HolderFragment.prepareInject(fragmentManager);
                    }
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
                            FragmentManager fragmentManager
                                    = appCompatActivity
                                    .getSupportFragmentManager();
                            fragmentManager.registerFragmentLifecycleCallbacks(
                                    fragmentLifecycleCallbacks,
                                    false
                            );
                            if (ReflectionUtil.getResIds(appCompatActivity) != null)
                            {
                                HolderFragment.prepareInject(fragmentManager);
                            }
                        }
                    }
                });
    }

    private synchronized static void injectToApplication(Application application)
    {
        if (appGlobal == null)
        {
            appGlobal = DependencyAnalyzer.analysis(application, application);
            if (appGlobal != null)
            {
                ReflectionUtil.inject(application, appGlobal);
            }
        }
    }

}
