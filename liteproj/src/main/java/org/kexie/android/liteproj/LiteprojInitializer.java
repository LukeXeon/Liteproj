package org.kexie.android.liteproj;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;


/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class LiteprojInitializer
        extends ContentProvider
{

    private static final String TAG = "LiteprojInitializer";

    @Override
    public boolean onCreate()
    {
        Context context = getContext();
        assert context != null;
        DependencyInjector.init(context);
        LifecycleManager.init(context);
        Log.d(TAG, "Liteproj init");
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
