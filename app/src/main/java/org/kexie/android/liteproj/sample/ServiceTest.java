package org.kexie.android.liteproj.sample;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.LiteService;
import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;


@Using(R.raw.test_app)
public class ServiceTest extends LiteService
{
    @Reference("holder")
    AppHolderTest test;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Logger.d("Service " + test);
    }
}
