package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

public abstract class Dependency
{
    public final static String FIELD = "field";

    public final static String PROPERTY = "property";

    public final static String NEW = "new";

    public final static String VAR = "var";

    public final static String ARG = "arg";

    public final static String REF = "ref";

    public final static String CLASS = "class";

    public final static String NAME = "name";

    public final static String INCLUDE = "include";

    public final static String SINGLETON = "singleton";

    public final static String FACTORY = "factory";

    public final static String SCOPE = "scope";

    public final static String RAW_RES = "@raw/";

    public final static String LET = "let";

    public final static String OWNER = "owner";

    @NonNull
    public final <T> T get(String name)
    {
        return onGet(name, this);
    }

    @NonNull
    public final <T> Class<T> getOwnerType()
    {
        return getResultType(OWNER);
    }

    @NonNull
    protected abstract <T> T onGet(String name, Dependency dependency);

    @NonNull
    public abstract <T> Class<T> getResultType(String name);

    @NonNull
    public abstract DependencyType getDependencyType(String name);

    public abstract void clear();
}
