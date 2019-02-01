package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

final class Singleton implements Factory
{
    private final Object object;
    private final Class<?> type;

    Singleton(@NonNull Object object)
    {
        this.object = object;
        this.type = object.getClass();
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T newInstance(DependencyManager dependencyManager)
    {
        return (T)object;
    }

    @NonNull
    @Override
    public Class<?> getResultType()
    {
        return type;
    }
}
