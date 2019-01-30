package org.kexie.android.arch.automatic.databinding;

import android.databinding.BindingAdapter;
import android.support.annotation.LayoutRes;
import android.support.v4.view.ViewPager;

import java.util.List;

public final class ViewPagerBindingAdapter
{
    private ViewPagerBindingAdapter()
    {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    @BindingAdapter({"app:itemName",
            "app:itemLayout",
            "app:dataSource"})
    public static void setAdapter(ViewPager view,
                                  String itemName,
                                  @LayoutRes int itemLayout,
                                  List<?> dataSource)
    {
        GenericPagerAdapter adapter = (GenericPagerAdapter) view.getAdapter();
        if (adapter == null
                || !adapter.setterName.equals(itemName)
                || adapter.layoutRes != itemLayout)
        {
            adapter = new GenericPagerAdapter(itemName, itemLayout);
            view.setAdapter(adapter);
        }
        adapter.setNewData(dataSource);
    }

}
