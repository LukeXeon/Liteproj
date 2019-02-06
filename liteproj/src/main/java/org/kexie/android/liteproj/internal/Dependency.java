package org.kexie.android.liteproj.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ArraySet;

import java.util.Map;
import java.util.Set;

//从xml解析出来得到的依赖信息
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Dependency
{
    private final Class<?> mOwnerType;
    private final Map<String, Provider> mReferences;

    Dependency(@NonNull Class<?> ownerType,
               @NonNull Map<String, Provider> references)
    {
        this.mOwnerType = ownerType;
        this.mReferences = references;
    }

    @NonNull
    public Set<String> getReferences()
    {
        return new ArraySet<>(mReferences.keySet());
    }

    @NonNull
    public Class<?> getOwnerType()
    {
        return mOwnerType;
    }

    @Nullable
    public Provider getProvider(String name)
    {
        return mReferences.get(name);
    }
}
