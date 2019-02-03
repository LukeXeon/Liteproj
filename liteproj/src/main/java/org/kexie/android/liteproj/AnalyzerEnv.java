package org.kexie.android.liteproj;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class AnalyzerEnv
{
    private static final String TAG = "AnalyzerEnv";

    private interface TextConverter
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    enum TextType
    {
        ILLEGAL,
        CONSTANT,
        REFERENCE,
    }

    private Node mCurrent;

    private final Provider mProxyProvider;

    private final Map<String, Provider> mProviders = new ArrayMap<>();

    private final List<TextConverter> mTextConverters;

    private final Pattern mNamePattern = Pattern
            .compile("[\u4e00-\u9fa5_A-Za-z][\u4e00-\u9fa5_A-Za-z0-9]*");

    //实例包内

    Dependency makeResult()
    {
        return new Dependency(mProxyProvider.getResultType(), mProviders);
    }

    AnalyzerEnv(@NonNull final Class<?> ownerType, @NonNull Node node)
    {
        this.mProxyProvider = new Provider()
        {
            @NonNull
            @Override
            public <T> T provide(@NonNull DependencyManager dependencyManager)
            {
                throw new IllegalStateException(
                        "Objects cannot be generated " +
                                "from proxy providers");
            }

            @NonNull
            @Override
            public DependencyType getType()
            {
                return DependencyType.SINGLETON;
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return ownerType;
            }
        };
        this.mTextConverters = newTextConverters();
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

    @NonNull
    TextType getTextType(@NonNull String text)
    {
        if (TextUtils.isEmpty(text))
        {
            return TextType.ILLEGAL;
        } else
        {
            if ((text.charAt(0) == '@')
                    && !TextUtils.isEmpty(text.substring(1, text.length())))
            {
                return TextType.CONSTANT;
            } else if (mNamePattern
                    .matcher(text)
                    .matches())
            {
                return TextType.REFERENCE;
            } else
            {
                return TextType.ILLEGAL;
            }
        }
    }

    @NonNull
    Provider newConstantProvider(@NonNull String let)
    {
        String value = let.substring(1, let.length());
        if (let.charAt(0) == '@')
        {
            return new DependencyProvider(
                    DependencyType.SINGLETON,
                    DependencyProvider.newSingleton(value),
                    Collections.<Provider.Setter>emptyList()
            );
        } else
        {
            for (TextConverter valueOf : mTextConverters)
            {
                try
                {
                    return new DependencyProvider(
                            DependencyType.SINGLETON,
                            DependencyProvider.newSingleton(valueOf.valueOf(value)),
                            Collections.<Provider.Setter>emptyList()
                    );
                } catch (NumberFormatException ignored)
                {

                }
            }
            throw new IllegalStateException(
                    String.format(
                            "The name %s does not match the rule of let"
                            , let));
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
        if (!DependencyManager.OWNER.equals(name)
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
        throw fromMessageThrow(String.format("Attr %s no found", name));
    }

    @NonNull
    RuntimeException fromMessageThrow(@NonNull String massage)
    {
        return new AnalysisException(String.format(
                "Error in %s ", mCurrent.asXML())
                + (TextUtils.isEmpty(massage)
                ? ""
                : String.format(", message = %s", massage)));
    }

    @NonNull
    RuntimeException
    formExceptionThrow(@NonNull Throwable e)
    {
        return new AnalysisException(
                String.format("Error in %s\n ",
                        mCurrent.asXML()), e);
    }

    @NonNull
    private static List<TextConverter> newTextConverters()
    {
        List<TextConverter> result = new LinkedList<>();
        result.add(new TextConverter()
        {
            @NonNull
            @Override
            public Object valueOf(@NonNull String value)
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
        for (final Class<?> type : new Class<?>[]{Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class})
        {
            result.add(new TextConverter()
            {
                @NonNull
                @Override
                public Object valueOf(@NonNull String value)
                {
                    try
                    {
                        return type.getMethod("valueOf", String.class)
                                .invoke(null, value);
                    } catch (IllegalAccessException
                            | NoSuchMethodException e)
                    {
                        throw new AssertionError(e);
                    } catch (InvocationTargetException e)
                    {
                        if (e.getCause() instanceof NumberFormatException)
                        {
                            throw (NumberFormatException) e.getCause();
                        } else
                        {
                            throw new AssertionError(e);
                        }
                    }
                }
            });
        }
        return result;
    }
}