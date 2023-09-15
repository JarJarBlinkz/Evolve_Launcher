package com.jarjarblinkz.EvolveLauncher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.jarjarblinkz.EvolveLauncher.platforms.AbstractPlatform;
import com.jarjarblinkz.EvolveLauncher.platforms.PSPPlatform;
import com.jarjarblinkz.EvolveLauncher.platforms.VRPlatform;
import com.jarjarblinkz.EvolveLauncher.ui.AppsAdapter;
import com.jarjarblinkz.EvolveLauncher.ui.GroupsAdapter;
import com.jarjarblinkz.EvolveLauncher.ui.SettingsGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.tiagohm.markdownview.MarkdownView;
import br.tiagohm.markdownview.css.InternalStyleSheet;
import br.tiagohm.markdownview.css.styles.Github;

public class MainActivity extends Activity
{
    private static final String CUSTOM_THEME = "theme.png";
    private static final boolean DEFAULT_NAMES = true;
    private static final int DEFAULT_OPACITY = 7;
    public static final int DEFAULT_SCALE = 2;
    private static final int DEFAULT_THEME = 0;
    public static final int DEFAULT_STYLE = 0;
    public static final int PICK_ICON_CODE = 450;
    public static final int PICK_THEME_CODE = 95;

    private static final int[] SCALES = {82, 99, 125, 165, 236};
    private static final int[] THEMES = {
            R.drawable.bkg_default,
            R.drawable.bkg_glass,
            R.drawable.bkg_rgb,
            R.drawable.bkg_skin,
            R.drawable.bkg_underwater
    };

    public static final String[] STYLES = {
            "banners",
            "icons",
            "tenaldo_square"
    };
    private static final boolean DEFAULT_AUTORUN = true;
    public static final String EMULATOR_PACKAGE = "org.ppsspp.ppssppvr";

    private static ImageView[] selectedThemeImageViews;
    // Edit Mode
    private GridView appGridView;
    private ImageView backgroundImageView;
    private GridView groupPanelGridView;

    @SuppressWarnings("unused")
    private boolean activityHasFocus;
    public static SharedPreferences sharedPreferences;
    private SettingsProvider settingsProvider;
    private AppsAdapter.SORT_FIELD mSortField = AppsAdapter.SORT_FIELD.APP_NAME;
    private AppsAdapter.SORT_ORDER mSortOrder = AppsAdapter.SORT_ORDER.ASCENDING;

    public static void reset(Context context) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            ((Activity)context).finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"SourceLockedOrientationActivity", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (AbstractPlatform.isMagicLeapHeadset()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        sharedPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        settingsProvider = SettingsProvider.getInstance(this);

        // Get UI instances
        appGridView = findViewById(R.id.appsView);
        backgroundImageView = findViewById(R.id.bkg_default);
        groupPanelGridView = findViewById(R.id.groupsView);

        // Handle group click listener
        groupPanelGridView.setOnItemClickListener((parent, view, position, id) -> {
            List<String> groups = settingsProvider.getAppGroupsSorted(false);
            if (hasSelection) {
                GroupsAdapter groupsAdapter = (GroupsAdapter) groupPanelGridView.getAdapter();
                if(position<groups.size()) {
                    for (String app : currentSelectedApps) groupsAdapter.setGroup(app, groups.get(position));
                }
                else {
                    for (String app : currentSelectedApps) groupsAdapter.setGroup(app, GroupsAdapter.HIDDEN_GROUP);
                }
                hasSelection = false;
                updateSelectionHint();
                reloadUI();
            } else {
                //List<String> groups = settingsProvider.getAppGroupsSorted(false);
                if (position == groups.size()) {
                    settingsProvider.selectGroup(GroupsAdapter.HIDDEN_GROUP);
                } else if (position == groups.size() + 1) {
                    settingsProvider.selectGroup(settingsProvider.addGroup());
                } else {
                    settingsProvider.selectGroup(groups.get(position));
                }
                reloadUI();
            }

        });

        // Multiple group selection
        groupPanelGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false)) {
                List<String> groups = settingsProvider.getAppGroupsSorted(false);
                Set<String> selected = settingsProvider.getSelectedGroups();

                String item = groups.get(position);
                if (selected.contains(item)) {
                    selected.remove(item);
                } else {
                    selected.add(item);
                }
                if (selected.isEmpty()) {
                    selected.add(groups.get(0));
                }
                settingsProvider.setSelectedGroups(selected);
                reloadUI();
            }
            return true;
        });

        // Set pi button
        View pi = findViewById(R.id.pi);
        pi.setOnClickListener(view -> showSettingsMain());
        pi.setOnLongClickListener(view -> {
            boolean editMode = sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
            if (!editMode) {
                settingsProvider.setSelectedGroups(settingsProvider.getAppGroups());
                reloadUI();
            }
            return true;
        });

        // Set sort button
        mSortField = AppsAdapter.SORT_FIELD.values()[sharedPreferences.getInt(SettingsProvider.KEY_SORT_FIELD, 0)];
        mSortOrder = AppsAdapter.SORT_ORDER.values()[sharedPreferences.getInt(SettingsProvider.KEY_SORT_ORDER, 0)];
        Spinner sortSpinner = findViewById(R.id.sort);
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this, R.array.sort_options, R.layout.spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setSelection(sharedPreferences.getInt(SettingsProvider.KEY_SORT_SPINNER, 0));
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switch (pos) {
                    //case 0 = default
                    case 1:
                        mSortField = AppsAdapter.SORT_FIELD.APP_NAME;
                        mSortOrder = AppsAdapter.SORT_ORDER.DESCENDING;
                        break;
                    case 2:
                        mSortField = AppsAdapter.SORT_FIELD.RECENT_DATE;
                        mSortOrder = AppsAdapter.SORT_ORDER.ASCENDING;
                        break;
                    case 3:
                        mSortField = AppsAdapter.SORT_FIELD.RECENT_DATE;
                        mSortOrder = AppsAdapter.SORT_ORDER.DESCENDING;
                        break;
                    case 4:
                        mSortField = AppsAdapter.SORT_FIELD.INSTALL_DATE;
                        mSortOrder = AppsAdapter.SORT_ORDER.ASCENDING;
                        break;
                    case 5:
                        mSortField = AppsAdapter.SORT_FIELD.INSTALL_DATE;
                        mSortOrder = AppsAdapter.SORT_ORDER.DESCENDING;
                        break;
                    default:
                        mSortField = AppsAdapter.SORT_FIELD.APP_NAME;
                        mSortOrder = AppsAdapter.SORT_ORDER.ASCENDING;
                        break;
                }

                //update UI
                if (appGridView.getAdapter() != null) {
                    ((AppsAdapter) appGridView.getAdapter()).sort(mSortField, mSortOrder);
                }

                //persist sort settings
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_SORT_SPINNER, pos);
                editor.putInt(SettingsProvider.KEY_SORT_FIELD, mSortField.ordinal());
                editor.putInt(SettingsProvider.KEY_SORT_ORDER, mSortOrder.ordinal());
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing here
            }
        });

        // Set update button
        View update = findViewById(R.id.update);
        update.setVisibility(View.GONE);
        update.setOnClickListener(view -> showUpdateMain());
        checkForUpdates(update);

        IntentFilter filter = new IntentFilter(FINISH_ACTION);
        registerReceiver(finishReceiver, filter);
    }
    private long lastUpdateCheck = 0L;
    // Stuff to finish the activity when it's in the background;
    // More straightforward methods don't work on Quest.
    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { finish();}
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }
    public static final String FINISH_ACTION = "com.threethan.launcher.FINISH";
    private void checkForUpdates(View update) {
        // disable all update checks
        if (true) {
            return;
        }
        //once every 4 hours
       return;
   }

    @Override
    public void onBackPressed() {
        if (AbstractPlatform.isMagicLeapHeadset()) {
            super.onBackPressed();
        } else {
            showSettingsMain();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityHasFocus = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityHasFocus = true;

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        boolean read = checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED;
        boolean write = checkSelfPermission(permissions[1]) == PackageManager.PERMISSION_GRANTED;
        if (read && write) {
            View update = findViewById(R.id.update);
            checkForUpdates(update);
            reloadUI();
        } else {
            requestPermissions(permissions, 0);
        }
    }

    private ImageView mSelectedImageView;

    public void setSelectedImageView(ImageView imageView) {
        mSelectedImageView = imageView;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ICON_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    ((AppsAdapter) appGridView.getAdapter()).onImageSelected(image.getPath(), mSelectedImageView);
                    break;
                }
            } else {
                ((AppsAdapter) appGridView.getAdapter()).onImageSelected(null, mSelectedImageView);
            }
        } else if (requestCode == PICK_THEME_CODE) {
            if (resultCode == RESULT_OK) {
                for (Image image : ImagePicker.getImages(data)) {
                    Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(image.getPath()), 1280);
                    ImageUtils.saveBitmap(bitmap, new File(getApplicationInfo().dataDir, CUSTOM_THEME));
                    setTheme(selectedThemeImageViews, THEMES.length);
                    reloadUI();
                    break;
                }
            }
        }
    }

    public String getSelectedPackage() {
        return ((AppsAdapter) appGridView.getAdapter()).getSelectedPackage();
    }

    public void reloadUI() {

        // set customization
        boolean names = sharedPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES);
        int opacity = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY);
        int theme = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        int scale = getPixelFromDip(SCALES[sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE)]);
        appGridView.setColumnWidth(scale);
        if (theme < THEMES.length) {
            Drawable d = getDrawable(THEMES[theme]);
            Drawable dCopy = d.getConstantState().newDrawable().mutate();
            dCopy.setAlpha(255 * opacity / 10);
            backgroundImageView.setImageDrawable(dCopy);
        } else {
            File file = new File(getApplicationInfo().dataDir, CUSTOM_THEME);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Drawable d = new BitmapDrawable(getResources(), bitmap);
            Drawable dCopy = d.getConstantState().newDrawable().mutate();
            dCopy.setAlpha(255 * opacity / 10);
            backgroundImageView.setImageDrawable(dCopy);
        }

        // set context
        scale += getPixelFromDip(8);
        boolean editMode = sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
        appGridView.setAdapter(new AppsAdapter(this, editMode, scale, names));
        groupPanelGridView.setAdapter(new GroupsAdapter(this, editMode));
        groupPanelGridView.setNumColumns(Math.min(groupPanelGridView.getAdapter().getCount(), GroupsAdapter.MAX_GROUPS - 1));
    }

    public void setTheme(ImageView[] views, int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SettingsProvider.KEY_CUSTOM_THEME, index);
        editor.apply();
        reloadUI();

        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
        }
        views[index].setBackgroundColor(Color.WHITE);
    }

    public void setStyle(ImageView[] views, int index) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SettingsProvider.KEY_CUSTOM_STYLE, index);
        editor.apply();
        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
        }
        views[index].setBackgroundColor(Color.WHITE);
        reloadUI();
    }

    public Dialog showPopup(int layout) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.layout).requestLayout();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        return dialog;
    }

    private boolean isSettingsLookOpen = false;

    private void showSettingsMain() {

        Dialog dialog = showPopup(R.layout.dialog_settings);
        SettingsGroup apps = dialog.findViewById(R.id.settings_apps);
        boolean editMode = !sharedPreferences.getBoolean(SettingsProvider.KEY_EDITMODE, false);
        apps.setIcon(editMode ? R.drawable.ic_editing_on : R.drawable.ic_editing_off);
        apps.setText(getString(editMode ? R.string.settings_apps_enable : R.string.settings_apps_disable));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            apps.setTooltipText(getString(editMode ? R.string.settings_apps_enable : R.string.settings_apps_disable));
        }
        apps.setOnClickListener(view1 -> {
            ArrayList<String> selected = settingsProvider.getAppGroupsSorted(true);
            if (editMode && (selected.size() > 1)) {
                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(selected.get(0));
                settingsProvider.setSelectedGroups(selectFirst);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_EDITMODE, editMode);
            editor.apply();
            reloadUI();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.settings_look).setOnClickListener(view -> {
            if (!isSettingsLookOpen) {
                isSettingsLookOpen = true;
                showSettingsLook();
            }
        });



        dialog.findViewById(R.id.settings_platforms).setOnClickListener(view -> showSettingsPlatforms());
        dialog.findViewById(R.id.settings_tweaks).setOnClickListener(view -> showSettingsTweaks());
        //add webkit for new releases
        dialog.findViewById(R.id.settings_webview).setOnClickListener((view -> showSettingsNew()));
        //
        //add ftp placeholder
        dialog.findViewById(R.id.settings_ftp).setOnClickListener((view -> showSettingsftp()));
        //
        //add sideload Placeholder
        dialog.findViewById(R.id.settings_sideload).setOnClickListener((view -> showSettingssideload()));
        //
        dialog.findViewById(R.id.settings_device).setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage("com.android.settings");
            startActivity(intent);
        });
        if (AbstractPlatform.isMagicLeapHeadset()) {
            dialog.findViewById(R.id.settings_device).setVisibility(View.GONE);
        }
        if (!AbstractPlatform.isOculusHeadset()) {
            dialog.findViewById(R.id.settings_tweaks).setVisibility(View.GONE);
        }
    }

    private void showUpdateMain() {
        Dialog dialog = showPopup(R.layout.dialog_update);

        MarkdownView mMarkdownView = dialog.findViewById(R.id.markdown_view);
        InternalStyleSheet css = new Github();
        css.addRule("body", "color: #FFF", "background: rgba(0,0,0,0);");
        mMarkdownView.addStyleSheet(css);
        mMarkdownView.setBackgroundColor(Color.TRANSPARENT);
        mMarkdownView.loadMarkdownFromUrlFallback("https://raw.githubusercontent.com/Veticia/PiLauncherNext/main/CHANGELOG.md",
            "**Couldn't load changelog. Check [here](https://github.com/Veticia/binaries/tree/main/releases) for the latest file.**");
    }

    private void showSettingsLook() {
        Dialog d = showPopup(R.layout.dialog_look);
        d.setOnDismissListener(dialogInterface -> isSettingsLookOpen = false);
        d.findViewById(R.id.open_accessibility).setOnClickListener(view -> {
            ButtonManager.isAccessibilityInitialized(this);
            ButtonManager.requestAccessibility(this);
        });
        Switch names = d.findViewById(R.id.checkbox_names);
        names.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_CUSTOM_NAMES, DEFAULT_NAMES));
        names.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_CUSTOM_NAMES, value);
            editor.apply();
            reloadUI();
        });

        SeekBar opacity = d.findViewById(R.id.bar_opacity);
        opacity.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_OPACITY, DEFAULT_OPACITY));
        opacity.setMax(10);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            opacity.setMin(0);
        }
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_OPACITY, value);
                editor.apply();
                reloadUI();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        SeekBar scale = d.findViewById(R.id.bar_scale);
        scale.setProgress(sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE));
        scale.setMax(SCALES.length - 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scale.setMin(0);
        }
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(SettingsProvider.KEY_CUSTOM_SCALE, value);
                editor.apply();
                reloadUI();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int theme = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_THEME, DEFAULT_THEME);
        ImageView[] views = {
                d.findViewById(R.id.theme0),
                d.findViewById(R.id.theme1),
                d.findViewById(R.id.theme2),
                d.findViewById(R.id.theme3),
                d.findViewById(R.id.theme4),
                d.findViewById(R.id.theme_custom)
        };
        for (ImageView image : views) {
            image.setBackgroundColor(Color.TRANSPARENT);
        }
        views[theme].setBackgroundColor(Color.WHITE);
        for (int i = 0; i < views.length; i++) {
            int index = i;
            views[i].setOnClickListener(view12 -> {
                //noinspection NonStrictComparisonCanBeEquality
                if (index >= THEMES.length) {
                    selectedThemeImageViews = views;
                    ImageUtils.showImagePicker(this, PICK_THEME_CODE);
                } else {
                    setTheme(views, index);
                }
            });
        }
        int style = sharedPreferences.getInt(SettingsProvider.KEY_CUSTOM_STYLE, DEFAULT_STYLE);
        if (style >= STYLES.length) { style = 0; }
        ImageView[] styles = {
                d.findViewById(R.id.style0),
                d.findViewById(R.id.style1),
                d.findViewById(R.id.style2)
        };
        for (ImageView image : styles) {
            image.setBackgroundColor(Color.TRANSPARENT);
        }
        styles[style].setBackgroundColor(Color.WHITE);
        for (int i = 0; i < styles.length; i++) {
            int index = i;
            styles[i].setOnClickListener(view13 -> setStyle(styles, index));
        }
        Switch autorun = d.findViewById(R.id.checkbox_autorun);
        autorun.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_AUTORUN, DEFAULT_AUTORUN));
        autorun.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_AUTORUN, value);
            editor.apply();
        });
        d.findViewById(R.id.service_backup_settings).setOnClickListener(view -> {
            saveSharedPreferencesToFile(new File("/sdcard/PiLauncherBackup.xml"));
        });
        d.findViewById(R.id.service_restore_settings).setOnClickListener(view -> {
            loadSharedPreferencesFromFile(new File("/sdcard/PiLauncherBackup.xml"));
            doRestart(getApplicationContext());
        }); //setOnClickListener
    }


    private void showSettingsPlatforms() {
        Dialog d = showPopup(R.layout.dialog_platforms);

        Switch android = d.findViewById(R.id.checkbox_android);
        android.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_ANDROID, true));
        android.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_ANDROID, value);
            editor.apply();
            reloadUI();
        });
        d.findViewById(R.id.layout_android).setVisibility(View.VISIBLE); //android platform is always supported

        Switch psp = d.findViewById(R.id.checkbox_psp);
        psp.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_PSP, true));
        psp.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_PSP, value);
            editor.apply();
            reloadUI();
        });
        d.findViewById(R.id.layout_psp).setVisibility(new PSPPlatform().isSupported(this) ? View.VISIBLE : View.GONE);

        Switch vr = d.findViewById(R.id.checkbox_vr);
        vr.setChecked(sharedPreferences.getBoolean(SettingsProvider.KEY_PLATFORM_VR, true));
        vr.setOnCheckedChangeListener((compoundButton, value) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SettingsProvider.KEY_PLATFORM_VR, value);
            editor.apply();
            reloadUI();
        });
        d.findViewById(R.id.layout_vr).setVisibility(new VRPlatform().isSupported() ? View.VISIBLE : View.GONE);
    }

    private void showSettingsTweaks() {
        Dialog d = showPopup(R.layout.dialog_tweaks);

        d.findViewById(R.id.service_app_shortcut).setOnClickListener(view -> {
            ButtonManager.isAccessibilityInitialized(this);
            ButtonManager.requestAccessibility(this);
        });
        d.findViewById(R.id.service_explore_app).setOnClickListener(view -> openAppDetails("com.oculus.explore"));
        d.findViewById(R.id.service_os_updater).setOnClickListener(view -> openAppDetails("com.oculus.updater"));
    }
    //Add Placeholder for ftp
    private void showSettingsftp() {
        Dialog d = showPopup(R.layout.dialog_ftp);
    }
    //Add Placeholder for sideload
    private void showSettingssideload() {
        Dialog d = showPopup(R.layout.dialog_sideload);
    }
    //add webkit for new releases
    @SuppressLint("SetJavaScriptEnabled")
    private void showSettingsNew() {
        Dialog d = showPopup(R.layout.dialog_webview);
        WebView NewReleases = d.findViewById(R.id.new_releases_webview);
        NewReleases.getSettings().setDomStorageEnabled(true);
        NewReleases.getSettings().setJavaScriptEnabled(true);
        NewReleases.loadUrl("https://www.oculus.com/experiences/quest/section/2540605779297669/#/?_k=n7l7da");
    }

    private int getPixelFromDip(int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    private final Handler handler = new Handler();

    public boolean openApp(ApplicationInfo app) {
        settingsProvider.updateRecent(app.packageName, System.currentTimeMillis());
        AbstractPlatform platform = AbstractPlatform.getPlatform(app);
        if(!platform.runApp(this, app, false)){
            TextView toastText = findViewById(R.id.toast_text);
            toastText.setText(R.string.failed_to_launch);
            toastText.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(500);
            fadeIn.setFillAfter(true);
            toastText.startAnimation(fadeIn);

            // Remove any previously scheduled Runnables
            handler.removeCallbacksAndMessages(null);

            handler.postDelayed(() -> {
                AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                fadeOut.setDuration(1000);
                fadeOut.setFillAfter(true);
                toastText.startAnimation(fadeOut);
            }, 2000);
            return false;
        }
        return true;
    }

    public void openAppDetails(String pkg) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + pkg));
        startActivity(intent);
    }

    // Edit Mode
    Set<String> currentSelectedApps = new HashSet<>();
    boolean hasSelection = false;
    public boolean selectApp(String app) {

        if (currentSelectedApps.contains(app)) {
            currentSelectedApps.remove(app);
            if (currentSelectedApps.isEmpty()) hasSelection = false;
            updateSelectionHint();
            return false;
        } else {
            currentSelectedApps.add(app);
            hasSelection = true;
            updateSelectionHint();

            return true;
        }
    }
    void updateSelectionHint() {
        TextView selectionHint = findViewById(R.id.SelectionHint);

        final int size = currentSelectedApps.size();
        if (size == 1) selectionHint.setText("Click a group to move the selected app.");
        else selectionHint.setText(String.valueOf(size) +" apps selected. Click a group to move them.");
        selectionHint.setVisibility(hasSelection ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean saveSharedPreferencesToFile(File dst) {
        File ff = new File("/data/data/"
                + MainActivity.this.getPackageName()
                + "/shared_prefs/" + getPackageName() + "_preferences"+".xml");

        Log.i("Backing up shared prefs", ff.getPath() + "");

        try {
            copyFile(ff, dst);
        } catch (IOException e) {
            //throw new RuntimeException(e);
            e.printStackTrace();
            return false;
        }
        return true;

    }
    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile(File src) {
        File ff = new File("/data/data/"
                + MainActivity.this.getPackageName()
                + "/shared_prefs/" + getPackageName() + "_preferences"+".xml");

        Log.i("Restoring shared prefs", ff.getPath() + "");

        try {
            copyFile(src, ff);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }
    public static void doRestart(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e("PI", "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e("PI", "Was not able to restart application, PM null");
                }
            } else {
                Log.e("PI", "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e("PI", "Was not able to restart application");
        }
    }

}
