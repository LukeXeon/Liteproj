package org.kexie.android.arch.automatic.dependency;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;



import org.kexie.android.arch.automatic.app.EmptyActivityLifecycleCallbacks;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
                    FragmentManager fragmentManager
                            = fragment.getChildFragmentManager();
                    fragmentManager.registerFragmentLifecycleCallbacks(
                            this,
                            false
                    );
                    HolderFragment.prepareInject(fragmentManager);
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
                            HolderFragment.prepareInject(fragmentManager);
                        }
                    }
                });
        appGlobal = DependencyAnalyzer.analysis(application, application);
        if (appGlobal != null)
        {
            inject(application, appGlobal);
        }
    }

    @SuppressWarnings("WeakerAccess")
    static void inject(Object object, Dependency dependency)
    {
        Class<?> type = object.getClass();
        while (type != null && !AnalyzerUtil.equalsToSupportTypes(type))
        {
            for (Field field : type.getDeclaredFields())
            {
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null)
                {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isFinal(modifiers)
                            && !Modifier.isStatic(modifiers)
                            && ReflectionUtil.isAssignTo(
                            dependency.getResultType(reference.value()),
                            field.getType()))
                    {
                        field.setAccessible(true);
                        try
                        {
                            field.set(object, dependency.get(reference.value()));
                        } catch (IllegalAccessException e)
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
                            && ReflectionUtil.isAssignTo(dependency
                                    .getResultType(reference.value()),
                            parameterTypes[0]))
                    {
                        property.setAccessible(true);
                        try
                        {
                            property.invoke(object, dependency.get(reference.value()));
                        } catch (IllegalAccessException | InvocationTargetException e)
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
