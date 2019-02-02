package org.kexie.android.liteporj;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

final class DependencyAnalyzer extends ContextWrapper
{
    private final SAXReader mReader = new SAXReader();

    private final LruCache<Integer, Dependency> mResultCache;

    private int getCacheSize()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(),
                            PackageManager.GET_SERVICES
                                    & PackageManager.GET_ACTIVITIES);
            return packageInfo.activities.length
                    + packageInfo.services.length;
        } catch (Exception e)
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
        Document document = openResource(rawXml);
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
            return null;
        }
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

    private Document openResource(@RawRes int rawXml)
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
