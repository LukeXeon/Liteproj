package org.kexie.android.liteproj.internal;

import android.support.annotation.NonNull;

import org.kexie.android.liteproj.DependencyManager;

interface Setter
{
    void set(@NonNull Object target,
             @NonNull DependencyManager dependency);
}
