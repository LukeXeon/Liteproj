package org.kexie.android.arch.automatic.databinding;


import android.databinding.BindingAdapter;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public final class RecyclerViewBindingAdapter
{
    private RecyclerViewBindingAdapter()
    {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    @BindingAdapter({"app:itemName",
            "app:itemLayout",
            "app:dataSource"})
    public static void setAdapter(RecyclerView view,
                                  String itemName,
                                  @LayoutRes int itemLayout,
                                  List<?> dataSource)
    {
        GenericRecyclerAdapter adapter = (GenericRecyclerAdapter) view.getAdapter();
        if (adapter == null
                || !adapter.setterName.equals(itemName)
                || adapter.layoutRes != itemLayout)
        {
            adapter = new GenericRecyclerAdapter(itemName, itemLayout);
            view.setAdapter(adapter);
        }
        adapter.setNewData(dataSource);
    }
}
