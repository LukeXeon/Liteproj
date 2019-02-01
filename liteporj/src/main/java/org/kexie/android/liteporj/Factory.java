package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

public interface Factory
{
    @NonNull
    <T> T newInstance(DependencyManager dependencyManager);

    @NonNull
    Class<?> getResultType();
}
