package org.kexie.android.arch.automatic.dependency;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import java.util.Objects;

public final class FragmentToActivityCompat
    extends DependencyWrapper
{
    private final Class<? extends AppCompatActivity> requireType;

    public FragmentToActivityCompat(Dependency base,
                                    Class<? extends AppCompatActivity>
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
        Fragment fragment = super.getOwner();
        return (T) Objects.requireNonNull(
                AppCompatActivity.class.cast(
                        fragment.getActivity()
                )
        ).getApplicationContext();
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
