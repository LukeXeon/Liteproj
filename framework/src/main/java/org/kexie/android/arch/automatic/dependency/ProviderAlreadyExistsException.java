package org.kexie.android.arch.automatic.dependency;

final class ProviderAlreadyExistsException extends Exception
{
    ProviderAlreadyExistsException(String name)
    {
        super(name + "provider already exists");
    }
}
