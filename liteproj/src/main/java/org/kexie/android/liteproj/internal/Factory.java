package org.kexie.android.liteproj.internal;

import android.support.annotation.NonNull;

import org.kexie.android.liteproj.DependencyManager;

interface Factory
{
    @NonNull
    <T> T newInstance(@NonNull DependencyManager dependencyManager);

    @NonNull
    Class<?> getResultType();
}
