package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

import java.util.List;

public class Provider
{
    private final DependencyType mType;
    private final Factory mFactory;
    private final List<Setter> mSetters;

    public Provider(DependencyType type, Factory factory, List<Setter> setters)
    {
        this.mType = type;
        this.mFactory = factory;
        this.mSetters = setters;
    }

    @NonNull
    <T> T provide(DependencyManager dependencyManager)
    {
        return null;
    }

    @NonNull
    DependencyType getType()
    {
        return null;
    }

    @NonNull
    Class<?> getResultType()
    {
        return null;
    }
}
