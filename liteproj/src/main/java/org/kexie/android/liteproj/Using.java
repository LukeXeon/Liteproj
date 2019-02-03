package org.kexie.android.liteproj;

import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.annotation.XmlRes;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({TYPE})
public @interface Using
{
    @XmlRes
    @RawRes
    @NonNull
    int[] value();

    @NonNull
    String[] assets();
}
