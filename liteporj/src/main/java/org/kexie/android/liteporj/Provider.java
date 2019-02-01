package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

public interface Provider extends Factory
{
    @NonNull
    <T> T newInstance(DependencyManager dependencyManager);

    @NonNull
    DependencyType getType();

    @NonNull
    Class<?> getResultType();
}
