package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

public interface Dependency
{

    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    <T> T get(String name);

    @NonNull
    Class<?> getResultType(String name);

    @NonNull
    DependencyType getDependencyType(String name);

}
