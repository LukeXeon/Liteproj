package org.kexie.android.liteproj.analyzer;

import android.support.annotation.NonNull;

import org.kexie.android.liteproj.DependencyManager;
import org.kexie.android.liteproj.DependencyType;
import org.kexie.android.liteproj.util.TypeUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

final class DependencyProvider implements Provider
{
    private final DependencyType mType;
    private final Factory mFactory;
    private final List<Setter> mSetters;

    DependencyProvider(@NonNull DependencyType type,
                       @NonNull Factory factory,
                       @NonNull List<Setter> setters)
    {
        this.mType = type;
        this.mFactory = factory;
        this.mSetters = setters;
    }

    @NonNull
    static Setter newSetter(@NonNull final Method method, @NonNull final String name)
    {
        return new Setter()
        {
            @Override
            public void set(@NonNull Object target, @NonNull DependencyManager dependency)
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(target, TypeUtil.castToType(dependency.get(name),
                            method.getParameterTypes()[0]));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NonNull
    static Setter newSetter(@NonNull final Field field, @NonNull final String name)
    {
        return new Setter()
        {
            @Override
            public void set(@NonNull Object target, @NonNull DependencyManager dependency)
            {
                field.setAccessible(true);
                try
                {
                    field.set(target, TypeUtil.castToType(dependency.get(name),
                            field.getType()));
                } catch (IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @NonNull
    @SuppressWarnings("unchecked")
    static Factory newFactory(@NonNull final Method method,
                              @NonNull final List<String> references)
    {
        return new Factory()
        {
            @NonNull
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
    static Factory newFactory(@NonNull final Constructor<?> constructor,
                              @NonNull final List<String> references)
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
    private static Object[] getReferences(@NonNull List<String> refs,
                                          @NonNull Class<?>[] targetClasses,
                                          @NonNull DependencyManager dependency)
    {
        Object[] args = new Object[refs.size()];
        for (int i = 0; i < refs.size(); i++)
        {
            String name = refs.get(i);
            args[i] = TypeUtil.castToType(dependency.get(name), targetClasses[i]);
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    static Factory newSingleton(Object object)
    {
        final Object nonNull = Objects.requireNonNull(object);
        return new Factory()
        {
            @NonNull
            @Override
            public <T> T newInstance(@NonNull DependencyManager dependencyManager)
            {
                return (T) nonNull;
            }

            @NonNull
            @Override
            public Class<?> getResultType()
            {
                return nonNull.getClass();
            }
        };
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T provide(@NonNull DependencyManager dependencyManager)
    {
        Object target = mFactory.newInstance(dependencyManager);
        for (Setter setter : mSetters)
        {
            setter.set(target, dependencyManager);
        }
        return (T) target;
    }

    @Override
    @NonNull
    public DependencyType getType()
    {
        return mType;
    }

    @Override
    @NonNull
    public Class<?> getResultType()
    {
        return mFactory.getResultType();
    }
}
