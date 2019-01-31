package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

interface Factory
{
    @NonNull
    <T> T newInstance(Dependency dependency);

    @NonNull
    Class<?> getResultType();
}
