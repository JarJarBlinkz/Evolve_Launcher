package com.jarjarblinkz.EvolveLauncher.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.jarjarblinkz.EvolveLauncher.SettingsProvider;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class VRPlatform extends AbstractPlatform {
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> installedApps = new ArrayList<>();
        if (!isSupported()) {
            return installedApps;
        }
        PackageManager pm = context.getPackageManager();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (isVirtualRealityApp(app)) {
                if (!SettingsProvider.launchIntents.containsKey(app.packageName)) {
                    SettingsProvider.launchIntents.put(app.packageName, pm.getLaunchIntentForPackage(app.packageName));
                }
                if(SettingsProvider.launchIntents.get(app.packageName) != null) {
                    if(!SettingsProvider.installDates.containsKey(app.packageName)) {
                        long installDate;
                        try {
                            PackageInfo packageInfo = pm.getPackageInfo(app.packageName, 0);
                            installDate = packageInfo.firstInstallTime;
                        } catch (PackageManager.NameNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                        SettingsProvider.installDates.put(app.packageName, installDate);
                    }
                    installedApps.add(app);
                }
            }
        }
        return installedApps;
    }

    public boolean isSupported() {
        return isMagicLeapHeadset() || isOculusHeadset() || isPicoHeadset();
    }

    @Override
    public boolean runApp(Context context, ApplicationInfo app, boolean multiwindow) {

        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (launchIntent != null) {
                if (SettingsProvider.getAppLaunchOut(app.packageName)) {
                    ((Activity) context).finish();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                context.startActivity(launchIntent);
                            }catch (Exception e) {
                                Log.e("runApp", "Failed to launch - exception in startActivity");
                            }
                        }
                    }, 650);
                } else {
                    context.startActivity(launchIntent);
                }
                return true;
            } else {
                Log.e("runApp", "Failed to launch - NULL launchIntent");
                return false;
            }
        }
        catch (Exception e) {
            Log.e("runApp", "Failed to launch - exception");
            return false;
        }

    }
}