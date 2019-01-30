package org.kexie.android.arch.automatic.dependency;

import android.app.Application;
import android.content.Context;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyAnalyzer
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

    private final Context context;

    private final Set<Integer> includes = new ArraySet<>();

    private final Map<String, Provider> providers = new ArrayMap<>();

    private DependencyAnalyzer(final Class<?> ownerType,
                               int rawXml,
                               Context context)
    {
        this.ownerProxy = new Provider()
        {
            @NonNull
            @Override
            public <T> T newInstance(Dependency dependency)
            {
                throw new IllegalStateException("this a proxy");
            }

            @NonNull
            @Override
            public DependencyType getType()
            {
                return DependencyType.Singleton;
            }

            @NonNull
            @SuppressWarnings({"unchecked"})
            @Override
            public <T> Class<T> getResultType()
            {
                return (Class<T>) ownerType;
            }
        };
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
        Element scope = document.getRootElement();
        checkOwner(scope);
        List<Element> elements = scope.elements();
        for (Element item : elements)
        {
            String name = item.getName();
            switch (name)
            {
                case AnalyzerUtil.VAR:
                {
                    providers.put(name, findProvider(item));
                }
                break;
                case AnalyzerUtil.INCLUDE:
                {

                }
                break;
                default:
                {
                    throw AnalyzerUtil.runtimeException(item,
                            "no match tag " + name);
                }
            }
        }
    }

    private void checkOwner(Element scope)
    {
        String typeName = AnalyzerUtil.getAttr(scope,
                AnalyzerUtil.OWNER,
                "owner attr no found");
        assert typeName != null;
        try
        {
            Class<?> type = Class.forName(typeName);
            List<Class<?>> supportTypes = new LinkedList<Class<?>>()
            {
                {
                    add(Application.class);
                    add(AppCompatActivity.class);
                    add(Fragment.class);
                    add(LiteService.class);
                }
            };
            boolean isSupport = false;
            for (Class<?> support : supportTypes)
            {
                if (support.isAssignableFrom(type))
                {
                    isSupport = true;
                    break;
                }
            }
            if (!isSupport)
            {
                throw AnalyzerUtil.runtimeException(scope,
                        "no support type " + typeName);
            }
        } catch (ClassNotFoundException e)
        {
            throw AnalyzerUtil.runtimeException(scope, e);
        }
    }

    private Provider getProvider(String name)
    {
        if (AnalyzerUtil.OWNER.equals(name))
        {
            return ownerProxy;
        }
        return providers.get(name);
    }

    private Provider findProvider(Element element)
    {
        String let = AnalyzerUtil.getAttr(element, AnalyzerUtil.LET, null);
        if (let != null)
        {
            return findConstantProvider(element);
        } else
        {
            return findFactoryProvider(element);
        }
    }

    private Provider findConstantProvider(Element element)
    {
        String let = AnalyzerUtil.getAttr(element,
                AnalyzerUtil.LET,
                null);
        assert let != null;
        switch (AnalyzerUtil.getNameType(let))
        {
            case Constant:
            {
                Provider provider = getProvider(let);
                if (provider == null)
                {
                    String value = let.substring(1, let.length());
                    if (let.charAt(0) == '@')
                    {
                        return new DependencyProvider(
                                DependencyType.Singleton,
                                ReflectionUtil.newConstant(value),
                                null
                        );
                    } else
                    {
                        for (ValueOf valueOf : VALUE_OF)
                        {
                            try
                            {
                                return new DependencyProvider(
                                        DependencyType.Singleton,
                                        ReflectionUtil.newConstant(
                                                valueOf.valueOf(value)
                                        ),
                                        null
                                );
                            } catch (NumberFormatException ignored)
                            {

                            }
                        }
                        throw new AssertionError();
                    }
                }
                return provider;
            }
            default:
            {
                throw AnalyzerUtil.runtimeException(element, "the tag let illegal");
            }
        }
    }

    private Provider findFactoryProvider(Element element)
    {
        try
        {
            String typeName = AnalyzerUtil.getAttr(element,
                    AnalyzerUtil.CLASS,
                    "");
            assert typeName != null;
            Class<?> path = Class.forName(typeName);
            boolean isSingleton = isSingleton(element);
            List<Setter> setters = new LinkedList<>();
            Factory factory = findFactory(element, path);
            return null;
        } catch (ClassNotFoundException e)
        {
            throw AnalyzerUtil.runtimeException(element, e);
        }
    }

    private boolean isSingleton(Element element)
    {
        String provider = AnalyzerUtil.getAttr(element,
                AnalyzerUtil.PROVIDER,
                null);
        if (TextUtils.isEmpty(provider)
                || AnalyzerUtil.SINGLETON.equals(provider))
        {
            return true;
        } else if (AnalyzerUtil.FACTORY.equals(provider))
        {
            return false;
        } else
        {
            throw AnalyzerUtil.runtimeException(element, AnalyzerUtil.PROPERTY + " = " + provider);
        }
    }

    private Factory findFactory(Element element, Class<?> path)
    {
        return null;
    }
}
