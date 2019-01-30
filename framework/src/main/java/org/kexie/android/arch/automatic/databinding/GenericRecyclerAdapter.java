package org.kexie.android.arch.automatic.databinding;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;


public class GenericRecyclerAdapter<T>
        extends BaseQuickAdapter<T,BaseViewHolder>
{
    final String setterName;

    final int layoutRes;

    @SuppressWarnings("WeakerAccess")
    public GenericRecyclerAdapter(String variableName,
                                  @LayoutRes int layoutResId)
    {
        super(layoutResId);
        this.layoutRes = layoutResId;
        this.setterName = variableName;
    }

    @Override
    protected void convert(BaseViewHolder helper, T data)
    {
        try
        {
            ViewDataBinding binding = DataBindingUtil.bind(helper.itemView);
            assert binding != null;
            DataBindingTool.setVariable(binding, setterName, data);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
