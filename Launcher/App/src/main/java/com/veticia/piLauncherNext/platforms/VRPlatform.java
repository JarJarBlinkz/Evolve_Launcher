package com.veticia.piLauncherNext.platforms;

import static com.veticia.piLauncherNext.MainActivity.DEFAULT_STYLE;
import static com.veticia.piLauncherNext.MainActivity.STYLES;
import static com.veticia.piLauncherNext.MainActivity.sharedPreferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import com.veticia.piLauncherNext.SettingsProvider;

import java.io.File;
import java.util.ArrayList;

public class VRPlatform extends AbstractPlatform {
    int style = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_STYLE, DEFAULT_STYLE);
    private final String ICONS1_URL = "https://raw.githubusercontent.com/Veticia/binaries/main/"+STYLES[style]+"/";
    private static final String ICONS_FALLBACK_URL = "https://pilauncher.lwiczka.pl/get_icon.php?id=";

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
                //debug
                //Log.e("VRPmDate", app.packageName + " @ " + app.taskAffinity);
            }
        }
        return installedApps;
    }

    @Override
    public boolean isSupported(Context context) {
        return isMagicLeapHeadset() || isOculusHeadset() || isPicoHeadset();
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        PackageManager pm = activity.getPackageManager();
        Resources resources;
        try {
            resources = pm.getResourcesForApplication(app.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        int iconId = app.icon;
        if (iconId == 0) {
            iconId = android.R.drawable.sym_def_app_icon;
        }
        Drawable appIcon = ResourcesCompat.getDrawableForDensity(resources, iconId, DisplayMetrics.DENSITY_XXXHIGH, null);
        icon.setImageDrawable(appIcon);

        String pkg = app.packageName;
        if (iconCache.containsKey(STYLES[style]+"."+pkg)) {
            icon.setImageDrawable(iconCache.get(STYLES[style]+"."+pkg));
            return;
        }

        final File file = pkg2path(activity, STYLES[style]+"."+pkg);
        if (file.exists()) {
            if (AbstractPlatform.updateIcon(icon, file, STYLES[style]+"."+pkg)) {
                return;
            }
        }
        downloadIcon(activity, pkg, name, () -> AbstractPlatform.updateIcon(icon, file, STYLES[style]+"."+pkg));
    }

    @Override
    public boolean runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launchIntent != null) {
            context.getApplicationContext().startActivity(launchIntent);
        } else {
            // Handle the case where the app doesn't have a launch intent
            //Toast's don't work in pico apparently
            //Toast.makeText(context, "Failed to launch", Toast.LENGTH_SHORT).show();
            Log.e("runApp", "Failed to launch");
            return false;
        }
        return true;
    }

    private void downloadIcon(final Activity activity, String pkg, String name, final Runnable callback) {
        final File file = pkg2path(activity, STYLES[style]+"."+pkg);
        new Thread(() -> {
            try {
                if (ignoredIcons.contains(STYLES[style]+"."+file.getName())) {
                    //ignored icon
                } else if (downloadIconFromUrl(ICONS1_URL + pkg + ".png", file)) {
                    activity.runOnUiThread(callback);
                } else if (downloadIconFromUrl(ICONS_FALLBACK_URL + pkg + "&set=" + STYLES[style], file)) {
                    activity.runOnUiThread(callback);
                } else {
                    Log.d("Missing icon", file.getName());
                    ignoredIcons.add(STYLES[style]+"."+file.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}