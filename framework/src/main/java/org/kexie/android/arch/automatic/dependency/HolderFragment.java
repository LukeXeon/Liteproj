package org.kexie.android.arch.automatic.dependency;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

public final class HolderFragment extends Fragment
{
    private static HolderFragment newInstance()
    {
        return new HolderFragment();
    }

    private Dependency dependency;

    public Dependency getDependency()
    {
        return dependency;
    }

    public static Dependency getDependency(Object object)
    {
        if (object instanceof AppCompatActivity)
        {
            return getByFragment(((AppCompatActivity) object)
                    .getSupportFragmentManager());
        } else if (object instanceof Fragment)
        {
            return getByFragment(((Fragment) object)
                    .getChildFragmentManager());
        } else
        {
            throw new AssertionError();
        }
    }

    private static Dependency getByFragment(FragmentManager fragmentManager)
    {
        HolderFragment holder = (HolderFragment) fragmentManager
                .findFragmentByTag(
                        HolderFragment.class.getCanonicalName()
                );
        if (holder == null)
        {
            holder = newInstance();
            FragmentTransaction transaction
                    = fragmentManager.beginTransaction();
            transaction.add(holder,
                    HolderFragment.class.getCanonicalName());
            transaction.commit();
        }
        return holder.getDependency();
    }

    private Object getOwner()
    {
        Object owner = getParentFragment();
        if (owner == null)
        {
            owner = getActivity();
        }
        if (owner == null)
        {
            throw new AssertionError();
        }
        return owner;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        dependency = DependencyAnalyzer.analysis(getOwner(), context);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        dependency = null;
    }
}
