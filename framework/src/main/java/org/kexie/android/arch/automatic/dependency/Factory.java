package org.kexie.android.arch.automatic.dependency;

interface Factory
{
    <T> T newInstance(Dependency dependency);

    Class<?> getResultType();
}
