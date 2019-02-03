package org.kexie.android.liteproj.analyzer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;


import java.util.Map;
import java.util.Set;

//从xml解析出来得到的依赖信息
public final class Dependency
{
    private final Class<?> mOwnerType;
    private final Map<String, Provider> mProviders;

    Dependency(@NonNull Class<?> ownerType,
               @NonNull Map<String, Provider> providers)
    {
        this.mOwnerType = ownerType;
        this.mProviders = providers;
    }

    @NonNull
    public Set<String> getNames()
    {
        return new ArraySet<>(mProviders.keySet());
    }

    @NonNull
    public Class<?> getOwnerType()
    {
        return mOwnerType;
    }

    @Nullable
    public Provider getProvider(String name)
    {
        return mProviders.get(name);
    }
}
