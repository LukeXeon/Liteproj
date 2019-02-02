package org.kexie.android.liteporj;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.util.LinkedList;
import java.util.List;

public final class LiteprojInitializer
        extends VoidContentProvider
{
    private static DependencyAnalyzer
            sDependencyAnalyzer;

    final static LifecycleObserver
            sLifecycleHandler = new GenericLifecycleObserver()
    {
        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event)
        {
            switch (event)
            {
                case ON_CREATE:
                {
                    onDependencyAttach(source);
                }
                break;
                case ON_DESTROY:
                {
                    onDependencyDestroy(source);
                }
                break;
            }
        }
    };

    private final static FragmentManager.FragmentLifecycleCallbacks
            sFragmentCallbacks = new FragmentManager.FragmentLifecycleCallbacks()
    {
        @Override
        public void onFragmentAttached(FragmentManager fm,
                                       Fragment f,
                                       Context context)
        {
            onDependencyAttach(f);
        }

        @Override
        public void onFragmentDestroyed(FragmentManager fm,
                                        Fragment f)
        {
            onDependencyDestroy(f);
        }
    };

    private final static Application.ActivityLifecycleCallbacks
            sActivityCallbacks = new EmptyActivityLifecycleCallbacks()
    {
        @Override
        public void onActivityCreated(Activity activity,
                                      Bundle savedInstanceState)
        {
            onDependencyAttach(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity)
        {
            onDependencyDestroy(activity);
        }
    };

    static synchronized void init(@NonNull Application application)
    {
        if (sDependencyAnalyzer == null)
        {
            sDependencyAnalyzer = new DependencyAnalyzer(application);
            application.registerActivityLifecycleCallbacks(sActivityCallbacks);
        }
    }

    private static void onDependencyAttach(@NonNull Object owner)
    {
        if (!DependencyManager.sTable.containsKey(owner))
        {
            if (owner instanceof FragmentActivity)
            {
                ((FragmentActivity) owner).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(sFragmentCallbacks,
                                true);
            }

            DependencyManager manager = null;
            int[] resIds = AnalyzerEnv.getResIds(owner);
            if (resIds != null && resIds.length != 0)
            {
                List<Dependency> dependencies = new LinkedList<>();
                for (int resId : resIds)
                {
                    dependencies.add(sDependencyAnalyzer.analysis(resId));
                }
                manager = new DependencyManager(owner, dependencies);
            }
            DependencyManager.sTable.put(owner, manager);
            if (manager != null)
            {
                AnalyzerEnv.inject(owner, manager);
            }
        }
    }

    private static void onDependencyDestroy(@NonNull Object owner)
    {
        DependencyManager manager = DependencyManager.sTable.remove(owner);
        if (manager != null)
        {
            manager.onDestroy();
        }
    }

    @Override
    public boolean onCreate()
    {
        Context context = getContext();
        assert context != null;
        init((Application) context.getApplicationContext());
        return true;
    }
}
