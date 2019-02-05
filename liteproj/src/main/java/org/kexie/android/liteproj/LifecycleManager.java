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

import org.kexie.android.liteproj.internal.DependencyAnalyzer;
import org.kexie.android.liteproj.internal.Dependency;
import org.kexie.android.liteproj.util.TextType;
import org.kexie.android.liteproj.util.TextUtil;
import org.kexie.android.liteproj.util.TypeUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

final class LifecycleManager
{
    private LifecycleManager()
    {
        throw new AssertionError();
    }

    private static final String TAG = "LifecycleManager";

    private static DependencyAnalyzer sDependencyAnalyzer;

    private static final Executor sAnalysisTaskExecutor
            = Executors.newCachedThreadPool();

    private static Class<?> sApplicationType;

    private final static FragmentManager.FragmentLifecycleCallbacks
            sFragmentCallbacks = new FragmentManager.FragmentLifecycleCallbacks()
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

    private static void inject(@NonNull Object object,
                               @NonNull DependencyManager dependency)
    {
        Log.d(TAG, String.format("inject to %s", object));
        Class<?> type = object.getClass();
        Set<Class<?>> frameworkTypes = new ArraySet<>();
        frameworkTypes.add(Application.class);
        frameworkTypes.add(FragmentActivity.class);
        frameworkTypes.add(Fragment.class);
        frameworkTypes.add(LiteService.class);
        frameworkTypes.add(LiteViewModel.class);
        while (type != null && !frameworkTypes.contains(type))
        {
            for (Field field : type.getDeclaredFields())
            {
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null)
                {
                    if (!TextType.REFERENCE.equals(TextUtil.getTextType(reference.value())))
                    {
                        throw new IllegalStateException(
                                String.format("The name '%s' illegal", reference.value()));
                    }
                    int modifiers = field.getModifiers();
                    if (!Modifier.isFinal(modifiers)
                            && !Modifier.isStatic(modifiers)
                            && TypeUtil.isAssignToType(
                            dependency.getResultType(reference.value()),
                            field.getType()))
                    {
                        field.setAccessible(true);
                        try
                        {
                            field.set(object, TypeUtil.castToType(
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
                    if (!TextType.REFERENCE.equals(TextUtil.getTextType(reference.value())))
                    {
                        throw new IllegalStateException(
                                String.format("The name '%s' illegal", reference.value()));
                    }
                    int modifiers = property.getModifiers();
                    String name = property.getName();
                    Class<?>[] parameterTypes = property.getParameterTypes();
                    if (name.length() >= 3
                            && "set".equals(name.substring(0, 2))
                            && parameterTypes.length == 1
                            && !Modifier.isStatic(modifiers)
                            && !Modifier.isAbstract(modifiers)
                            && TypeUtil.isAssignToType(dependency
                                    .getResultType(reference.value()),
                            parameterTypes[0]))
                    {
                        property.setAccessible(true);
                        try
                        {
                            property.invoke(object,
                                    TypeUtil.castToType(dependency.get(reference.value()),
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

    private static void check(@NonNull Class<?> ownerType,
                              @NonNull List<Dependency> dependencies)
    {
        if (dependencies.size() == 0)
        {
            throw new IllegalStateException(String.format(
                    "%s @Using not reference resource",
                    ownerType));
        }
        Set<String> set = new ArraySet<>();
        Set<String> result = new ArraySet<>();
        for (Dependency dependency : dependencies)
        {
            Set<String> newSet = dependency.getReferences();
            result.addAll(set);
            result.retainAll(newSet);
            if (result.isEmpty())
            {
                set.addAll(newSet);
            } else
            {
                throw new RuntimeException(String.format(
                        "Name conflicts occur when merging dependencies," +
                                " and the set of conflicts is %s",
                        result.toString()));
            }
            Class<?> dOwnerType = dependency.getOwnerType();
            if (!dOwnerType.isAssignableFrom(ownerType))
            {
                if (!((FragmentActivity.class.isAssignableFrom(ownerType)
                        || LiteViewModel.class.isAssignableFrom(ownerType)
                        || LiteService.class.isAssignableFrom(ownerType))
                        && dOwnerType.isAssignableFrom(sApplicationType)))
                {
                    if (!(Fragment.class.isAssignableFrom(ownerType)
                            && (dOwnerType.isAssignableFrom(sApplicationType)
                            || FragmentActivity.class.isAssignableFrom(dOwnerType))))
                    {
                        throw new RuntimeException(
                                String.format("Error in @Using resources " +
                                                "dependency type %s owner type %s",
                                        dOwnerType,
                                        ownerType));
                    }
                }
            }
        }
    }

    static void init(@NonNull Context context)
    {
        if (sInitialized.getAndSet(true))
        {
            return;
        }
        Application application = (Application) context.getApplicationContext();
        sDependencyAnalyzer = new DependencyAnalyzer(application);
        sApplicationType = application.getClass();
        application.registerActivityLifecycleCallbacks(sActivityCallbacks);
        attachTo(application);
    }

    static void attachTo(@NonNull Object owner)
    {
        if (!DependencyManager.sTable.containsKey(owner))
        {
            if (owner instanceof FragmentActivity)
            {
                ((FragmentActivity) owner).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(sFragmentCallbacks,
                                true);
            } else if (owner instanceof Activity)
            {
                Log.w(TAG, String.format(
                        "Using this %s type will cause fragments below to fail" +
                                " to receive injection events," +
                                " so you'd better use FragmentActivity",
                        owner.getClass()));
            }
            DependencyManager manager = null;
            Using using = owner.getClass().getAnnotation(Using.class);
            if (using != null)
            {
                List<FutureTask<Dependency>> analysisTasks = new LinkedList<>();
                if (using.value().length != 0)
                {
                    Set<Integer> idSet = new ArraySet<>(using.value().length);
                    for (final int resId : using.value())
                    {
                        if (!idSet.contains(resId))
                        {
                            idSet.add(resId);
                            FutureTask<Dependency> task = new FutureTask<>(
                                    new Callable<Dependency>()
                                    {
                                        @Override
                                        public Dependency call()
                                        {
                                            return sDependencyAnalyzer.analysis(resId);
                                        }
                                    });
                            sAnalysisTaskExecutor.execute(task);
                            analysisTasks.add(task);
                        }
                    }
                }
                if (using.assets().length != 0)
                {
                    for (final String asset
                            : new ArraySet<>(Arrays.asList(using.assets())))
                    {
                        FutureTask<Dependency> task = new FutureTask<>(
                                new Callable<Dependency>()
                                {
                                    @Override
                                    public Dependency call()
                                    {
                                        return sDependencyAnalyzer.analysis(asset);
                                    }
                                });
                        sAnalysisTaskExecutor.execute(task);
                        analysisTasks.add(task);
                    }
                }
                List<Dependency> dependencies = new LinkedList<>();
                for (FutureTask<Dependency> task : analysisTasks)
                {
                    try
                    {
                        dependencies.add(task.get());
                    } catch (InterruptedException | ExecutionException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                check(owner.getClass(), dependencies);
                manager = new DependencyManager(owner, dependencies);
            }
            DependencyManager.sTable.put(owner, manager);
            if (manager != null)
            {
                inject(owner, manager);
            }
        }
    }

    static void detachFrom(@NonNull Object owner)
    {
        DependencyManager manager = DependencyManager.sTable.remove(owner);
        if (manager != null)
        {
            manager.onDestroy();
        }
    }
}