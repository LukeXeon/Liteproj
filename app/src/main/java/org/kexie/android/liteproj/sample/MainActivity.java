package org.kexie.android.liteproj.sample;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;

import okhttp3.Request;


@Using({R.xml.all_test,R.xml.test2,R.xml.test2})
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    @Reference
    String url3;

    @Reference("url2")
    String url2;

    @Reference("request")
    Request request;

    @Reference("bean")
    Bean bean;

    @Reference("holder")
    AppHolderTest holderTest;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Logger.addLogAdapter(new AndroidLogAdapter());
        setContentView(R.layout.activity_main);
        Logger.d(request + "\n" + bean + "\n" + holderTest.context);
        Logger.d(url2);
        Logger.d(url3);
        ViewModelTest viewModel = ViewModelProviders.of(this).get(ViewModelTest.class);
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
