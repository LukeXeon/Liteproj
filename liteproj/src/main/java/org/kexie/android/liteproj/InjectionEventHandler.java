package org.kexie.android.liteproj;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.ArraySet;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class InjectionEventHandler
{
    private InjectionEventHandler()
    {
        throw new AssertionError();
    }

    private static DependencyAnalyzer sDependencyAnalyzer;

    private final static FragmentManager.FragmentLifecycleCallbacks
            sFragmentCallbacks = new FragmentManager.FragmentLifecycleCallbacks()
    {
        @Override
        public void onFragmentAttached(@NonNull FragmentManager fm,
                                       @NonNull Fragment f,
                                       @NonNull Context context)
        {
            onAttach(f);
        }

        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm,
                                        @NonNull Fragment f)
        {
            onDestroy(f);
        }
    };

    private final static Application.ActivityLifecycleCallbacks
            sActivityCallbacks = new EmptyLifecycleCallbacks()
    {
        @Override
        public void onActivityCreated(Activity activity,
                                      Bundle savedInstanceState)
        {
            onAttach(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity)
        {
            onDestroy(activity);
        }
    };

    private static final String TAG = "InjectionEventHandler";

    private static void inject(@NonNull Object object,
                               @NonNull DependencyManager dependency)
    {
        Log.d(TAG, String.format("inject to %s", object));
        Class<?> type = object.getClass();
        Set<Class<?>> baseTypes = new ArraySet<>();
        baseTypes.add(FragmentActivity.class);
        baseTypes.add(Fragment.class);
        baseTypes.add(Application.class);
        baseTypes.add(LiteService.class);
        baseTypes.add(LiteViewModel.class);
        while (type != null && !baseTypes.contains(type))
        {
            for (Field field : type.getDeclaredFields())
            {
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null)
                {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isFinal(modifiers)
                            && !Modifier.isStatic(modifiers)
                            && AnalyzerEnv.isAssignTo(
                            dependency.getResultType(reference.value()),
                            field.getType()))
                    {
                        field.setAccessible(true);
                        try
                        {
                            field.set(object, AnalyzerEnv.castTo(
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
                            && AnalyzerEnv.isAssignTo(dependency
                                    .getResultType(reference.value()),
                            parameterTypes[0]))
                    {
                        property.setAccessible(true);
                        try
                        {
                            property.invoke(object,
                                    AnalyzerEnv.castTo(dependency.get(reference.value()),
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

    static synchronized void init(@NonNull Application application)
    {
        if (sDependencyAnalyzer == null)
        {
            sDependencyAnalyzer = new DependencyAnalyzer(application);
            application.registerActivityLifecycleCallbacks(sActivityCallbacks);
            onAttach(application);
        }
    }

    static void onAttach(@NonNull Object owner)
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
                org.kexie.android.liteproj.InjectionEventHandler.inject(owner, manager);
            }
        }
    }

    static void onDestroy(@NonNull Object owner)
    {
        DependencyManager manager = DependencyManager.sTable.remove(owner);
        if (manager != null)
        {
            manager.onDestroy();
        }
    }
}
