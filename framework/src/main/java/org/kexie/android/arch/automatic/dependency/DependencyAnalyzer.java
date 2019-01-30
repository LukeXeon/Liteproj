package org.kexie.android.arch.automatic.dependency;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kexie.android.arch.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyAnalyzer
        extends ContextWrapper
{
    private static final List<ValueOf> VALUE_OF
            = Collections.unmodifiableList(new LinkedList<ValueOf>()
    {
        {
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    if (!Character.isDigit(value.charAt(0)))
                    {
                        if (value.length() == 1)
                        {
                            return value.charAt(0);
                        } else if ("true".equals(value) || "false".equals(value))
                        {
                            return Boolean.valueOf(value);
                        }
                    }
                    throw new NumberFormatException();
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    return Byte.valueOf(value);
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    return Short.valueOf(value);
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    return Integer.valueOf(value);
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    return Long.valueOf(value);
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    return Float.valueOf(value);
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    return Double.valueOf(value);
                }
            });
            add(new ValueOf()
            {
                @Override
                public Object valueOf(String value)
                {
                    throw new RuntimeException("no type match " + value);
                }
            });
        }
    });

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

    private final Provider ownerProxy;

    private final Set<Integer> includes = new ArraySet<>();

    private final Map<String, Provider> providers = new ArrayMap<>();

    private DependencyAnalyzer(Context context, Class<?> ownerType, int rawXml)
    {
        super(context.getApplicationContext());
        this.ownerProxy = new OwnerProxyProvider(ownerType);
    }
}
