package org.kexie.android.arch.automatic.internal;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.kexie.android.arch.automatic.dependency.Dependencies;
import org.kexie.android.arch.automatic.permissson.Permissions;

import java.util.Objects;

public final class AutomaticInitializer extends ContentProvider
{
    @Override
    public boolean onCreate()
    {
        Application application = (Application) Objects
                .requireNonNull(getContext())
                .getApplicationContext();
        Permissions.init(application);
        Dependencies.init(application);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] strings,
                        @Nullable String s,
                        @Nullable String[] strings1,
                        @Nullable String s1)
    {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri)
    {
        return null;
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri,
                      @Nullable ContentValues contentValues)
    {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri,
                      @Nullable String s,
                      @Nullable String[] strings)
    {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri,
                      @Nullable ContentValues contentValues,
                      @Nullable String s,
                      @Nullable String[] strings)
    {
        return 0;
    }
}
