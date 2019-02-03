package org.kexie.android.liteproj.util;

import android.support.annotation.NonNull;

public interface Filter<T>
{
    boolean filter(@NonNull T item);
}
