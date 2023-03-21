package com.veticia.piLauncherNext.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

public class VRPlatform extends AbstractPlatform {

    private static final String ICONS1_URL = "https://raw.githubusercontent.com/Veticia/binaries/main/banners/";

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        if (!isSupported(context)) {
            return output;
        }

        PackageManager pm = context.getPackageManager();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (isVirtualRealityApp(app)) {
                output.add(app);
            }
        }
        return output;
    }

    @Override
    public boolean isSupported(Context context) {
        return isMagicLeapHeadset() || isOculusHeadset() || isPicoHeadset();
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        icon.setImageDrawable(app.loadIcon(activity.getPackageManager()));

        String pkg = app.packageName;
        if (iconCache.containsKey(pkg)) {
            icon.setImageDrawable(iconCache.get(pkg));
            return;
        }

        final File file = pkg2path(activity, pkg);
        if (file.exists()) {
            if (AbstractPlatform.updateIcon(icon, file, pkg)) {
                return;
            }
        }
        downloadIcon(activity, pkg, name, () -> AbstractPlatform.updateIcon(icon, file, pkg));
    }

    @Override
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        context.getApplicationContext().startActivity(launchIntent);
    }

    private void downloadIcon(final Activity context, String pkg, String name, final Runnable callback) {
        final File file = pkg2path(context, pkg);
        new Thread(() -> {
            try {
                String autogen = null;
                if (ignoredIcons.contains(file.getName())) {
                    //ignored icon
                } else if (downloadFile(ICONS1_URL + pkg + ".png", file)) {
                    context.runOnUiThread(callback);
                } else {
                    Log.d("Missing icon", file.getName());
                    ignoredIcons.add(file.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
