package org.kexie.android.liteporj;

import android.support.annotation.NonNull;

interface Setter
{
    void set(@NonNull Object target,@NonNull DependencyManager dependency);
}
