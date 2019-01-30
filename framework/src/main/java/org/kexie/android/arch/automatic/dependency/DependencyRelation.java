package org.kexie.android.arch.automatic.dependency;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class DependencyRelation
{
    private final Class<?> ownerType;
    private final Map<String, DependencyProvider> providers;

    private DependencyRelation(@NonNull Class<?> ownerType,
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

    public Set<String> nameSet()
    {
        return new ArraySet<>(providers.keySet());
    }

    public Class<?> getOwnerType()
    {
        return ownerType;
    }

    @Nullable
    protected DependencyProvider getProvider(String name)
    {
        return providers.get(name);
    }
}
