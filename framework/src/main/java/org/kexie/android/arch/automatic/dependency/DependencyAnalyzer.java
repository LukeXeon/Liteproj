package org.kexie.android.arch.automatic.dependency;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.support.v4.util.LruCache;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyAnalyzer
{
    private final static SAXReader SAX_READER
            = new SAXReader();
    //DependencyRelation
    private final static LruCache<Integer, DependencyRelation> CACHE
            = new LruCache<>(32);

    //返回DependencyImpl
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public static Dependency analysis(Object owner, Context context)
    {
        int[] ids = getResIds(owner);
        if (ids == null)
        {
            return null;
        }
        List<DependencyRelation> list = new ArrayList<>();
        for (int id : ids)
        {
            DependencyRelation relation = findCache(id);
            if (relation == null)
            {

            }
            list.add(relation);
        }
        return DependencyImpl.newInstance(owner, list);
    }

    @Nullable
    private static DependencyRelation findCache(@RawRes int rawRes)
    {
        return CACHE.get(rawRes);
    }

    @Nullable
    private static int[] getResIds(Object owner)
    {
        Using using = owner.getClass().getAnnotation(Using.class);
        if (using != null)
        {
            return using.value();
        }
        return null;
    }

    private final Class<?> ownerType;

    private final Context context;

    private final Set<Integer> includes = new ArraySet<>();

    private final Map<String, DependencyProvider> providers = new ArrayMap<>();

    private DependencyAnalyzer(Class<?> ownerType,
                               int rawXml,
                               Context context)
    {
        this.ownerType = ownerType;
        this.context = context.getApplicationContext();
        analysis(getDocument(rawXml));
    }

    private Document getDocument(@RawRes int rawXml)
    {
        try (InputStream stream = context.getResources().openRawResource(rawXml))
        {
            return SAX_READER.read(stream);
        } catch (IOException | DocumentException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void analysis(Document document)
    {

    }



}
