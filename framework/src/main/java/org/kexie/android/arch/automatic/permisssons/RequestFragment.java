package org.kexie.android.arch.automatic.permisssons;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.kexie.android.arch.automatic.internal.EmptyActivityLifecycleCallbacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@SuppressLint("ValidFragment")
final class RequestFragment
        extends Fragment
{

    static void createByFirstActivity(Application application,
                                      String[] permission)
    {
        Bundle bundle = new Bundle();
        bundle.putStringArray(RequestFragment.class.getCanonicalName(), permission);
        final RequestFragment fragment = new RequestFragment();
        fragment.setArguments(bundle);
        application.registerActivityLifecycleCallbacks(
                new EmptyActivityLifecycleCallbacks()
                {
                    @Override
                    public void onActivityCreated(Activity activity,
                                                  Bundle savedInstanceState)
                    {
                        if (activity instanceof AppCompatActivity)
                        {
                            AppCompatActivity appCompatActivity
                                    = (AppCompatActivity) activity;
                            Application application = (Application)
                                    appCompatActivity.getApplicationContext();
                            FragmentManager fragmentManager
                                    = appCompatActivity.getSupportFragmentManager();
                            FragmentTransaction transaction
                                    = fragmentManager.beginTransaction();
                            transaction.add(fragment, UUID.randomUUID().toString());
                            transaction.commit();
                            application.unregisterActivityLifecycleCallbacks(this);
                        } else
                        {
                            throw new RuntimeException("first activity must is AppCompatActivity");
                        }
                    }
                });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        assert bundle != null;
        String[] requestedPermissions = bundle
                .getStringArray(RequestFragment.class.getCanonicalName());
        assert requestedPermissions != null;
        requestPermissions(requestedPermissions,
                Objects.requireNonNull(getContext())
                        .getApplicationInfo().uid);
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );
        Context application = Objects.requireNonNull(getContext())
                .getApplicationContext();
        if (requestCode == application.getApplicationInfo().uid)
        {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++)
            {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    list.add(permissions[i]);
                }
            }
            if (list.size() != 0 && application instanceof Permissions.Callback)
            {
                Permissions.Callback callback = (Permissions.Callback) application;
                callback.onPermissionsDenied(list.toArray(new String[0]));
            }
            FragmentManager fragmentManager = getFragmentManager();
            assert fragmentManager != null;
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(this);
            transaction.commit();
        }
    }

}
