package org.kexie.android.liteporj;

import android.app.Application;
import android.arch.lifecycle.LifecycleObserver;
import android.support.annotation.NonNull;

import java.util.List;

//1.完善异常信息,并提供更高的出错容忍
//2.添加对jetpack组件包的支持,使用LiteViewModel支持ViewModel
//3.优化内存使用Analyzer使用单实例,删除HolderFragment
//4.添加对Service的支持,使用LiteService支持Service
//5.优化冗余代码
//6.添加更多注释

public final class Liteproj
{
    private Liteproj()
    {
        throw new AssertionError();
    }

    private static DependencyLifecycleHandler sLifecycleHandler;

    static void init(@NonNull Application application)
    {
        application.registerActivityLifecycleCallbacks(
                (sLifecycleHandler = new DependencyLifecycleHandler(application))
        );
    }

    @SuppressWarnings("WeakerAccess")
    public static LifecycleObserver getLifecycleHandler()
    {
        return sLifecycleHandler;
    }

    static DependencyManager createDependencyManager(Object owner,
                                                     List<Dependency> dependencies)
    {
        return null;
    }
}
