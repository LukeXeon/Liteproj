package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;
import android.support.annotation.RawRes;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface Using
{
    @NonNull
    int[] value();
}
