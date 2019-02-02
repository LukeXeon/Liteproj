package org.kexie.android.liteporj;

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
