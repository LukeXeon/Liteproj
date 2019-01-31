package org.kexie.android.arch.ioc;

final class ProviderAlreadyExistsException extends Exception
{
    ProviderAlreadyExistsException(String name)
    {
        super(name + "provider already exists");
    }
}
