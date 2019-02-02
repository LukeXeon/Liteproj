package org.kexie.android.liteproj;

import android.support.annotation.NonNull;

interface Setter
{
    void set(@NonNull Object target,@NonNull DependencyManager dependency);
}
