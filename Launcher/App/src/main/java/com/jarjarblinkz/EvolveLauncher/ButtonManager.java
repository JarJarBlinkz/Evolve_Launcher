package com.jarjarblinkz.EvolveLauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("CommentedOutCode")
public class ButtonManager extends AccessibilityService
{
    public void onAccessibilityEvent(AccessibilityEvent event)
    {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String[] exploreAccessibilityEventNames = getResources().getStringArray(R.array.explore_accessibility_event_names);
            
            for (String eventName : exploreAccessibilityEventNames) {
                if (eventName.compareTo(eventText) == 0) {

                    Intent finishIntent = new Intent(MainActivity.FINISH_ACTION);
                    sendBroadcast(finishIntent);

                    Intent launchIntent = new Intent(this, MainActivity.class);

                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);


                    Log.i("PiLauncherService", "Opening launcher activity from accessibility event");
                    startActivity(launchIntent);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i("PiLauncherService", "Opening launcher activity from accessibility event (delayed 100ms)");
                            startActivity(launchIntent);
                        }
                    }, 650);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i("PiLauncherService", "Opening launcher activity from accessibility event (delayed 800ms)");
                            startActivity(launchIntent);
                        }
                    }, 800);
                }
            }
        }
    }

    public void onInterrupt() {}

    //protected void onServiceConnected() {
    //    super.onServiceConnected();
    //}

    @SuppressWarnings("UnusedReturnValue")
    public static boolean isAccessibilityInitialized(Context context)
    {
        try {
            android.provider.Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

            String settingValue = android.provider.Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                return settingValue.contains(context.getPackageName());
            }
        } catch (android.provider.Settings.SettingNotFoundException e) {
            return false;
        }
        return false;
    }

    public static void requestAccessibility(Context context) {
        Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        localIntent.setPackage("com.android.settings");
        context.startActivity(localIntent);
    }

}