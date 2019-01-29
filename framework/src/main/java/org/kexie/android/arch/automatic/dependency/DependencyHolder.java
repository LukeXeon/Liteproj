package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;
import android.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;

public final class DependencyHolder
        extends Dependency
{
    private WeakReference<Object> owner;
    private Dependency internal;
    private Map<String, Object> singletons = new ArrayMap<>();

    public DependencyHolder(@NonNull Object owner, @NonNull Dependency internal)
    {
        if (!internal.getOwnerType().equals(owner.getClass()))
        {
            throw new IllegalStateException("owner type no match");
        }
        this.owner = new WeakReference<>(owner);
        this.internal = internal;
    }

    private void selfCheck()
    {
        if (owner == null)
        {
            throw new IllegalStateException("dependency has been cleaned up");
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T> T getOwner()
    {
        return (T) Objects.requireNonNull(owner.get(),
                "context is release");
    }

    @NonNull
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T onGet(String name, Dependency dependency)
    {
        selfCheck();
        if (!equals(dependency))
        {
            throw new IllegalStateException(
                    "must from this getDependency"
            );
        }
        if (OWNER.equals(name))
        {
            return getOwner();
        }
        if (DependencyType.SINGLETON
                .equals(internal.getDependencyType(name)))
        {
            Object singleton = singletons.get(name);
            if (singleton == null)
            {
                singleton = internal.onGet(name, dependency);
                singletons.put(name, singleton);
            }
            return (T) singleton;
        } else
        {
            return internal.onGet(name, dependency);
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getResultType(String name)
    {
        selfCheck();
        return internal.getResultType(name);
    }

    @NonNull
    @Override
    public DependencyType getDependencyType(String name)
    {
        selfCheck();
        return internal.getDependencyType(name);
    }

    @Override
    public void clear()
    {
        selfCheck();
        owner = null;
        internal = null;
        singletons.clear();
        singletons = null;
    }
}
