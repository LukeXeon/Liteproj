package org.kexie.android.liteproj.internal;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import org.kexie.android.liteproj.DependencyManager;
import org.kexie.android.liteproj.DependencyType;
import org.kexie.android.liteproj.util.TypeUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Provider
{
    private final static Map<String, Provider> sConstantCache
            = new WeakHashMap<>();

    static final Provider sNullProxyProvider = new Provider()
    {
        @NonNull
        @Override
        public <T> T provide(@NonNull DependencyManager dependencyManager)
        {
            throw new IllegalStateException(
                    "Objects cannot be generated " +
                            "read proxy providers");
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
            return Void.TYPE;
        }
    };

    @NonNull
    static Provider createOwnerProxyProvider(final Class<?> ownerType)
    {
        return new Provider()
        {
            @NonNull
            @Override
            public <T> T provide(@NonNull DependencyManager dependencyManager)
            {
                throw new IllegalStateException(
                        "Objects cannot be generated " +
                                "read proxy providers");
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
    }

    @NonNull
    static Provider createProvider(@NonNull final DependencyType type,
                                   @NonNull final Factory factory,
                                   @NonNull final List<Setter> setters)
    {
        return new Provider()
        {
            @NonNull
            @SuppressWarnings({"unchecked"})
            @Override
            public <T> T provide(@NonNull DependencyManager dependencyManager)
            {
                Object target = factory.newInstance(dependencyManager);
                for (Setter setter : setters)
                {
                    setter.set(target, dependencyManager);
                }
                return (T) target;
            }

            @NonNull
            @Override
            public DependencyType getType()
            {
                return type;
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return factory.getResultType();
            }
        };
    }

    @NonNull
    static Provider markConstant(@NonNull Name name)
    {
        switch (name.type)
        {
            case CONSTANT:
            {
                Provider provider = sConstantCache.get(name.text);
                if (provider == null)
                {
                    synchronized (sConstantCache)
                    {
                        provider = sConstantCache.get(name.text);
                        if (provider == null)
                        {
                            final Object object = Name.toValue(name.text);
                            provider = new Provider()
                            {
                                @NonNull
                                @SuppressWarnings({"unchecked"})
                                @Override
                                public <T> T provide(@NonNull DependencyManager dependency)
                                {
                                    return (T) object;
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
                                    return object.getClass();
                                }
                            };
                            sConstantCache.put(name.text, provider);
                        }
                    }
                }
                return provider;
            }
            default:
            {
                throw new IllegalStateException(String.format("Illegal name %s for get constant", name));
            }
        }
    }

    @NonNull
    static Setter createPropertySetter(@NonNull final Method method,
                                       @NonNull final Name name)
    {
        return new Setter()
        {
            @Override
            public void set(@NonNull Object target,
                            @NonNull DependencyManager dependency)
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(target,
                            TypeUtil.castToType(
                                    getValueByName(dependency, name),
                                    method.getParameterTypes()[0]));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NonNull
    static Setter createFieldSetter(@NonNull final Field field,
                                    @NonNull final Name name)
    {
        return new Setter()
        {
            @Override
            public void set(@NonNull Object target, @NonNull DependencyManager dependency)
            {
                field.setAccessible(true);
                try
                {
                    field.set(target,
                            TypeUtil.castToType(
                                    getValueByName(dependency, name),
                                    field.getType()));
                } catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NonNull
    static Factory createMethodFactory(@NonNull final Method method,
                                       @NonNull final List<Name> references)
    {
        return new Factory()
        {
            @NonNull
            @SuppressWarnings("unchecked")
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependency)
            {
                method.setAccessible(true);
                try
                {
                    return (T) method.invoke(null,
                            getReferences(references,
                                    method.getParameterTypes(),
                                    dependency
                            )
                    );
                } catch (IllegalAccessException
                        | InvocationTargetException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return method.getReturnType();
            }
        };
    }

    @NonNull
    static Factory createBuilderFactory(@NonNull final Class<?> builderType,
                                        @NonNull final Map<Method, Name> references,
                                        @NonNull final Method build)
    {
        return new Factory()
        {
            @NonNull
            @SuppressWarnings("unchecked")
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependency)
            {
                try
                {
                    Object builder = builderType.newInstance();
                    for (Map.Entry<Method, Name> entry
                            : references.entrySet())
                    {
                        Method setter = entry.getKey();
                        setter.invoke(builder, TypeUtil.castToType(
                                getValueByName(dependency, entry.getValue()),
                                setter.getParameterTypes()[0]));
                    }
                    return (T) build.invoke(builder);
                } catch (InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return build.getReturnType();
            }
        };
    }

    @NonNull
    static Factory createConstructorFactory(@NonNull final Constructor<?> constructor,
                                            @NonNull final List<Name> references)
    {
        return new Factory()
        {
            @SuppressWarnings("unchecked")
            @NonNull
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependency)
            {
                constructor.setAccessible(true);
                try
                {
                    return (T) constructor.newInstance(
                            getReferences(
                                    references,
                                    constructor.getParameterTypes(),
                                    dependency
                            )
                    );
                } catch (IllegalAccessException
                        | InvocationTargetException
                        | InstantiationException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return constructor.getDeclaringClass();
            }

        };
    }

    @NonNull
    private static Object[] getReferences(@NonNull List<Name> refs,
                                          @NonNull Class<?>[] targetClasses,
                                          @NonNull DependencyManager dependency)
    {
        Object[] args = new Object[refs.size()];
        for (int i = 0; i < refs.size(); i++)
        {
            args[i] = TypeUtil.castToType(
                    getValueByName(dependency,
                            refs.get(i)),
                    targetClasses[i]);
        }
        return args;
    }

    @NonNull
    private static Object getValueByName(@NonNull DependencyManager dependency,
                                         @NonNull Name name)
    {
        switch (name.type)
        {
            case CONSTANT:
            {
                return markConstant(name).provide(dependency);
            }
            case REFERENCE:
            {
                return Objects.requireNonNull(dependency.get(name.text),
                        "Can't provide a null text");
            }
            default:
            {
                throw new AssertionError();
            }
        }
    }

    @NonNull
    public abstract <T> T provide(@NonNull DependencyManager dependencyManager);

    @NonNull
    public abstract DependencyType getType();

    @NonNull
    public abstract Class<?> getResultType();
}
