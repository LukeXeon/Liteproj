package org.kexie.android.arch.automatic.dependency;

/**
 * Created by Microworld on 2019/01/19.
 */


public interface Filter<T>
{
    boolean filter(T item);
}