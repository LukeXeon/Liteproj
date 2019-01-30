package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;

public interface Dependency
{
    String FIELD = "field";

    String PROPERTY = "property";

    String NEW = "new";

    String VAR = "var";

    String ARG = "arg";

    String REF = "ref";

    String CLASS = "class";

    String NAME = "name";

    String INCLUDE = "include";

    String SINGLETON = "singleton";

    String FACTORY = "factory";

    String SCOPE = "scope";

    String RAW_RES = "@raw/";

    String LET = "let";

    String OWNER = "owner";

    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    <T> T get(String name);

    @NonNull
    Class<?> getResultType(String name);

    @NonNull
    DependencyType getDependencyType(String name);

}
