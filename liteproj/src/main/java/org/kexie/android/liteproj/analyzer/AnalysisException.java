package org.kexie.android.liteproj.analyzer;

import android.support.annotation.NonNull;

@SuppressWarnings("WeakerAccess")
public class AnalysisException extends RuntimeException
{
    AnalysisException(@NonNull String message)
    {
        super(message);
    }

    AnalysisException(@NonNull String message, @NonNull Throwable e)
    {
        super(message, e);
    }

    AnalysisException(@NonNull Throwable e)
    {
        super(e);
    }
}
