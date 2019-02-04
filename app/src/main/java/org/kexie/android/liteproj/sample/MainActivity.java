package org.kexie.android.liteproj.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.orhanobut.logger.Logger;

import org.kexie.android.liteproj.Reference;
import org.kexie.android.liteproj.Using;

import okhttp3.Request;


@Using({R.xml.all_test})
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

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
        setContentView(R.layout.activity_main);
        Logger.d(request + "\n" + bean + "\n" + holderTest.context);
    }
}
