package org.kexie.android.arch.automatic.databinding;

import android.databinding.BindingAdapter;
import android.support.annotation.LayoutRes;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public final class ViewPagerBindingAdapter
{
    private ViewPagerBindingAdapter()
    {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    @BindingAdapter({"app:dataSource"})
    public static void setDataSource(ViewPager view, List<?> data)
    {
        GenericPagerAdapter adapter
                = (GenericPagerAdapter) view.getAdapter();
        if (adapter == null)
        {
            Log.e(RecyclerView.class.getSimpleName(),
                    "no has adapter to bind "
                            + Arrays.toString(data.toArray()));
            return;
        }
        adapter.setNewData(data);
    }

    @BindingAdapter({"app:itemName", "app:itemLayout"})
    public static void setAdapter(ViewPager view,
                                  String itemName,
                                  @LayoutRes int itemLayout)
    {
        view.setAdapter(new GenericPagerAdapter<>(itemName, itemLayout));
    }

}
