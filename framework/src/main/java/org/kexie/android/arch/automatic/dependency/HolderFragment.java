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

    public HolderFragment()
    {
        setRetainInstance(true);
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

    static void prepareInject(FragmentManager fragmentManager)
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
                    HolderFragment.class.getCanonicalName())
                    .commitNowAllowingStateLoss();
        } else
        {
            throw new IllegalStateException("holder fragment already exists");
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
            throw new IllegalStateException("no prepare");
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
        Object owner = getOwner();
        dependency = DependencyAnalyzer.analysis(owner, context);
        if (dependency != null)
        {
            DependenciesManager.inject(owner, dependency);
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        dependency = null;
    }
}
