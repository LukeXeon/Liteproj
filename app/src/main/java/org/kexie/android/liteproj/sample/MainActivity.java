package org.kexie.android.liteproj.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;

import okhttp3.Request;


@Using({R.xml.all_test,R.xml.test2,R.xml.test2,})
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    @Reference("url3")
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
    }
}
