package com.veticia.piLauncherNext.platforms;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;

public class VRPlatform extends AbstractPlatform {
    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> installedApps = new ArrayList<>();
        if (!isSupported(context)) {
            return installedApps;
        }

        PackageManager pm = context.getPackageManager();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (isVirtualRealityApp(app) && launchIntent != null) {
                try {
                    PackageInfo packageInfo = pm.getPackageInfo(app.packageName, 0);
                    long installDate = packageInfo.firstInstallTime;
                    app.taskAffinity = Long.toString(installDate);
                }
                catch (PackageManager.NameNotFoundException e) {
                    app.taskAffinity = "0";
                }
                installedApps.add(app);
            }
        }
        return installedApps;
    }

    @Override
    public boolean isSupported(Context context) {
        return isMagicLeapHeadset() || isOculusHeadset() || isPicoHeadset();
    }

    @Override
    public boolean runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launchIntent != null) {
            context.getApplicationContext().startActivity(launchIntent);
        } else {
            Log.e("runApp", "Failed to launch");
            return false;
        }
        return true;
    }
}