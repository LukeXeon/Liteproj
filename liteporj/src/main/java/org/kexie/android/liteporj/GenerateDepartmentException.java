package org.kexie.android.liteporj;

public class GenerateDepartmentException
        extends RuntimeException
{
    GenerateDepartmentException(String message)
    {
        super(message);
    }

    GenerateDepartmentException(String message, Throwable e)
    {
        super(message, e);
    }

    GenerateDepartmentException(Throwable e)
    {
        super(e);
    }
}
