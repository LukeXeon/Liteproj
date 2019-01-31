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

public final class Dependency
{
    public final static String OWNER = "owner";

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
        return new Dependency(owner, relations);
    }

    @SuppressWarnings({"unchecked"})
    private Dependency(@NonNull Object owner,
                          @NonNull List<DependencyRelation> relations)
    {
        this.ownerType = owner.getClass();
        Analyzing.checkSupportTypeCompat(ownerType);
        this.owner = new WeakReference<>(owner);
        for (DependencyRelation relation : relations)
        {
            Class<?> requireType = relation.getOwnerType();
            if (requireType.isAssignableFrom(ownerType))
            {
                dependencies.put(relation, this);
            } else if (owner instanceof AppCompatActivity
                    && Application.class.isAssignableFrom(requireType))
            {
                Dependency dependency = DependenciesManager
                        .of(((AppCompatActivity) owner)
                                .getApplicationContext());
                if (dependency != null)
                {
                    dependencies.put(relation, dependency);
                }
            } else if (owner instanceof Fragment
                    && AppCompatActivity.class.isAssignableFrom(requireType))
            {
                Dependency dependency = DependenciesManager
                        .of(Objects.requireNonNull(((Fragment) owner)
                                .getActivity()));
                if (dependency != null)
                {
                    dependencies.put(relation, dependency);
                }
            } else if (owner instanceof Fragment
                    && Application.class.isAssignableFrom(requireType))
            {
                Dependency dependency = DependenciesManager
                        .of(Objects.requireNonNull(((Fragment) owner)
                                .getActivity()).getApplicationContext());
                if (dependency != null)
                {
                    dependencies.put(relation, dependency);
                }
            } else
            {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @NonNull
    private Object getOwner()
    {
        return Objects.requireNonNull(owner.get(),
                "owner has been released");
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    public <T> T get(String name)
    {
        if (OWNER.equals(name))
        {
            return (T)getOwner();
        }
        for (DependencyRelation relation : dependencies.keySet())
        {
            Provider provider = relation.getProvider(name);
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
    public Class<?> getResultType(String name)
    {
        if (OWNER.equals(name))
        {
            return ownerType;
        }
        for (DependencyRelation item : dependencies.keySet())
        {
            Provider provider = item.getProvider(name);
            if (provider != null)
            {
                return provider.getResultType();
            }
        }
        throw new NoSuchElementException(name);
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    public DependencyType getDependencyType(String name)
    {
        if (OWNER.equals(name))
        {
            return DependencyType.Singleton;
        }
        for (DependencyRelation item : dependencies.keySet())
        {
            Provider provider = item.getProvider(name);
            if (provider != null)
            {
                return provider.getType();
            }
        }
        throw new NoSuchElementException(name);
    }
}
