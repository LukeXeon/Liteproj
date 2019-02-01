package org.kexie.android.arch.ioc;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final class DependencyAnalyzer
        extends ContextWrapper
{
    private final static SAXReader SAX_READER
            = new SAXReader();
    //DependencyRelation
    private final static LruCache<Integer, DependencyRelation> CACHE
            = new LruCache<>(32);

    //返回Dependency
    @Nullable
    public static Dependency analysis(Object owner, Context context)
    {
        int[] resIds = ReflectionUtil.getResIds(owner);
        if (resIds == null)
        {
            return null;
        }
        List<DependencyRelation> list = new LinkedList<>();
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

    private final Provider owner;

    private final Map<String, Provider> providers = new ArrayMap<>();

    private DependencyAnalyzer(Context context,
                               Class<?> ownerType)
    {
        super(context.getApplicationContext());
        this.owner = new OwnerSubstituteProvider(ownerType);
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
            throws ProviderAlreadyExistsException
    {
        if (!getString(R.string.owner_string).equals(name)
                && !providers.containsKey(name))
        {
            providers.put(name, provider);
        } else
        {
            throw new ProviderAlreadyExistsException(name);
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
                try
                {
                    addProvider(Analysing.getAttrIfEmptyThrow(element,
                            getString(R.string.name_string)),
                            doAnalysisVar(element));
                } catch (ProviderAlreadyExistsException e)
                {
                    throw Analysing.formExceptionThrow(element, e);
                }
            } else
            {
                throw Analysing.fromMessageThrow(element,
                        "no found 'var' tag");
            }
        }
        return new DependencyRelation(getProvider(
                getString(R.string.owner_string))
                .getResultType(),
                providers);
    }

    private Provider doAnalysisVar(Element element)
    {
        String let = Analysing.getAttrNoThrow(element,
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
        String let = Analysing.getAttrIfEmptyThrow(element,
                getString(R.string.let_string));
        if (TextType.Constant.equals(Analysing.getTextType(let)))
        {
            Provider provider = getProvider(let);
            if (provider == null)
            {
                try
                {
                    provider = Analysing.createConstantProvider(let);
                    addProvider(let, provider);
                } catch (IllegalFormatTextException
                        | ProviderAlreadyExistsException e)
                {
                    throw Analysing.formExceptionThrow(element, e);
                }
            }
            return provider;
        } else
        {
            throw Analysing.fromMessageThrow(element,
                    "illegal name " + let);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private Provider doAnalysisNormalVar(Element element)
    {
        Class<?> path = getClassAttr(element);
        List<Element> elements = element.elements();
        List<Setter> setters = new LinkedList<>();
        Factory factory = doSearchNew(element, path);
        path = factory.getResultType();
        if (Analysing.listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                String name = item.getName();
                if (getString(R.string.new_string).equals(name))
                {
                    //ignore
                } else if (getString(R.string.field_string).equals(name))
                {
                    setters.add(doAnalysisField(item, path));
                } else if (getString(R.string.property_string).equals(name))
                {
                    setters.add(doAnalysisProperty(item, path));
                } else
                {
                    throw Analysing.fromMessageThrow(item,
                            "error token");
                }
            }
        }
        return new DependencyProvider(isSingleton(element)
                ? DependencyType.Singleton
                : DependencyType.Factory,
                factory,
                setters);
    }

    private Setter doAnalysisField(Element element, Class<?> path)
    {
        String name = Analysing.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        String refOrLet = getRefOrLetAttr(element);
        try
        {
            return ReflectionUtil.newSetter(
                    ReflectionUtil.findSupportField(
                            path,
                            name,
                            getProviderResultTypeIfNullThrow(refOrLet),
                            ReflectionUtil.SUPPORT_FIELD_FILTER), refOrLet);
        } catch (NoSuchFieldException | IllegalFormatTextException e)
        {
            throw Analysing.formExceptionThrow(element, e);
        }
    }

    private Setter doAnalysisProperty(Element element, Class<?> path)
    {
        String name = Analysing.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        String refOrLet = getRefOrLetAttr(element);
        try
        {
            return ReflectionUtil.newSetter(
                    ReflectionUtil.findSupportMethod(path, "set"
                                    + Character.toUpperCase(name.charAt(0))
                                    + name.substring(1, name.length()),
                            new Class<?>[]{getProviderResultTypeIfNullThrow(refOrLet)},
                            ReflectionUtil.SUPPORT_PROPERTY_FILTER), refOrLet);
        } catch (NoSuchMethodException | IllegalFormatTextException e)
        {
            throw Analysing.formExceptionThrow(element, e);
        }
    }

    private Factory doSearchNew(Element element, Class<?> path)
    {
        List<Element> elements = element.elements();
        Factory factory = null;
        if (Analysing.listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                if (getString(R.string.new_string).equals(item.getName()))
                {
                    if (factory == null)
                    {
                        factory = doAnalysisNew(item, path);
                    } else
                    {
                        throw Analysing.fromMessageThrow(item,
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
            throw Analysing.formExceptionThrow(element, e);
        }
    }

    private Factory doAnalysisNew(Element element, Class<?> path)
    {
        List<Element> elements = element.elements();
        List<String> refOrLet = new LinkedList<>();
        if (Analysing.listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                String name = item.getName();
                if (getString(R.string.let_string).equals(name)
                        || getString(R.string.arg_string).equals(name))
                {
                    refOrLet.add(getRefOrLetAttr(item));
                } else
                {
                    throw Analysing.fromMessageThrow(item,
                            "'arg' no found");
                }
            }
        }
        Class<?>[] classes = new Class<?>[refOrLet.size()];
        for (int i = 0; i < classes.length; i++)
        {
            try
            {
                classes[i] = getProviderResultTypeIfNullThrow(refOrLet.get(i));
            } catch (IllegalFormatTextException e)
            {
                throw Analysing.formExceptionThrow(element, e);
            }
        }
        String isCustom = Analysing.getAttrNoThrow(element,
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
                                ReflectionUtil.SUPPORT_FACTORY_FILTER),
                        refOrLet);
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
            throw Analysing.formExceptionThrow(element, e);
        }
    }

    private Class<?> getProviderResultTypeIfNullThrow(String name)
            throws IllegalFormatTextException
    {
        Provider provider = providers.get(name);
        if (provider != null)
        {
            return provider.getResultType();
        } else
        {
            throw new IllegalFormatTextException(
                    "providers no has name = " + name
            );
        }
    }

    private String getRefOrLetAttr(Element element)
    {
        String ref = Analysing.getAttrNoThrow(element,
                getString(R.string.ref_string));
        String let = Analysing.getAttrNoThrow(element,
                getString(R.string.let_string));
        if (ref != null)
        {
            if (TextType.Reference.equals(
                    Analysing.getTextType(ref)))
            {
                return ref;
            }
        } else if (let != null)
        {
            Provider provider = getProvider(let);
            if (provider == null)
            {
                try
                {
                    provider = Analysing.createConstantProvider(let);
                    addProvider(let, provider);
                } catch (IllegalFormatTextException | ProviderAlreadyExistsException e)
                {
                    throw Analysing.formExceptionThrow(element, e);
                }
            }
            return let;
        }
        throw Analysing.fromMessageThrow(element,
                "ref or let illegal ref = " + ref + " let = " + let);
    }

    private boolean isSingleton(Element element)
    {
        String provider = Analysing.getAttrNoThrow(element,
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
            throw Analysing.fromMessageThrow(element,
                    "illegal provider = " + provider);
        }
    }

    private Class<?> getClassAttr(Element element)
    {
        try
        {
            return Class.forName(Analysing.getAttrIfEmptyThrow(
                    element,
                    getString(R.string.class_string)
            ));
        } catch (ClassNotFoundException e)
        {
            throw Analysing.formExceptionThrow(element, e);
        }
    }

    private void checkOwnerType(Element root)
    {
        if (getString(R.string.scope_string).equals(root.getName()))
        {
            try
            {
                if (!Class.forName(Analysing.getAttrIfEmptyThrow(root,
                        getString(R.string.owner_string)))
                        .equals(getProvider(getString(R.string.owner_string))
                                .getResultType()))
                {
                    throw Analysing.fromMessageThrow(root,
                            "owner type not match");
                }
            } catch (ClassNotFoundException e)
            {
                throw Analysing.formExceptionThrow(root, e);
            }
        }
    }
}
