package org.kexie.android.liteproj.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ArraySet;

import org.kexie.android.liteproj.util.TextType;
import org.kexie.android.liteproj.util.TextUtil;

import java.util.Map;
import java.util.Set;

//从xml解析出来得到的依赖信息
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    public Set<String> getReferences()
    {
        Set<String> result = new ArraySet<>();
        for (String key : mProviders.keySet())
        {
            if (!TextType.CONSTANT.equals(TextUtil.getTextType(key)))
            {
                result.add(key);
            }
        }
        return result;
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
