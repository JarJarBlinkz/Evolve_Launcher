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

public class AndroidPlatform extends AbstractPlatform {
    int style = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_STYLE, DEFAULT_STYLE);
    private final String ICONS1_URL = "https://raw.githubusercontent.com/Veticia/binaries/main/"+STYLES[style]+"/";
    private static final String ICONS_FALLBACK_URL = "https://pilauncher.lwiczka.pl/get_icon.php?id=";

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (!isVirtualRealityApp(app)) {
                output.add(app);
            }
        }
        return output;
    }

    @Override
    public boolean isSupported(Context context) {
        return true;
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
        if (multiwindow) {
            context.getApplicationContext().startActivity(launchIntent);
        } else {
            context.startActivity(launchIntent);
        }
    }

    private void downloadIcon(final Activity context, String pkg, String name, final Runnable callback) {
        final File file = pkg2path(context, STYLES[style]+"."+pkg);
        new Thread(() -> {
            try {
                if (ignoredIcons.contains(STYLES[style]+"."+file.getName())) {
                    //ignored icon
                } else if (downloadFile(ICONS1_URL + pkg + ".png", file)) {
                    context.runOnUiThread(callback);
                } else if (downloadFile(ICONS_FALLBACK_URL + pkg + "&set=" + STYLES[style], file)) {
                    context.runOnUiThread(callback);
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