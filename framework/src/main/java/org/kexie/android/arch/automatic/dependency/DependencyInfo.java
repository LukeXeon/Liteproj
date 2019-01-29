package org.kexie.android.arch.automatic.dependency;
;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class DependencyInfo
        extends Dependency
{
    private final Class<?> ownerType;
    private final Map<String, DependencyProvider> providers;

    private DependencyInfo(@NonNull Class<?> ownerType,
                           @Nullable Map<String, DependencyProvider> providers)
    {
        this.ownerType = ownerType;
        if (providers == null)
        {
            this.providers = Collections.emptyMap();
        } else
        {
            this.providers = new ArrayMap<>();
            this.providers.putAll(providers);
        }
    }

    @NonNull
    private DependencyProvider getProvider(String name)
    {
        return Objects.requireNonNull(providers.get(name),
                name + " no found");
    }

    @NonNull
    @Override
    protected <T> T onGet(String name, Dependency dependency)
    {
        if (equals(dependency))
        {
            throw new IllegalStateException(
                    "invoke get(String) method require form a proxy instance"
            );
        }
        return Objects.requireNonNull(getProvider(name)
                        .<T>newInstance(dependency),
                "can not provide null");
    }

    @NonNull
    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getResultType(String name)
    {
        if (OWNER.equals(name))
        {
            return (Class<T>) ownerType;
        }
        return getProvider(name).getResultType();
    }

    @NonNull
    @Override
    public DependencyType getDependencyType(String name)
    {
        if (OWNER.equals(name))
        {
            return DependencyType.SINGLETON;
        }
        return getProvider(name).getType();
    }

    @Override
    public void clear()
    {

    }
}
