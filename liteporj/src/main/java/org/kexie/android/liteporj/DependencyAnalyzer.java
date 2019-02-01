package org.kexie.android.liteporj;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.util.LruCache;

import org.dom4j.io.SAXReader;

final class DependencyAnalyzer extends ContextWrapper
{
    private final SAXReader mReader = new SAXReader();

    private final LruCache<Integer, Dependency> mResultCache;

    public DependencyAnalyzer(Context base)
    {
        super(base.getApplicationContext());
        try
        {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(),
                    PackageManager.GET_SERVICES
                            & PackageManager.GET_ACTIVITIES);
            mResultCache = new LruCache<>(packageInfo.activities.length
                    + packageInfo.services.length);
        } catch (Exception e)
        {
            throw new AssertionError(e);
        }
    }
}
