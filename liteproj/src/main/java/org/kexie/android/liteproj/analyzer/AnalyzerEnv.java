package org.kexie.android.liteproj.analyzer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;
import org.kexie.android.liteproj.DependencyManager;
import org.kexie.android.liteproj.GenerateDependencyException;

import java.util.Map;

final class AnalyzerEnv
{
    private Node mCurrent;

    private final Provider mProxyProvider;

    private final Map<String, Provider> mProviders = new ArrayMap<>();

    //实例包内

    Dependency makeResult()
    {
        return new Dependency(mProxyProvider.getResultType(), mProviders);
    }

    AnalyzerEnv(@NonNull Class<?> ownerType, @NonNull Node node)
    {
        this.mProxyProvider = ProviderImpl.createProxyProvider(ownerType);
        mark(node);
    }

    void mark(@NonNull Node currentNode)
    {
        this.mCurrent = currentNode;
    }

    @NonNull
    Class<?> getResultTypeIfNullThrow(@NonNull String name)
    {
        Provider provider = getProvider(name);
        if (provider != null)
        {
            return provider.getResultType();
        } else
        {
            throw fromMessageThrow(String.format("no found name by %s provider", name));
        }
    }

    @Nullable
    Provider getProvider(@NonNull String name)
    {
        if (DependencyManager.OWNER.equals(name))
        {
            return mProxyProvider;
        }
        return mProviders.get(name);
    }


    void addProvider(@NonNull String name, @NonNull Provider provider)
    {
        if (!DependencyManager.NULL.equals(name)
                && !DependencyManager.OWNER.equals(name)
                && !mProviders.containsKey(name))
        {
            mProviders.put(name, provider);
        } else
        {
            throw fromMessageThrow(
                    String.format("The provider named %s already exists",
                            name)
            );
        }
    }

    @Nullable
    String getAttrNoThrow(@NonNull Element element,
                          @NonNull String attr)
    {
        mark(element);
        if (element.attributeCount() != 0)
        {
            Attribute attribute = element.attribute(attr);
            if (attribute != null)
            {
                return attribute.getValue();
            }
        }
        return null;
    }

    @NonNull
    String getAttrIfEmptyThrow(@NonNull Element element,
                               @NonNull String attr)
    {
        mark(element);
        String name = getAttrNoThrow(element, attr);
        if (!TextUtils.isEmpty(name))
        {
            return name;
        }
        throw fromMessageThrow(String.format("Attr %s no found", attr));
    }

    @NonNull
    RuntimeException fromMessageThrow(@NonNull String massage)
    {
        return new GenerateDependencyException(String.format(
                "Error in %s ", mCurrent.asXML())
                + (TextUtils.isEmpty(massage)
                ? ""
                : String.format(", message = %s", massage)));
    }

    @NonNull
    RuntimeException
    formExceptionThrow(@NonNull Throwable e)
    {
        return new GenerateDependencyException(
                String.format("Error in %s\n ",
                        mCurrent.asXML()), e);
    }
}