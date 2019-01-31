package org.kexie.android.arch.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.orhanobut.logger.Logger;

import org.kexie.android.arch.ioc.Reference;
import org.kexie.android.arch.ioc.Using;

@Using(R.raw.test_main)
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    @Reference("factory")
    private Bean o;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d("" + o + "  " + o.string + "  " + o.object);
    }
}
