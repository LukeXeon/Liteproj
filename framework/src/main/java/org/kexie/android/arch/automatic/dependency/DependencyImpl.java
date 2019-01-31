package org.kexie.android.arch.automatic.dependency;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class DependencyImpl
        extends Dependency
{
    private final Class<?> ownerType;
    private final WeakReference<Object> owner;
    private final Map<DependencyRelation, Dependency> dependencies
            = new ArrayMap<>();
    private final Map<String, Object> singletons
            = new ArrayMap<>();

    public static Dependency newInstance(@NonNull Object owner,
                                         @NonNull List<DependencyRelation> relations)
    {
        if (relations.size() > 1)
        {
            Set<String> set = relations.get(0).nameSet();
            Set<String> result = new ArraySet<>();
            for (DependencyRelation relation
                    : relations.subList(1, relations.size()))
            {
                Set<String> newSet = relation.nameSet();
                result.addAll(set);
                result.retainAll(newSet);
                if (result.size() == 0)
                {
                    set.addAll(newSet);
                } else
                {
                    throw new RuntimeException(result.toString());
                }
            }
        }
        return new DependencyImpl(owner, relations);
    }

    @SuppressWarnings({"unchecked"})
    private DependencyImpl(@NonNull Object owner,
                          @NonNull List<DependencyRelation> relations)
    {
        this.ownerType = owner.getClass();
        this.owner = new WeakReference<>(owner);
        for (DependencyRelation relation : relations)
        {
            Class<?> requireType = relation.getOwnerType();
            if (requireType.isAssignableFrom(ownerType))
            {
                dependencies.put(relation, this);
            } else if (AppCompatActivity.class.isAssignableFrom(ownerType)
                    || Application.class.isAssignableFrom(requireType))
            {
                dependencies.put(relation,
                        new ActivityToAppCompat(this,
                                (Class<? extends Application>) requireType));
            } else if (Fragment.class.isAssignableFrom(ownerType)
                    || AppCompatActivity.class.isAssignableFrom(requireType))
            {
                dependencies.put(relation,
                        new FragmentToActivityCompat(this,
                                (Class<? extends AppCompatActivity>) requireType));
            } else if (Fragment.class.isAssignableFrom(ownerType)
                    || Application.class.isAssignableFrom(requireType))
            {
                dependencies.put(relation,
                        new FragmentToAppCompat(this,
                                (Class<? extends Application>) requireType));
            } else
            {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @NonNull
    public <T> T getOwner()
    {
        return (T)Objects.requireNonNull(owner.get(),
                "owner has been released");
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T get(String name)
    {
        if (OWNER.equals(name))
        {
            return getOwner();
        }
        for (DependencyRelation relation : dependencies.keySet())
        {
            DependencyProvider provider = relation.getProvider(name);
            if (provider != null)
            {
                if (DependencyType.Singleton.equals(provider.getType()))
                {
                    Object singleton = singletons.get(name);
                    if (singleton == null)
                    {
                        singleton = provider.newInstance(dependencies.get(relation));
                        singletons.put(name,singleton);
                    }
                    return (T) singleton;
                }
                return provider.newInstance(dependencies.get(relation));
            }
        }
        throw new NoSuchElementException(name);
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public Class<?> getResultType(String name)
    {
        if (OWNER.equals(name))
        {
            return ownerType;
        }
        for (DependencyRelation item : dependencies.keySet())
        {
            DependencyProvider provider = item.getProvider(name);
            if (provider != null)
            {
                return provider.getResultType();
            }
        }
        throw new NoSuchElementException(name);
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public DependencyType getDependencyType(String name)
    {
        if (OWNER.equals(name))
        {
            return DependencyType.Singleton;
        }
        for (DependencyRelation item : dependencies.keySet())
        {
            DependencyProvider provider = item.getProvider(name);
            if (provider != null)
            {
                return provider.getType();
            }
        }
        throw new NoSuchElementException(name);
    }
}
