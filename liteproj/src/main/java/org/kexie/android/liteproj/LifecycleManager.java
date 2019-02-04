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

import org.kexie.android.liteproj.analyzer.Dependency;
import org.kexie.android.liteproj.analyzer.DependencyAnalyzer;
import org.kexie.android.liteproj.util.TextType;
import org.kexie.android.liteproj.util.TextUtil;
import org.kexie.android.liteproj.util.TypeUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class LifecycleManager
{
    private LifecycleManager()
    {
        throw new AssertionError();
    }

    private static DependencyAnalyzer sAnalyzer;

    private static Class<?> sApplicationType;

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
            onDetach(f);
        }
    };

    private final static Application.ActivityLifecycleCallbacks
            sActivityCallbacks = new LifecycleHandler()
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
            onDetach(activity);
        }
    };

    private static final String TAG = "LifecycleManager";

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

    static synchronized void init(@NonNull Context context)
    {
        if (sAnalyzer == null)
        {
            Application application = (Application) context.getApplicationContext();
            sAnalyzer = new DependencyAnalyzer(application);
            sApplicationType = application.getClass();
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
            Using using = owner.getClass().getAnnotation(Using.class);
            if (using != null)
            {
                List<Dependency> dependencies = new LinkedList<>();
                if (using.value().length != 0)
                {
                    for (int resId : using.value())
                    {
                        dependencies.add(sAnalyzer.analysis(resId));
                    }
                }
                if (using.assets().length != 0)
                {
                    for (String asset : using.assets())
                    {
                        dependencies.add(sAnalyzer.analysis(asset));
                    }
                }
                if (check(owner.getClass(), dependencies))
                {
                    manager = new DependencyManager(owner, dependencies);
                } else
                {
                    throw new RuntimeException("Error in @Using resources");
                }
            }
            DependencyManager.sTable.put(owner, manager);
            if (manager != null)
            {
                inject(owner, manager);
            }
        }
    }

    private static boolean check(@NonNull Class<?> ownerType,
                                 @NonNull List<Dependency> dependencies)
    {
        if (dependencies.size() == 0)
        {
            return false;
        }
        for (Dependency dependency : dependencies)
        {
            Class<?> dOwnerType = dependency.getOwnerType();
            if (!dOwnerType.isAssignableFrom(ownerType))
            {
                if (!(FragmentActivity.class.isAssignableFrom(ownerType)
                        || LiteViewModel.class.isAssignableFrom(ownerType)
                        || LiteService.class.isAssignableFrom(ownerType)))
                {
                    if (!Fragment.class.isAssignableFrom(ownerType)
                        && (sApplicationType.equals(dOwnerType)
                            ||FragmentActivity.class.isAssignableFrom(dOwnerType)))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static void onDetach(@NonNull Object owner)
    {
        DependencyManager manager = DependencyManager.sTable.remove(owner);
        if (manager != null)
        {
            manager.onDestroy();
        }
    }
}
