package org.kexie.android.liteproj;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final class DependencyAnalyzer extends ContextWrapper
{
    private final SAXReader mReader = new SAXReader();

    private static final String TAG = "DependencyAnalyzer";

    private final LruCache<Integer, Dependency> mResultCache;

    private int getCacheSize()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(),
                            PackageManager.GET_SERVICES
                                    | PackageManager.GET_ACTIVITIES);
            int size = ((packageInfo.activities == null
                    || packageInfo.activities.length == 0
                    ? 1
                    : packageInfo.activities.length)
                    * Runtime.getRuntime().availableProcessors()
                    + (packageInfo.services == null
                    || packageInfo.services.length == 0
                    ? 1
                    : packageInfo.services.length));
            Log.i(TAG, String.format("cache size = %d", size));
            return size;
        } catch (PackageManager.NameNotFoundException e)
        {
            throw new AssertionError(e);
        }
    }

    public DependencyAnalyzer(Context base)
    {
        super(base.getApplicationContext());
        mResultCache = new LruCache<>(getCacheSize());
    }

    @NonNull
    public Dependency analysis(@RawRes int rawXml)
    {
        Dependency dependency = mResultCache.get(rawXml);
        if (dependency == null)
        {
            dependency = analysisInternal(rawXml);
            mResultCache.put(rawXml, dependency);
        }
        return dependency;
    }

    @NonNull
    private Dependency analysisInternal(int rawXml)
    {
        Document document = getDocument(rawXml);
        Element scope = document.getRootElement();
        AnalyzerEnv env = new AnalyzerEnv(getOwnerType(scope), scope);
        List<Element> elements = scope.elements();
        for (Element element : elements)
        {
            env.mark(element);
            if (getString(R.string.var_string).equals(element.getName()))
            {
                env.addProvider(env.getAttrIfEmptyThrow(element,
                        getString(R.string.name_string)),
                        analysisVar(env, element));
            } else
            {
                throw env.fromMessageThrow("Need a 'var' tag");
            }
        }
        return env.makeResult();
    }

    private Provider analysisVar(AnalyzerEnv env, Element element)
    {
        env.mark(element);
        String let = env.getAttrNoThrow(element, getString(R.string.let_string));
        if (let != null)
        {
            return analysisLetAssignToVar(env, element);
        } else
        {
            return analysisProviderVar(env, element);
        }
    }

    private Provider analysisProviderVar(AnalyzerEnv env, Element element)
    {
        env.mark(element);
        Class<?> path = getClassAttr(env, element);
        List<Element> elements = element.elements();
        List<Setter> setters = new LinkedList<>();
        Factory factory = doSearchNew(env, element, path);
        path = factory.getResultType();
        if (AnalyzerEnv.listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(item);
                String name = item.getName();
                if (getString(R.string.new_string).equals(name))
                {
                    continue;
                }
                if (getString(R.string.field_string).equals(name))
                {
                    setters.add(analysisField(env, item, path));
                } else if (getString(R.string.property_string).equals(name))
                {
                    setters.add(analysisProperty(env, item, path));
                } else
                {
                    throw env.fromMessageThrow(
                            String.format("Error token %s",
                                    item.getName()));
                }
            }
        }
        return new DependencyProvider(isSingleton(env, element)
                ? DependencyType.SINGLETON
                : DependencyType.FACTORY,
                factory,
                setters);
    }

    private boolean isSingleton(AnalyzerEnv env, Element element)
    {
        String provider = env.getAttrNoThrow(element,
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
            throw env.fromMessageThrow(String.format(
                    "Illegal provider = %s",
                    provider));
        }
    }

    private Setter analysisField(AnalyzerEnv env, Element element, Class<?> path)
    {
        String name = env.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        String refOrLet = getRefOrLetAttr(env, element);
        return AnalyzerEnv.newSetter(
                env.findField(path,
                        name,
                        env.getResultTypeIfNullThrow(refOrLet)),
                refOrLet);
    }


    private Factory doSearchNew(AnalyzerEnv env, Element element, Class<?> path)
    {
        env.mark(element);
        List<Element> elements = element.elements();
        Factory factory = null;
        if (AnalyzerEnv.listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(item);
                if (getString(R.string.new_string).equals(item.getName()))
                {
                    if (factory == null)
                    {
                        factory = analysisNew(env, item, path);
                    } else
                    {
                        throw env.fromMessageThrow("The 'new' tag must only one");
                    }
                }
            }
        }
        return factory == null
                ? AnalyzerEnv.newFactory(
                env.findConstructor(path,
                        null),
                Collections.<String>emptyList())
                : factory;
    }

    private Setter analysisProperty(AnalyzerEnv env, Element element, Class<?> path)
    {
        String name = env.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        String refOrLet = getRefOrLetAttr(env, element);
        return AnalyzerEnv.newSetter(
                env.findProperty(
                        path,
                        name,
                        env.getResultTypeIfNullThrow(refOrLet)),
                refOrLet);
    }

    private Factory analysisNew(AnalyzerEnv env, Element element, Class<?> path)
    {
        env.mark(element);
        List<Element> elements = element.elements();
        List<String> refOrLet = new LinkedList<>();
        if (AnalyzerEnv.listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(element);
                String name = item.getName();
                if (getString(R.string.let_string).equals(name)
                        || getString(R.string.arg_string).equals(name))
                {
                    refOrLet.add(getRefOrLetAttr(env, item));
                } else
                {
                    throw env.fromMessageThrow("Tag 'arg' no found");
                }
            }
        }
        Class<?>[] classes = new Class<?>[refOrLet.size()];
        for (int i = 0; i < classes.length; i++)
        {
            classes[i] = env.getResultTypeIfNullThrow(refOrLet.get(i));
        }
        String isCustom = env.getAttrNoThrow(element,
                getString(R.string.name_string));
        isCustom = isCustom != null
                && !getString(R.string.new_normal_string).equals(isCustom)
                ? isCustom : null;
        if (isCustom != null)
        {
            return AnalyzerEnv.newFactory(
                    env.findFactory(
                            path,
                            isCustom,
                            classes),
                    refOrLet);
        } else
        {
            return AnalyzerEnv.newFactory(
                    env.findConstructor(path,
                            classes)
                    , refOrLet);
        }
    }

    private String getRefOrLetAttr(AnalyzerEnv env, Element element)
    {
        String ref = env.getAttrNoThrow(element,
                getString(R.string.ref_string));
        String let = env.getAttrNoThrow(element,
                getString(R.string.let_string));
        if (ref != null)
        {
            if (TextType.REFERENCE.equals(
                    env.getTextType(ref)))
            {
                return ref;
            }
        } else if (let != null)
        {
            Provider provider = env.getProvider(let);
            if (provider == null)
            {
                provider = env.newConstantProvider(let);
                env.addProvider(let, provider);
            }
            return let;
        }
        throw env.fromMessageThrow(String.format(
                "Illegal %s = %s",
                !TextUtils.isEmpty(ref) ? "ref" : "let",
                !TextUtils.isEmpty(ref) ? ref : let));
    }

    private Provider analysisLetAssignToVar(AnalyzerEnv env, Element element)
    {
        env.mark(element);
        String let = env.getAttrIfEmptyThrow(element,
                getString(R.string.let_string));
        if (TextType.CONSTANT.equals(env.getTextType(let)))
        {
            Provider provider = env.getProvider(let);
            if (provider == null)
            {
                provider = env.newConstantProvider(let);
                env.addProvider(let, provider);
            }
            return provider;
        } else
        {
            throw env.fromMessageThrow(String.format("Incorrect name '%s'", let));
        }
    }

    private Class<?> getOwnerType(Element root)
    {
        if (getString(R.string.scope_string).equals(root.getName()))
        {
            Attribute attribute = root.attribute(getString(R.string.owner_string));
            if (attribute != null && !TextUtils.isEmpty(attribute.getName()))
            {
                try
                {
                    return Class.forName(attribute.getValue());
                } catch (ClassNotFoundException e)
                {
                    throw new GenerateDepartmentException(e);
                }
            }

        }
        throw new GenerateDepartmentException("XML file format error in " + root.asXML());
    }

    private Class<?> getClassAttr(AnalyzerEnv env, Element element)
    {
        try
        {
            return Class.forName(env.getAttrIfEmptyThrow(
                    element,
                    getString(R.string.class_string)
            ));
        } catch (ClassNotFoundException e)
        {
            throw env.formExceptionThrow(e);
        }
    }

    private Document getDocument(@RawRes int rawXml)
    {
        try (InputStream stream = getResources().openRawResource(rawXml))
        {
            return mReader.read(stream);
        } catch (IOException | DocumentException e)
        {
            throw new RuntimeException(e);
        }
    }
}