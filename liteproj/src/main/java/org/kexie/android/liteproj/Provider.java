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

    interface Factory
    {
        @NonNull
        <T> T newInstance(@NonNull DependencyManager dependencyManager);

        @NonNull
        Class<?> getResultType();
    }

    interface Setter
    {
        void set(@NonNull Object target, @NonNull DependencyManager dependency);
    }
}
