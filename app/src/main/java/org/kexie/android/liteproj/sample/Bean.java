package org.kexie.android.liteproj.sample;

public class Bean
{
    public float field;

    public String string;

    Object object;

    public void setObject(Object object)
    {
        this.object = object;
    }

    @Override
    public String toString()
    {
        return super.toString() + "\n" + field + "\n" + object + "\n" + string;
    }
}
