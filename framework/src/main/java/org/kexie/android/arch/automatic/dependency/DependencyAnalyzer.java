package org.kexie.android.arch.automatic.dependency;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import org.dom4j.io.SAXReader;

public final class DependencyAnalyzer
{
    private final static SAXReader SAX_READER = new SAXReader();
    //缓存describe
    private final static LruCache<Integer, Dependency> CACHE = new LruCache<>(32);

    //返回已经attach的Dependency
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public static Dependency analysis(Object owner, Context context)
    {
        context = context.getApplicationContext();
        return null;
    }
}
