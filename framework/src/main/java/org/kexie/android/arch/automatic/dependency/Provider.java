package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

interface Provider extends Factory
{
    @NonNull
    <T> T newInstance(Dependency dependency);

    @NonNull
    DependencyType getType();

    @NonNull
    Class<?> getResultType();
}
