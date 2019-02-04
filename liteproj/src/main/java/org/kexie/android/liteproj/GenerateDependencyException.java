package org.kexie.android.liteproj;

import android.support.annotation.NonNull;

public class GenerateDependencyException extends RuntimeException
{
    public GenerateDependencyException(@NonNull String message)
    {
        super(message);
    }

    public GenerateDependencyException(@NonNull String message,
                                       @NonNull Throwable e)
    {
        super(message, e);
    }

    public GenerateDependencyException(@NonNull Throwable e)
    {
        super(e);
    }
}
