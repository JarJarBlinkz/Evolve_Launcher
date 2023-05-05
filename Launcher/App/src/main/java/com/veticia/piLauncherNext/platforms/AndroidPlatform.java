package com.veticia.piLauncherNext.platforms;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;

public class AndroidPlatform extends AbstractPlatform {
    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (!isVirtualRealityApp(app) && launchIntent != null) {
                try {
                    PackageInfo packageInfo = pm.getPackageInfo(app.packageName, 0);
                    long installDate = packageInfo.firstInstallTime;
                    app.taskAffinity = Long.toString(installDate);
                }
                catch (PackageManager.NameNotFoundException e) {
                    app.taskAffinity = "0";
                }
                output.add(app);
            }
        }
        return output;
    }

    @Override
    public boolean runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launchIntent != null) {
            if (multiwindow) {
                context.getApplicationContext().startActivity(launchIntent);
            } else {
                context.startActivity(launchIntent);
            }
        }else{
            Log.e("runApp", "Failed to launch");
            return false;
        }
        return true;
    }
}