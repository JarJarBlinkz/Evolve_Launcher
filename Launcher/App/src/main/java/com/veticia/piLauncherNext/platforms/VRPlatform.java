package com.veticia.piLauncherNext.platforms;

import static com.veticia.piLauncherNext.MainActivity.DEFAULT_STYLE;
import static com.veticia.piLauncherNext.MainActivity.STYLES;
import static com.veticia.piLauncherNext.MainActivity.mPreferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.ImageView;

import com.veticia.piLauncherNext.SettingsProvider;

import java.io.File;
import java.util.ArrayList;

public class VRPlatform extends AbstractPlatform {
    int style = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_STYLE, DEFAULT_STYLE);
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
            if (isVirtualRealityApp(app)) {
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
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        icon.setImageDrawable(app.loadIcon(activity.getPackageManager()));

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
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        context.getApplicationContext().startActivity(launchIntent);
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