package org.kexie.android.liteproj.sample;

import android.app.Application;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.LiteViewModel;
import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;

@Using(R.raw.test_app)
public class ViewModelTest extends LiteViewModel
{
    @Reference("holder")
    private AppHolderTest test;

    public ViewModelTest(@NonNull Application application)
    {
        super(application);
        Logger.d("ViewModel test " + test);
    }
}
