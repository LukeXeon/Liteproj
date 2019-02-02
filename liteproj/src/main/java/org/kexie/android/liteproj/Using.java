package org.kexie.android.liteproj;

import android.support.annotation.NonNull;
import android.support.annotation.RawRes;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({TYPE})
public @interface Using
{
    @RawRes
    @NonNull
    int[] value();
}
