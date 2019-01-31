package org.kexie.android.arch.automatic.permissson;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;


public final class PermissionsManager
{
    private PermissionsManager()
    {
        throw new AssertionError();
    }

    private final static String[] EMPTY_STRING_ARRAY = new String[0];


    public interface Callback
    {
        void onPermissionsDenied(@NonNull String[] permissions);
    }

    @NonNull
    public static String[] getDefinedPermissions(Application application)
    {
        try
        {
            PackageInfo packageInfo = application.getPackageManager()
                    .getPackageInfo(application.getPackageName(),
                            PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null)
            {
                return packageInfo.requestedPermissions;
            } else
            {
                return EMPTY_STRING_ARRAY;
            }
        } catch (PackageManager.NameNotFoundException e)
        {
            throw new AssertionError(e);
        }
    }

    @NonNull
    public static String[] getDeniedPermissions(Application application)
    {
        String[] requestedPermissions = getDefinedPermissions(application);
        if (requestedPermissions.length != 0)
        {
            ArrayList<String> requestedPermissionsList = new ArrayList<>();
            for (String permission : requestedPermissions)
            {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat
                        .checkSelfPermission(application, permission))
                {
                    requestedPermissionsList.add(permission);
                    // 进入到这里代表没有权限.
                }
            }
            requestedPermissionsList.toArray(EMPTY_STRING_ARRAY);
        }
        return EMPTY_STRING_ARRAY;
    }

    public static void init(Application application)
    {
        String[] requestedPermissions = getDeniedPermissions(application);
        if (requestedPermissions.length != 0)
        {
            RequestFragment.createByFirstActivity(application, requestedPermissions);
        }
    }
}
