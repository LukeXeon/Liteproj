package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

import java.util.List;

final class DependencyProvider implements Provider
{
    private final DependencyType mType;
    private final Factory mFactory;
    private final List<Setter> mSetters;

    DependencyProvider(DependencyType type, Factory factory, List<Setter> setters)
    {
        this.mType = type;
        this.mFactory = factory;
        this.mSetters = setters;
    }

    @NonNull
    public <T> T provide(DependencyManager dependencyManager)
    {
        return null;
    }

    @NonNull
    public DependencyType getType()
    {
        return null;
    }

    @NonNull
    public Class<?> getResultType()
    {
        return null;
    }
}
