package org.kexie.android.liteproj.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;


@Using({R.raw.test_avtivity, R.raw.test_app})
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    @Reference("factory")
    private Bean o;

    @Reference("holder")
    private AppHolderTest test;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d("" + o + "  " + o.string + "  " + o.object + " " + test.context);
    }
}
