package org.kexie.android.liteporj;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.WeakHashMap;

//依赖管理器
public final class DependencyManager
{
    //使用‘owner’来访问依赖持有者
    @SuppressWarnings("WeakerAccess")
    public static final String OWNER = "owner";
    //静态表用来保存和对象关联的依赖管理器
    final static Map<Object, DependencyManager> sTable = new WeakHashMap<>();
    //持有者的类型
    private Class<?> mOwnerType;
    //使用弱引用保存持有者，防止内存泄漏
    private WeakReference<Object> mOwner;
    //每种依赖对应的依赖管理器管理器是不同的，将依赖请求转发给其他管理器
    private Map<Dependency, DependencyManager> mManagers;
    //用来缓存单例模式的快表
    private Map<String, Object> mSingletons = new ArrayMap<>();

    //可以从这里很方便的得到依赖管理器
    public static DependencyManager form(Object owner)
    {
        return sTable.get(owner);
    }

    //构造函数
    public DependencyManager(@NonNull Object owner,
                             @NonNull Map<Dependency, DependencyManager> managers)
    {
        mOwnerType = owner.getClass();
        mOwner = new WeakReference<>(owner);
        mManagers = managers;
    }

    //返回持有者，若持有者已经被释放，则报错
    @NonNull
    private Object getOwner()
    {
        return Objects.requireNonNull(mOwner.get(),
                "Owner has been released");
    }

    //获取依赖，若此管理器上不存在，则将请求转发到其他管理器
    @NonNull
    @SuppressWarnings({"WeakerAccess", "unchecked"})
    public <T> T get(String name)
    {
        if (OWNER.equals(name))
        {
            return (T) getOwner();
        }
        for (Dependency dependency : mManagers.keySet())
        {
            DependencyProvider provider = dependency.getProvider(name);
            if (provider != null)
            {
                if (DependencyType.SINGLETON.equals(provider.getType()))
                {
                    Object singleton = mSingletons.get(name);
                    if (singleton == null)
                    {
                        singleton = provider.provide(mManagers.get(dependency));
                        mSingletons.put(name, singleton);
                    }
                    return (T) singleton;
                }
                return provider.provide(mManagers.get(dependency));
            }
        }
        throw new NoSuchElementException(String.format("By name %s", name));
    }

    //获取依赖类型，但是不进行生成操作
    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    public Class<?> getResultType(String name)
    {
        if (OWNER.equals(name))
        {
            return mOwnerType;
        }
        for (Dependency item : mManagers.keySet())
        {
            DependencyProvider provider = item.getProvider(name);
            if (provider != null)
            {
                return provider.getResultType();
            }
        }
        throw new NoSuchElementException(String.format("By name %s", name));
    }

    //获取依赖的提供器类型,单例或者是工厂模式
    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    public DependencyType getDependencyType(String name)
    {
        if (OWNER.equals(name))
        {
            return DependencyType.SINGLETON;
        }
        for (Dependency item : mManagers.keySet())
        {
            DependencyProvider provider = item.getProvider(name);
            if (provider != null)
            {
                return provider.getType();
            }
        }
        throw new NoSuchElementException(String.format("By name %s", name));
    }

    //在生命周期结束时调用,清理内部状态
    void onDestroy()
    {
        mOwnerType = null;
        mOwner = null;
        mManagers = null;
        mSingletons = null;
    }
}
