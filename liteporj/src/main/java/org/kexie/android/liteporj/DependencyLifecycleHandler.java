package org.kexie.android.liteporj;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.GenericLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import static android.arch.lifecycle.Lifecycle.Event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class DependencyLifecycleHandler
        extends MergeLifecycleCallbacks
        implements GenericLifecycleObserver
{
    private final DependencyAnalyzer sAnalyzer;

    //兼容Application
    DependencyLifecycleHandler(Application application)
    {
        this.sAnalyzer = new DependencyAnalyzer(application);
        handleEvent(application, Event.ON_CREATE);
    }

    //兼容LiteViewModel和LiteService
    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event)
    {
        handleEvent(source, event);
    }

    //兼容v4 Fragment
    @Override
    public void onFragmentAttached(FragmentManager fm, Fragment f, Context context)
    {
        handleEvent(f, Event.ON_CREATE);
    }

    //兼容v4 Fragment
    @Override
    public void onFragmentDestroyed(FragmentManager fm, Fragment f)
    {
        handleEvent(f, Event.ON_DESTROY);
    }

    //只适配v4 FragmentActivity,其他Activity直接放行,Activity可能会创建多次
    // 但是依赖注入只会发生一次
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState)
    {
        handleEvent(activity, Event.ON_CREATE);
    }

    //兼容v4 FragmentActivity
    @Override
    public void onActivityDestroyed(Activity activity)
    {
        handleEvent(activity, Event.ON_DESTROY);
    }

    //处理事件
    private void handleEvent(Object owner, Event event)
    {
        switch (event)
        {
            case ON_CREATE:
            {
                if (!DependencyManager.sTable.containsKey(owner))
                {
                    DependencyManager.sTable.put(owner, null);
                    if (owner instanceof FragmentActivity)
                    {
                        ((FragmentActivity) owner).getSupportFragmentManager()
                                .registerFragmentLifecycleCallbacks(this,
                                        true);
                        inject(owner, null);
                    }
                }
            }
            break;
            case ON_DESTROY:
            {
                DependencyManager manager = DependencyManager.sTable.remove(owner);
                if (manager != null)
                {
                    manager.onDestroy();
                }
            }
            break;
        }
    }

    private static void inject(Object object, DependencyManager dependency)
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
}
