package org.kexie.android.liteproj;

import android.support.annotation.NonNull;

interface Provider
{
    @NonNull
    <T> T provide(@NonNull DependencyManager dependencyManager);

    @NonNull
    DependencyType getType();

    @NonNull
    Class<?> getResultType();
}
