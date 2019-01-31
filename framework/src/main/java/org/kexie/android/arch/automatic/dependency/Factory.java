package org.kexie.android.arch.automatic.dependency;

public interface Factory
{
    <T> T newInstance(Dependency dependency);

    Class<?> getResultType();
}
