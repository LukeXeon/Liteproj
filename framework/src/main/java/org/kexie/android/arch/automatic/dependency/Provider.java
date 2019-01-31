package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

public interface Provider extends Factory
{
    @NonNull
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    <T> T newInstance(Dependency dependency);

    @NonNull
    @SuppressWarnings("WeakerAccess")
    DependencyType getType();

    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    Class<?> getResultType();
}
