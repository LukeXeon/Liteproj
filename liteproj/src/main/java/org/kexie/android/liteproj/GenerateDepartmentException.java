package org.kexie.android.liteproj;

import android.support.annotation.NonNull;

@SuppressWarnings("WeakerAccess")
public class GenerateDepartmentException
        extends RuntimeException
{
    GenerateDepartmentException(@NonNull String message)
    {
        super(message);
    }

    GenerateDepartmentException(@NonNull String message, @NonNull Throwable e)
    {
        super(message, e);
    }

    GenerateDepartmentException(@NonNull Throwable e)
    {
        super(e);
    }
}
