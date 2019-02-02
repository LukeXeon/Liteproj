package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

interface Provider
{
    @NonNull
    <T> T provide(DependencyManager dependencyManager);

    @NonNull
    DependencyType getType();

    @NonNull
    Class<?> getResultType();
}
