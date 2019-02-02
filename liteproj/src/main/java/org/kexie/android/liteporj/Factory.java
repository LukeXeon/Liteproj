package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

interface Factory
{
    @NonNull
    <T> T newInstance(@NonNull DependencyManager dependencyManager);

    @NonNull
    Class<?> getResultType();
}
