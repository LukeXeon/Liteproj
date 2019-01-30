package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

public abstract class Dependency
{
    @SuppressWarnings({"WeakerAccess"})
    public static final String OWNER = "owner";

    @NonNull
    public abstract <T> T getOwner();

    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    public abstract <T> T get(String name);

    @NonNull
    public abstract Class<?> getResultType(String name);

    @NonNull
    public abstract DependencyType getDependencyType(String name);
}
