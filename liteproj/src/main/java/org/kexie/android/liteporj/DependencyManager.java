package org.kexie.android.liteporj;

import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
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
    @SuppressWarnings("WeakerAccess")
    public static DependencyManager form(Object owner)
    {
        return sTable.get(owner);
    }

    @NonNull
    private Map<Dependency, DependencyManager>
    merge(@NonNull Object owner, @NonNull List<Dependency> dependencies)
    {
        //check
        if (dependencies.size() > 1)
        {
            Set<String> set = dependencies.get(0).getNames();
            Set<String> result = new ArraySet<>();
            for (Dependency relation : dependencies
                    .subList(1, dependencies.size()))
            {
                Set<String> newSet = relation.getNames();
                result.addAll(set);
                result.retainAll(newSet);
                if (result.size() == 0)
                {
                    set.addAll(newSet);
                } else
                {
                    throw new GenerateDepartmentException(
                            String.format(
                                    "Dependency conflicts occur during Mergers set = %s",
                                    result.toString()));
                }
            }
        }
        Map<Dependency, DependencyManager> managers = new ArrayMap<>();
        for (Dependency dependency : dependencies)
        {
            if (dependency.getOwnerType()
                    .isAssignableFrom(owner.getClass()))
            {
                managers.put(dependency, this);
            } else if (owner instanceof FragmentActivity
                    || owner instanceof LiteService)
            {
                managers.putAll(mergeUpper(
                        ((ContextWrapper) owner).getApplicationContext(),
                        dependencies));
            } else if (owner instanceof Fragment)
            {
                managers.putAll(mergeUpper(
                        Objects.requireNonNull(((Fragment) owner)
                                .getActivity()),
                        dependencies));
            } else if (owner instanceof LiteViewModel)
            {
                managers.putAll(mergeUpper(
                        ((LiteViewModel) owner).getApplication(),
                        dependencies));
            }
        }
        return managers;
    }

    @NonNull
    private static Map<Dependency, DependencyManager>
    mergeUpper(@NonNull Object owner,
               @NonNull List<Dependency> dependencies)
    {
        Map<Dependency, DependencyManager> result = new ArrayMap<>();
        List<Dependency> filterResult = new LinkedList<>();
        for (Dependency dependency : dependencies)
        {
            if (dependency.getOwnerType()
                    .isAssignableFrom(owner.getClass()))
            {
                filterResult.add(dependency);
            }
        }
        DependencyManager manager = sTable.get(owner);
        if (manager != null)
        {
            for (Dependency dependency : dependencies)
            {
                manager.mManagers.put(dependency, manager);
            }
        } else
        {
            manager = new DependencyManager(owner, filterResult);
            sTable.put(owner, manager);
        }
        for (Dependency dependency : filterResult)
        {
            result.put(dependency, manager);
        }
        if (owner instanceof FragmentActivity
                || owner instanceof LiteService)
        {
            result.putAll(mergeUpper(
                    ((ContextWrapper) owner).getApplicationContext(),
                    dependencies));
        } else if (owner instanceof LiteViewModel)
        {
            result.putAll(mergeUpper(
                    ((LiteViewModel) owner).getApplication(),
                    dependencies));
        } else if (owner instanceof Fragment)
        {
            result.putAll(mergeUpper(Objects.requireNonNull(((Fragment) owner)
                            .getActivity()),
                    dependencies));
        }
        return result;
    }

    //构造函数
    DependencyManager(@NonNull Object owner,
                      @NonNull List<Dependency> dependencies)
    {
        mOwnerType = owner.getClass();
        mManagers = merge(owner, dependencies);
        mOwner = new WeakReference<>(owner);
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
            Provider provider = dependency.getProvider(name);
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
            Provider provider = item.getProvider(name);
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
            Provider provider = item.getProvider(name);
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
