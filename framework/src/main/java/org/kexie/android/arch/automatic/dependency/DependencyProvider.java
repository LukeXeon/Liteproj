package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DependencyProvider
{
    public interface Factory
    {
        <T> T newInstance(Dependency dependency);

        Class<?> getResultType();

        Class<?>[] getParameterTypes();
    }

    public interface Setter
    {
        void set(Object target, Dependency dependency);
    }

    private final static Class<?>[] EMPTY_CLASSES = new Class<?>[0];

    private final DependencyType type;
    private final Factory factory;
    private final List<Setter> setters;

    public DependencyProvider(@NonNull DependencyType type,
                              @NonNull Factory factory,
                              @Nullable List<Setter> setters)
    {
        this.type = type;
        this.factory = factory;
        this.setters = setters == null
                ? Collections.<Setter>emptyList()
                : Collections.unmodifiableList(setters);
    }


    @NonNull
    private static Object[] getReferences(List<String> refs,
                                          Class<?>[] targetClasses,
                                          Dependency dependency)
    {
        Object[] args = new Object[refs.size()];
        for (int i = 0; i < refs.size(); i++)
        {
            String name = refs.get(i);
            args[i] = Types.castTo(dependency.get(name), targetClasses[i]);
        }
        return args;
    }

    @NonNull
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T> T newInstance(Dependency dependency)
    {
        Object target = factory.newInstance(dependency);
        for (Setter setter : setters)
        {
            setter.set(target, dependency);
        }
        return (T) target;
    }

    @NonNull
    @SuppressWarnings("WeakerAccess")
    public DependencyType getType()
    {
        return type;
    }

    @NonNull
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T> Class<T> getResultType()
    {
        return (Class<T>) factory.getResultType();
    }

    public static Setter newSetter(final Method method, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, Dependency dependency)
            {
                try
                {
                    method.setAccessible(true);
                    method.invoke(target, dependency.get(name));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Setter newSetter(final Field field, final String name)
    {
        return new Setter()
        {
            @Override
            public void set(Object target, Dependency dependency)
            {
                field.setAccessible(true);
                try
                {
                    field.set(target, dependency.get(name));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Factory newFactory(final Method method,
                                     final List<String> references)
    {
        return new Factory()
        {
            @Override
            public <T> T newInstance(Dependency dependency)
            {
                method.setAccessible(true);
                try
                {
                    return (T) method.invoke(null,
                            getReferences(references,
                                    method.getParameterTypes(),
                                    dependency
                            ));
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Class<?> getResultType()
            {
                return method.getReturnType();
            }

            @Override
            public Class<?>[] getParameterTypes()
            {
                return method.getParameterTypes();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Factory newFactory(final Constructor<?> constructor,
                                     final List<String> references)
    {
        return new Factory()
        {
            @Override
            public <T> T newInstance(Dependency dependency)
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
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Class<?> getResultType()
            {
                return constructor.getDeclaringClass();
            }

            @Override
            public Class<?>[] getParameterTypes()
            {
                return constructor.getParameterTypes();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Factory newConstant(Object object)
    {
        final Object notNull = Objects.requireNonNull(object);
        return new Factory()
        {
            @Override
            public <T> T newInstance(Dependency dependency)
            {
                return (T) notNull;
            }

            @Override
            public Class<?> getResultType()
            {
                return notNull.getClass();
            }

            @Override
            public Class<?>[] getParameterTypes()
            {
                return EMPTY_CLASSES;
            }
        };
    }
}
