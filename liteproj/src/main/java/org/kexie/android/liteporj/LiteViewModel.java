package org.kexie.android.liteporj;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

public class LiteViewModel
        extends AndroidViewModel
        implements LifecycleOwner
{
    private final LifecycleRegistry mRegistry;

    public LiteViewModel(@NonNull Application application)
    {
        super(application);
        mRegistry = new LifecycleRegistry(this);
        mRegistry.addObserver(LiteprojInitializer.sLifecycleHandler);
        mRegistry.markState(Lifecycle.State.CREATED);
    }

    @CallSuper
    @Override
    protected void onCleared()
    {
        super.onCleared();
        mRegistry.markState(Lifecycle.State.DESTROYED);
        mRegistry.removeObserver(LiteprojInitializer.sLifecycleHandler);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle()
    {
        return mRegistry;
    }
}
