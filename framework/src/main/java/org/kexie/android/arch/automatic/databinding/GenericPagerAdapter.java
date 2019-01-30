package org.kexie.android.arch.automatic.databinding;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class GenericPagerAdapter<T>
        extends PagerAdapter
{
    private final String setterName;

    private final int layoutRes;

    private final SparseArray<ViewDataBinding> using;

    private final List<T> data;

    private final List<ViewDataBinding> cache;

    @SuppressWarnings("WeakerAccess")
    public GenericPagerAdapter(String variableName,
                               @LayoutRes int layoutRes)
    {
        this.setterName = variableName;
        this.layoutRes = layoutRes;
        this.using = new SparseArray<>();
        this.data = new ArrayList<>();
        this.cache = new ArrayList<>();
    }

    @NonNull
    @Override
    public View instantiateItem(@NonNull ViewGroup container,
                                int position)
    {
        ViewDataBinding binding = using.get(position);
        if (binding == null)
        {
            binding = getBinding(container);
            using.put(position, binding);
        }
        DataBindingTool.setVariable(binding, setterName, data.get(position));
        View view = binding.getRoot();
        container.addView(view);
        return view;
    }

    private ViewDataBinding getBinding(ViewGroup root)
    {
        if (!cache.isEmpty())
        {
            return cache.remove(cache.size() - 1);
        } else
        {
            return DataBindingUtil.inflate(
                    LayoutInflater.from(root.getContext()),
                    layoutRes,
                    root,
                    false
            );
        }
    }

    @Override
    public final void destroyItem(@NonNull ViewGroup container,
                                  int position,
                                  @NonNull Object object)
    {
        destroyItem(container, position, (View) object);
    }

    @SuppressWarnings({"WeakerAccess"})
    public void destroyItem(@NonNull ViewGroup container,
                            int position,
                            @NonNull View view)
    {
        container.removeView(view);
        using.remove(position);
        cache.add(DataBindingUtil.bind(view));
    }

    public List<T> getData()
    {
        return data;
    }

    @SuppressWarnings({"WeakerAccess"})
    public void setNewData(@Nullable List<T> data)
    {
        this.data.clear();
        if (data != null)
        {
            this.data.addAll(data);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return data.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view,
                                    @NonNull Object object)
    {
        return view == object;
    }
}
