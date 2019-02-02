package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

interface Filter<T>
{
    boolean filter(@NonNull T item);
}
