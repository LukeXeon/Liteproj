package org.kexie.android.liteproj.sample;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;
import org.kexie.android.liteproj.util.TextUtil;


@Using({R.raw.test_avtivity, R.xml.test_app})
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
        Logger.d("test " + test);
        ViewModelTest test = ViewModelProviders.of(this).get(ViewModelTest.class);
        bindService(new Intent(this, ServiceTest.class),
                new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {

            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {

            }
        }, BIND_AUTO_CREATE);
    }

}
