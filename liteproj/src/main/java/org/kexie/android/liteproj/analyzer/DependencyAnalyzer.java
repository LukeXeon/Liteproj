package org.kexie.android.liteproj.analyzer;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.kexie.android.liteproj.DependencyType;
import org.kexie.android.liteproj.R;
import org.kexie.android.liteproj.util.TextType;
import org.kexie.android.liteproj.util.TextUtil;
import org.kexie.android.liteproj.util.TypeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class DependencyAnalyzer
        extends ContextWrapper
{
    private interface TextConverter
    {
        @NonNull
        Object valueOf(@NonNull String value);
    }

    private static final String TAG = "DependencyAnalyzer";

    private static final List<TextConverter> sTextConverters = newTextConverters();

    private final SAXReader mSAXReader = new SAXReader();

    private final LruCache<Object, Dependency> mResultCache;

    @NonNull
    private Document readXml(@NonNull InputStream stream,
                             boolean isCompressed)
    {
        if (!isCompressed)
        {
            try
            {
                return mSAXReader.read(stream);
            } catch (DocumentException e)
            {
                throw new RuntimeException(e);
            }
        } else
        {
            try
            {
                return new AXmlReader().read(stream);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean listNoEmpty(@Nullable List<?> list)
    {
        return list != null && list.size() != 0;
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

    @NonNull
    private static Object getVal(@NonNull String val)
    {
        String value = val.substring(1, val.length());
        if (val.charAt(0) == '@')
        {
            return value;
        } else
        {
            for (TextConverter valueOf : sTextConverters)
            {
                try
                {
                    return valueOf.valueOf(value);
                } catch (NumberFormatException ignored)
                {

                }
            }
            throw new NumberFormatException(
                    String.format(
                            "The name %s does not match the rule of let"
                            , val));
        }
    }

    private int getCacheSize()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(),
                            PackageManager.GET_SERVICES
                                    | PackageManager.GET_ACTIVITIES);
            int size = ((packageInfo.activities == null
                    || packageInfo.activities.length == 0 ? 1
                    : packageInfo.activities.length)
                    * Runtime.getRuntime().availableProcessors()
                    + (packageInfo.services == null
                    || packageInfo.services.length == 0 ? 0
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
    public Dependency analysis(@RawRes int xml)
    {
        Dependency dependency = mResultCache.get(xml);
        if (dependency == null)
        {
            switch (getResources().getResourceTypeName(xml))
            {
                case "raw":
                {
                    dependency = analysisDocument(
                            readXml(getResources().openRawResource(xml),
                                    false));
                }
                break;
                case "xml":
                {
                    dependency = analysisDocument(
                            readXml(getResources().openRawResource(xml),
                                    true));
                }
                break;
                default:
                {
                    throw new IllegalStateException("Files can be in the 'raw' directory " +
                            "or the 'xml' directory");
                }
            }
            mResultCache.put(xml, dependency);
        }
        return dependency;
    }

    @NonNull
    public Dependency analysis(@NonNull String asset)
    {
        Dependency dependency = mResultCache.get(asset);
        if (dependency == null)
        {
            try (InputStream stream = getAssets().open(asset))
            {
                dependency = analysisDocument(
                        readXml(stream, false)
                );
                mResultCache.put(asset, dependency);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return dependency;
    }

    @NonNull
    private Dependency analysisDocument(Document document)
    {
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
        String let = env.getAttrNoThrow(element, getString(R.string.val_string));
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
        Class<?> type = getTypeAttrIfErrorThrow(env, element);
        List<Element> elements = element.elements();
        List<Provider.Setter> setters = new LinkedList<>();
        Provider.Factory factory = searchFactory(env, element, type);
        if (listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(item);
                String name = item.getName();
                if (getString(R.string.new_string).equals(name)
                        || getString(R.string.factory_string).equals(name)
                        || getString(R.string.builder_string).equals(name))
                {
                    continue;
                }
                if (getString(R.string.field_string).equals(name))
                {
                    setters.add(analysisField(env, item, type));
                } else if (getString(R.string.property_string).equals(name))
                {
                    setters.add(analysisProperty(env, item, type));
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
        env.mark(element);
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

    private Provider.Setter analysisField(AnalyzerEnv env, Element element, Class<?> path)
    {
        env.mark(element);
        String name = env.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        String refOrVal = getRefOrValAttr(env, element);
        return DependencyProvider.newSetter(
                TypeUtil.getTypeField(path,
                        name,
                        env.getResultTypeIfNullThrow(refOrVal)),
                refOrVal);
    }

    private Provider.Factory searchFactory(AnalyzerEnv env,
                                           Element element,
                                           Class<?> type)
    {
        env.mark(element);
        List<Element> elements = element.elements();
        for (Element item : elements)
        {
            env.mark(item);
            if (getString(R.string.new_string).equals(item.getName()))
            {
                return analysisNew(env, item, type);
            } else if (getString(R.string.factory_string).equals(item.getName()))
            {
                return analysisFactory(env, item, type);
            } else if (getString(R.string.builder_string).equals(item.getName()))
            {
                return analysisBuilder(env, item, type);
            }
        }
        return DependencyProvider.newNew(
                TypeUtil.getTypeConstructor(type, null),
                Collections.<String>emptyList());
    }

    private Provider.Factory analysisNew(AnalyzerEnv env,
                                         Element element,
                                         Class<?> type)
    {
        env.mark(element);
        List<Element> elements = element.elements();
        List<String> refOrVal = new LinkedList<>();
        if (listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(item);
                String name = item.getName();
                if (getString(R.string.arg_string).equals(name))
                {
                    String temp = getRefOrValAttr(env, item);
                    if (!refOrVal.contains(temp))
                    {
                        refOrVal.add(temp);
                    }
                    else
                    {
                        throw env.fromMessageThrow(
                                String.format("The name '%s' already exist", temp)
                        );
                    }
                } else
                {
                    throw env.fromMessageThrow("Tag 'arg' no found");
                }
            }
        }
        Class<?>[] classes = new Class<?>[refOrVal.size()];
        for (int i = 0; i < classes.length; i++)
        {
            classes[i] = env.getResultTypeIfNullThrow(refOrVal.get(i));
        }
        return DependencyProvider.newNew(
                TypeUtil.getTypeConstructor(type,
                        classes)
                , refOrVal);
    }

    private Provider.Factory analysisFactory(AnalyzerEnv env,
                                             Element element,
                                             Class<?> type)
    {
        env.mark(element);
        Class<?> factoryType = getTypeAttrIfErrorThrow(env, element);
        String factoryName = env.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        List<Element> elements = element.elements();
        List<String> refOrVal = new LinkedList<>();
        if (listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(item);
                String name = item.getName();
                if (getString(R.string.arg_string).equals(name))
                {
                    String temp = getRefOrValAttr(env, item);
                    if (!refOrVal.contains(temp))
                    {
                        refOrVal.add(temp);
                    } else
                    {
                        throw env.fromMessageThrow(
                                String.format("The name '%s' already exist", temp)
                        );
                    }
                } else
                {
                    throw env.fromMessageThrow("Tag 'arg' no found");
                }
            }
        }
        Class<?>[] classes = new Class<?>[refOrVal.size()];
        for (int i = 0; i < classes.length; i++)
        {
            classes[i] = env.getResultTypeIfNullThrow(refOrVal.get(i));
        }
        Method factoryMethod = TypeUtil.getTypeFactory(factoryType,
                factoryName,
                classes);
        if (TypeUtil.isAssignToType(factoryMethod.getReturnType(), type))
        {
            return DependencyProvider.newFactory(factoryMethod,
                    refOrVal);
        } else
        {
            throw env.fromMessageThrow(
                    String.format("Return type no match (form %s to %s)",
                            factoryMethod.getReturnType(),
                            type));
        }
    }

    private Provider.Factory analysisBuilder(AnalyzerEnv env,
                                             Element element,
                                             Class<?> type)
    {
        Class<?> builderType = getTypeAttrIfErrorThrow(env, element);
        List<Element> elements = element.elements();
        Map<String, String> refOrVal = new ArrayMap<>();
        if (listNoEmpty(elements))
        {
            for (Element item : elements)
            {
                env.mark(item);
                String name = item.getName();
                if (getString(R.string.arg_string).equals(name))
                {
                    String argName = env.getAttrIfEmptyThrow(element,
                            getString(R.string.name_string));
                    if (!refOrVal.containsKey(argName))
                    {
                        refOrVal.put(argName, getRefOrValAttr(env, item));
                    } else
                    {
                        throw env.fromMessageThrow(
                                String.format("The name '%s' already exist", argName)
                        );
                    }
                } else
                {
                    throw env.fromMessageThrow("Tag 'arg' no found");
                }
            }
        }
        Map<Method, String> setters = new ArrayMap<>();
        Method buildMethod = TypeUtil.getTypeInstanceMethod(builderType,
                "build",
                null);
        if (!TypeUtil.isAssignToType(buildMethod.getReturnType(), type))
        {
            throw env.fromMessageThrow(
                    String.format("Return type no match (form %s to %s)",
                            buildMethod.getReturnType(),
                            type));
        }
        for (Map.Entry<String, String> entry : refOrVal.entrySet())
        {
            setters.put(TypeUtil.getTypeInstanceMethod(
                    builderType,
                    entry.getKey(),
                    new Class<?>[]{env.getResultTypeIfNullThrow(entry.getValue())}),
                    entry.getValue());
        }
        return DependencyProvider.newBuilder(builderType,
                setters,
                buildMethod);
    }

    private Provider.Setter analysisProperty(AnalyzerEnv env,
                                             Element element,
                                             Class<?> path)
    {
        env.mark(element);
        String name = env.getAttrIfEmptyThrow(element,
                getString(R.string.name_string));
        String refOrVal = getRefOrValAttr(env, element);
        return DependencyProvider.newSetter(
                TypeUtil.getTypeProperty(
                        path,
                        name,
                        env.getResultTypeIfNullThrow(refOrVal)),
                refOrVal);
    }

    private String getRefOrValAttr(AnalyzerEnv env, Element element)
    {
        env.mark(element);
        String ref = env.getAttrNoThrow(element,
                getString(R.string.ref_string));
        String let = env.getAttrNoThrow(element,
                getString(R.string.val_string));
        if (ref != null)
        {
            if (TextType.REFERENCE.equals(
                    TextUtil.getTextType(ref)))
            {
                return ref;
            }
        } else if (let != null)
        {
            Provider provider = env.getProvider(let);
            if (provider == null)
            {
                provider = new DependencyProvider(
                        DependencyType.SINGLETON,
                        DependencyProvider.newSingleton(
                                getVal(let)),
                        Collections.<Provider.Setter>emptyList());
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
                getString(R.string.val_string));
        if (TextType.CONSTANT.equals(TextUtil.getTextType(let)))
        {
            Provider provider = env.getProvider(let);
            if (provider == null)
            {
                provider = new DependencyProvider(
                        DependencyType.SINGLETON,
                        DependencyProvider.newSingleton(
                                getVal(let)),
                        Collections.<Provider.Setter>emptyList());
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
        if (getString(R.string.dependency_string).equals(root.getName()))
        {
            Attribute attribute = root.attribute(getString(R.string.owner_string));
            if (attribute != null && !TextUtils.isEmpty(attribute.getName()))
            {
                try
                {
                    return Class.forName(attribute.getValue());
                } catch (ClassNotFoundException e)
                {
                    throw new AnalysisException(e);
                }
            }

        }
        throw new AnalysisException("XML file format error in " + root.asXML());
    }

    private Class<?> getTypeAttrIfErrorThrow(AnalyzerEnv env, Element element)
    {
        try
        {
            return Class.forName(env.getAttrIfEmptyThrow(
                    element,
                    getString(R.string.type_string)
            ));
        } catch (ClassNotFoundException e)
        {
            throw env.formExceptionThrow(e);
        }
    }
}