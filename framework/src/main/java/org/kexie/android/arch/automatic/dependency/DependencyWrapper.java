package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

public class DependencyWrapper extends Dependency
{
    private final Dependency base;

    public DependencyWrapper(Dependency base)
    {
        this.base = base;
    }

    @NonNull
    @Override
    public <T> T getOwner()
    {
        return base.getOwner();
    }

    @NonNull
    @Override
    public <T> T get(String name)
    {
        return base.get(name);
    }

    @NonNull
    @Override
    public Class<?> getResultType(String name)
    {
        return base.getResultType(name);
    }

    @NonNull
    @Override
    public DependencyType getDependencyType(String name)
    {
        return base.getDependencyType(name);
    }
}
