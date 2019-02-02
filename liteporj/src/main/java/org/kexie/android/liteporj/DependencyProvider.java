package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

import java.util.List;

final class DependencyProvider implements Provider
{
    private final DependencyType mType;
    private final Factory mFactory;
    private final List<Setter> mSetters;

    DependencyProvider(@NonNull DependencyType type,
                       @NonNull Factory factory,
                       @NonNull List<Setter> setters)
    {
        this.mType = type;
        this.mFactory = factory;
        this.mSetters = setters;
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T provide(@NonNull DependencyManager dependencyManager)
    {
        Object target = mFactory.newInstance(dependencyManager);
        for (Setter setter : mSetters)
        {
            setter.set(target, dependencyManager);
        }
        return (T) target;
    }

    @Override
    @NonNull
    public DependencyType getType()
    {
        return mType;
    }

    @Override
    @NonNull
    public Class<?> getResultType()
    {
        return mFactory.getResultType();
    }
}
