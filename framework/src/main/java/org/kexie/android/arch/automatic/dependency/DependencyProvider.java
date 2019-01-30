package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class DependencyProvider
        implements Provider
{

    private final DependencyType type;
    private final Factory factory;
    private final List<Setter> setters;

    public DependencyProvider(@NonNull DependencyType type,
                              @NonNull Factory factory,
                              @Nullable List<Setter> setters)
    {
        this.type = type;
        this.factory = factory;
        this.setters = setters == null
                ? Collections.<Setter>emptyList()
                : Collections.unmodifiableList(setters);
    }

    @Override
    @NonNull
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T> T newInstance(Dependency dependency)
    {
        Object target = factory.newInstance(dependency);
        for (Setter setter : setters)
        {
            setter.set(target, dependency);
        }
        return (T) target;
    }

    @Override
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public DependencyType getType()
    {
        return type;
    }

    @Override
    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    public Class<?> getResultType()
    {
        return factory.getResultType();
    }

}
