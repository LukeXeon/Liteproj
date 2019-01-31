package org.kexie.android.arch.automatic.dependency;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kexie.android.arch.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class DependencyAnalyzer
        extends ContextWrapper
{
    private final static SAXReader SAX_READER
            = new SAXReader();
    //DependencyRelation
    private final static LruCache<Integer, DependencyRelation> CACHE
            = new LruCache<>(32);

    //返回Dependency
    @Nullable
    @SuppressWarnings("WeakerAccess")
    public static Dependency analysis(Object owner, Context context)
    {
        int[] resIds = getResIds(owner);
        if (resIds == null)
        {
            return null;
        }
        List<DependencyRelation> list = new ArrayList<>();
        for (int resId : resIds)
        {
            DependencyRelation relation = CACHE.get(resId);
            if (relation == null)
            {
                DependencyAnalyzer analyzer
                        = new DependencyAnalyzer(context,
                        owner.getClass());
                relation = analyzer.doAnalysis(
                        analyzer.getDocument(resId)
                );
                CACHE.put(resId, relation);
            }
            list.add(relation);
        }
        return Dependency.newInstance(owner, list);
    }

    @Nullable
    private static int[] getResIds(Object owner)
    {
        Using using = owner.getClass().getAnnotation(Using.class);
        return using == null ? null : using.value();
    }

    private final Provider owner;

    private final Map<String, Provider> providers = new ArrayMap<>();

    private DependencyAnalyzer(Context context,
                               Class<?> ownerType)
    {
        super(context.getApplicationContext());
        this.owner = new OwnerProxyProvider(ownerType);
    }

    private Document getDocument(@RawRes int rawXml)
    {
        try (InputStream stream = getResources().openRawResource(rawXml))
        {
            return SAX_READER.read(stream);
        } catch (IOException | DocumentException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Provider getProvider(String name)
    {
        if (getString(R.string.owner_string).equals(name))
        {
            return owner;
        }
        return providers.get(name);
    }

    private void addProvider(String name, Provider provider)
    {
        if (!getString(R.string.owner_string).equals(name)
                && !providers.containsKey(name))
        {
            providers.put(name, provider);
        } else
        {
            throw new RuntimeException();
        }
    }

    @NonNull
    private DependencyRelation doAnalysis(Document document)
    {
        Element scope = document.getRootElement();
        checkOwnerType(scope);
        List<Element> elements = scope.elements();
        for (Element element : elements)
        {
            if (getString(R.string.var_string).equals(element.getName()))
            {
                addProvider(Analyzing.getAttrIfEmptyThrow(element,
                        getString(R.string.name_string)),
                        doAnalysisVar(element));
            } else
            {
                throw Analyzing.fromMessageThrow(element,
                        "no a 'var' tag");
            }
        }
        return new DependencyRelation(owner.getResultType(), providers);
    }

    private Provider doAnalysisVar(Element element)
    {
        String let = Analyzing.getAttrNoThrow(element,
                getString(R.string.let_string));
        if (let != null)
        {
            return doAnalysisLetVar(element);
        } else
        {
            return doAnalysisNormalVar(element);
        }
    }

    private Provider doAnalysisLetVar(Element element)
    {
        String let = Analyzing.getAttrIfEmptyThrow(element,
                getString(R.string.let_string));
        if (NameType.Constant.equals(Analyzing.getNameType(let)))
        {
            Provider provider = getProvider(let);
            if (provider == null)
            {
                try
                {
                    provider = Analyzing.newConstantProvider(let);
                    addProvider(let, provider);
                } catch (GenerateDepartmentException e)
                {
                    throw Analyzing.formExceptionThrow(element, e);
                }
            }
            return provider;
        } else
        {
            throw Analyzing.fromMessageThrow(element,
                    "illegal name " + let);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private Provider doAnalysisNormalVar(Element element)
    {
        Class<?> path = getClassAttr(element);
        boolean isSingleton = isSingleton(element);
        List<Element> elements = element.elements();
        List<Setter> setters = new LinkedList<>();
        Factory factory = doAnalysisNew(element, path);
        path = factory.getResultType();
        if (!Analyzing.isEmptyList(elements))
        {
            for (Element item : elements)
            {
                String name = item.getName();
                if (getString(R.string.new_string).equals(name))
                {
                } else if (getString(R.string.field_string).equals(name))
                {
                    setters.add(doAnalysisField(item, path));
                } else if (getString(R.string.property_string).equals(name))
                {
                    setters.add(doAnalysisProperty(element, path));
                } else
                {
                    throw Analyzing.fromMessageThrow(item,
                            "error token");
                }
            }
        }
        return new DependencyProvider(isSingleton
                ? DependencyType.Singleton
                : DependencyType.Factory,
                factory,
                setters);
    }

    private Setter doAnalysisField(Element element,Class<?> path)
    {
        return null;
    }

    private Setter doAnalysisProperty(Element element,Class<?> path)
    {
        return null;
    }

    private Factory doAnalysisNew(Element element, Class<?> path)
    {
        List<Element> elements = element.elements();
        Factory factory = null;
        if (Analyzing.isEmptyList(elements))
        {
            for (Element item : elements)
            {
                if (getString(R.string.new_string)
                        .equals(item.getName()))
                {
                    if (factory == null)
                    {
                        factory = doAnalysisNew0(item, path);
                    } else
                    {
                        throw Analyzing.fromMessageThrow(item,
                                "'new' must only one");
                    }
                }
            }
        }
        try
        {
            return factory == null
                    ? ReflectionUtil.newFactory(
                    ReflectionUtil.findSupportConstructor(path,
                            null,
                            null),
                    Collections.<String>emptyList())
                    : factory;
        } catch (NoSuchMethodException e)
        {
            throw Analyzing.formExceptionThrow(element, e);
        }
    }

    private Factory doAnalysisNew0(Element element, Class<?> path)
    {
        List<Element> elements = element.elements();
        List<String> refOrLet = new LinkedList<>();
        if (Analyzing.isEmptyList(elements))
        {
            for (Element item : elements)
            {
                if (getString(R.string.arg_string).equals(item.getName()))
                {
                    refOrLet.add(getRefOrLetAttr(item));
                } else
                {
                    throw Analyzing.fromMessageThrow(item,
                            "'arg' no found");
                }
            }
        }
        Class<?>[] classes = new Class<?>[refOrLet.size()];
        for (int i = 0; i < classes.length; i++)
        {
            try
            {
                classes[i] = getProviderClassIfNullThrow(refOrLet.get(i));
            } catch (GenerateDepartmentException e)
            {
                throw Analyzing.formExceptionThrow(element, e);
            }
        }
        String isCustom = Analyzing.getAttrNoThrow(element,
                getString(R.string.name_string));
        isCustom = isCustom != null
                && !getString(R.string.new_normal_string).equals(isCustom)
                ? isCustom : null;
        try
        {
            if (isCustom != null)
            {
                return ReflectionUtil.newFactory(
                        ReflectionUtil.findSupportMethod(
                                path,
                                isCustom,
                                classes,
                                new Filter<Method>()
                                {
                                    @Override
                                    public boolean filter(Method item)
                                    {
                                        return Modifier.isStatic(item.getModifiers())
                                                && !void.class
                                                .equals(item.getReturnType());
                                    }
                                }), refOrLet);
            } else
            {
                return ReflectionUtil.newFactory(
                        ReflectionUtil.findSupportConstructor(path,
                                classes,
                                null)
                        , refOrLet);
            }
        } catch (NoSuchMethodException e)
        {
            throw Analyzing.formExceptionThrow(element, e);
        }
    }

    private Class<?> getProviderClassIfNullThrow(String name)
            throws GenerateDepartmentException
    {
        Provider provider = providers.get(name);
        if (provider != null)
        {
            return provider.getResultType();
        } else
        {
            throw new GenerateDepartmentException(
                    "providers no has name=" + name
            );
        }
    }

    private String getRefOrLetAttr(Element element)
    {
        String ref = Analyzing.getAttrNoThrow(element,
                getString(R.string.ref_string));
        String let = Analyzing.getAttrNoThrow(element,
                getString(R.string.let_string));
        if (ref != null)
        {
            if (NameType.Reference.equals(
                    Analyzing.getNameType(ref)))
            {
                return ref;
            }
        } else if (let != null)
        {
            Provider provider = getProvider(let);
            if (provider != null)
            {
                provider = Analyzing.newConstantProvider(let);
                addProvider(let, provider);
            }
            return let;
        }
        throw Analyzing.fromMessageThrow(element,
                "ref or let illegal");
    }

    private boolean isSingleton(Element element)
    {
        String provider = Analyzing.getAttrNoThrow(element,
                getString(R.string.provider_string));
        if (TextUtils.isEmpty(provider))
        {
            return false;
        }
        if (getString(R.string.singleton_string).equals(provider))
        {
            return true;
        } else if (getString(R.string.factory_string).equals(provider))
        {
            return false;
        } else
        {
            throw Analyzing.fromMessageThrow(element,
                    "illegal provider=" + provider);
        }
    }

    private Class<?> getClassAttr(Element element)
    {
        try
        {
            return Class.forName(Analyzing.getAttrIfEmptyThrow(
                    element,
                    getString(R.string.class_string)
            ));
        } catch (ClassNotFoundException e)
        {
            throw Analyzing.formExceptionThrow(element, e);
        }
    }

    private void checkOwnerType(Element root)
    {
        if (getString(R.string.scope_string)
                .equals(root.getName()))
        {
            try
            {
                if (!Class.forName(Analyzing.getAttrIfEmptyThrow(root,
                        getString(R.string.owner_string)))
                        .equals(getProvider(getString(R.string.owner_string))
                                .getResultType()))
                {
                    throw Analyzing.fromMessageThrow(root,
                            "owner type not match");
                }
            } catch (ClassNotFoundException e)
            {
                throw Analyzing.formExceptionThrow(root, e);
            }
        }
    }
}
