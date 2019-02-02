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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

//1.完善异常信息,并提供更高的出错容忍
//2.添加对jetpack组件包的支持,使用LiteViewModel支持ViewModel
//3.优化内存使用Analyzer使用单实例,删除HolderFragment
//4.添加对Service的支持,使用LiteService支持Service
//5.优化冗余代码
//6.添加更多注释

public final class Liteproj
{
    private Liteproj()
    {
        throw new AssertionError();
    }

    private static final ThreadLocal<DependencyAnalyzer> sDependencyAnalyzer
            = new ThreadLocal<>();

    private final static LifecycleObserver sLifecycleHandler
            = new GenericLifecycleObserver()
    {
        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event)
        {
            switch (event)
            {
                case ON_CREATE:
                {
                    onCreate(source);
                }
                break;
                case ON_DESTROY:
                {
                    onDestroy(source);
                }
                break;
            }
        }
    };

    private final static FragmentManager.FragmentLifecycleCallbacks sFragmentCallbacks
            = new FragmentManager.FragmentLifecycleCallbacks()
    {
        @Override
        public void onFragmentAttached(FragmentManager fm,
                                       Fragment f,
                                       Context context)
        {
            onCreate(f);
        }

        @Override
        public void onFragmentDestroyed(FragmentManager fm,
                                        Fragment f)
        {
            onDestroy(f);
        }
    };

    private final static Application.ActivityLifecycleCallbacks sActivityCallbacks
            = new LifecycleCallbacksAdapter()
    {
        @Override
        public void onActivityCreated(Activity activity,
                                      Bundle savedInstanceState)
        {
            onCreate(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity)
        {
            onDestroy(activity);
        }
    };

    static void init(@NonNull Application application)
    {
        sDependencyAnalyzer.set(new DependencyAnalyzer(application));
        application.registerActivityLifecycleCallbacks(sActivityCallbacks);
    }

    @SuppressWarnings("WeakerAccess")
    public static LifecycleObserver getLifecycleHandler()
    {
        return sLifecycleHandler;
    }

    private static void onCreate(Object owner)
    {
        if (!DependencyManager.sTable.containsKey(owner))
        {
            DependencyManager.sTable.put(owner, null);
            if (owner instanceof FragmentActivity)
            {
                ((FragmentActivity) owner).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(sFragmentCallbacks,
                                true);
                onInject(owner, null);
            }
        }
    }

    private static void onDestroy(Object owner)
    {
        DependencyManager manager = DependencyManager.sTable.remove(owner);
        if (manager != null)
        {
            manager.onDestroy();
        }
    }

    private static void onInject(Object object, DependencyManager dependency)
    {
        Class<?> type = object.getClass();
        while (type != null)
        {
            for (Field field : type.getDeclaredFields())
            {
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null)
                {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isFinal(modifiers)
                            && !Modifier.isStatic(modifiers)
                            && Reflections.isAssignTo(
                            dependency.getResultType(reference.value()),
                            field.getType()))
                    {
                        field.setAccessible(true);
                        try
                        {
                            field.set(object, Reflections.castTo(
                                    dependency.get(reference.value()),
                                    field.getType()));
                        } catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            for (Method property : type.getDeclaredMethods())
            {
                Reference reference = property.getAnnotation(Reference.class);
                if (reference != null)
                {
                    int modifiers = property.getModifiers();
                    String name = property.getName();
                    Class<?>[] parameterTypes = property.getParameterTypes();
                    if (name.length() >= 3
                            && "set".equals(name.substring(0, 2))
                            && parameterTypes.length == 1
                            && !Modifier.isStatic(modifiers)
                            && !Modifier.isAbstract(modifiers)
                            && Reflections.isAssignTo(dependency
                                    .getResultType(reference.value()),
                            parameterTypes[0]))
                    {
                        property.setAccessible(true);
                        try
                        {
                            property.invoke(object,
                                    Reflections.castTo(dependency.get(reference.value()),
                                            parameterTypes[0]));
                        } catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            type = type.getSuperclass();
        }
    }

    static DependencyManager createDependencyManager(Object owner,
                                                     List<Dependency> dependencies)
    {
        return null;
    }
}
