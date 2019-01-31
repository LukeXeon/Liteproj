package org.kexie.android.arch.sample;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.orhanobut.logger.Logger;

import org.kexie.android.arch.automatic.dependency.DependenciesManager;
import org.kexie.android.arch.automatic.dependency.Reference;
import org.kexie.android.arch.automatic.dependency.Using;

@Using(R.raw.test_main)
public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";

    @Reference("test")
    private Object o;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.d(o+"");
    }

}
