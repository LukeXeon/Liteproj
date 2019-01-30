package org.kexie.android.arch.automatic.dependency;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

public final class ACAppDependency
        extends DependencyWrapper
{
    private final Class<? extends Application> requireType;

    public ACAppDependency(Dependency base,
                           Class<? extends Application>
                                               requireType)
    {
        super(base);
        this.requireType = requireType;
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T getOwner()
    {
        AppCompatActivity activity = super.getOwner();
        return (T) activity.getApplicationContext();
    }

    @NonNull
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T get(String name)
    {
        if (OWNER.equals(name))
        {
            return getOwner();
        }
        return super.get(name);
    }

    @NonNull
    @Override
    public Class<?> getResultType(String name)
    {
        if (OWNER.equals(name))
        {
            return requireType;
        }
        return super.getResultType(name);
    }
}
