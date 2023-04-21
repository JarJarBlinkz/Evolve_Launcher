package com.veticia.piLauncherNext.ui;

import static com.veticia.piLauncherNext.MainActivity.DEFAULT_SCALE;
import static com.veticia.piLauncherNext.MainActivity.DEFAULT_STYLE;
import static com.veticia.piLauncherNext.MainActivity.STYLES;
import static com.veticia.piLauncherNext.MainActivity.mPreferences;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.veticia.piLauncherNext.ImageUtils;
import com.veticia.piLauncherNext.MainActivity;
import com.veticia.piLauncherNext.R;
import com.veticia.piLauncherNext.SettingsProvider;
import com.veticia.piLauncherNext.platforms.AbstractPlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends BaseAdapter
{
    int style = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_STYLE, DEFAULT_STYLE);
    private final MainActivity mContext;
    private final List<ApplicationInfo> mInstalledApps;
    private final boolean isEditMode;
    private final boolean showTextLabels;
    private final int mScale;
    private final SettingsProvider mSettings;

    private static Drawable mTempIcon;
    private static File mTempFile;
    private static String mTempPackage;
    private static long mTempTimestamp;

    public AppsAdapter(MainActivity context, boolean editMode, int scale, boolean names)
    {
        mContext = context;
        isEditMode = editMode;
        showTextLabels = names;
        mScale = scale;
        mSettings = SettingsProvider.getInstance(mContext);

        ArrayList<String> groups = mSettings.getAppGroupsSorted(false);
        ArrayList<String> selected = mSettings.getAppGroupsSorted(true);
        boolean first = !selected.isEmpty() && !groups.isEmpty() && selected.get(0).compareTo(groups.get(0)) == 0;
        mInstalledApps = mSettings.getInstalledApps(context, selected, first);
    }

    private static class ViewHolder {
        RelativeLayout layout;
        ImageView imageView;
        TextView textView;
        ImageView progressBar;
    }

    public int getCount()
    {
        return mInstalledApps.size();
    }

    public Object getItem(int position)
    {
        return mInstalledApps.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    @SuppressLint("NewApi")
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;

        final ApplicationInfo actApp = mInstalledApps.get(position);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            // Create a new ViewHolder and inflate the layout
            convertView = inflater.inflate(R.layout.lv_app, parent, false);
            holder = new ViewHolder();
            holder.layout = convertView.findViewById(R.id.layout);
            holder.imageView = convertView.findViewById(R.id.imageLabel);
            holder.textView = convertView.findViewById(R.id.textLabel);
            holder.progressBar = convertView.findViewById(R.id.progress_bar);
            convertView.setTag(holder);
        } else {
            // ViewHolder already exists, reuse it
            holder = (ViewHolder) convertView.getTag();
        }

        // Set size of items
        ViewGroup.LayoutParams params = holder.layout.getLayoutParams();

        params.width = mScale;
        if (style == 0) {
            if(showTextLabels) {
                params.height = (int) (mScale * 0.8);
            }else{
                params.height = (int) (mScale * 0.6525);
            }
        } else {
            if(showTextLabels) {
                params.height = (int) (mScale * 1.18);
            }else{
                params.height = mScale;
            }
        }
        holder.layout.setLayoutParams(params);

        // set value into textview
        PackageManager pm = mContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mContext, actApp.packageName, actApp.loadLabel(pm));
        holder.textView.setText(name);
        int kScale = mPreferences.getInt(SettingsProvider.KEY_CUSTOM_SCALE, DEFAULT_SCALE) + 1;
        float textSize = holder.textView.getTextSize();
        holder.textView.setTextSize(Math.max(10, textSize / 5 * kScale));
        holder.textView.setVisibility(showTextLabels ? View.VISIBLE : View.GONE);

        if (isEditMode) {
            // short click for app details, long click to activate drag and drop
            holder.layout.setOnTouchListener((view, motionEvent) -> {
                if ((motionEvent.getAction() == MotionEvent.ACTION_DOWN) ||
                        (motionEvent.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {
                    mTempPackage = actApp.packageName;
                    mTempTimestamp = System.currentTimeMillis();
                    ClipData data = ClipData.newPlainText(name, name);
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        view.startDragAndDrop(data, shadowBuilder, view, 0);
                    } else {
                        view.startDrag(data, shadowBuilder, view, 0);
                    }
                }
                return false;
            });

            // drag and drop
            holder.layout.setOnDragListener((view, event) -> {
                if (actApp.packageName.compareTo(mTempPackage) == 0) {
                    if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                        view.setVisibility(View.INVISIBLE);
                    } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                        mContext.reloadUI();
                    } else if (event.getAction() == DragEvent.ACTION_DROP) {
                        if (System.currentTimeMillis() - mTempTimestamp < 250) {
                            showAppDetails(actApp);
                        } else {
                            mContext.reloadUI();
                        }
                    }
                    return event.getAction() != DragEvent.ACTION_DROP;
                }
                return true;
            });
        } else {
            holder.layout.setOnClickListener(view -> {
                holder.progressBar.setVisibility(View.VISIBLE);
                RotateAnimation rotateAnimation = new RotateAnimation(
                        0, 360,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                rotateAnimation.setDuration(1000);
                rotateAnimation.setRepeatCount(Animation.INFINITE);
                rotateAnimation.setInterpolator(new LinearInterpolator());
                holder.progressBar.startAnimation(rotateAnimation);
                if(!mContext.openApp(actApp)) {
                    holder.progressBar.setVisibility(View.GONE);
                    holder.progressBar.clearAnimation();
                }
            });
            holder.layout.setOnLongClickListener(view -> {
                showAppDetails(actApp);
                return false;
            });
        }

        // set application icon
        AbstractPlatform platform = AbstractPlatform.getPlatform(actApp);
        try {
            platform.loadIcon(mContext, holder.imageView, actApp, name);
        } catch (Resources.NotFoundException e) {
            Log.e("loadIcon", "Error loading icon for app: " + actApp.packageName, e);
        }
        return convertView;
    }

    public void onImageSelected(String path, ImageView selectedImageView) {
        AbstractPlatform.clearIconCache();
        if (path != null) {
            Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(path), 450);
            ImageUtils.saveBitmap(bitmap, mTempFile);
            selectedImageView.setImageBitmap(bitmap);
        } else {
            selectedImageView.setImageDrawable(mTempIcon);
            AbstractPlatform.updateIcon(selectedImageView, mTempFile, STYLES[style]+"."+mTempPackage);
        }
        mContext.reloadUI();
        this.notifyDataSetChanged(); // for real time updates
    }

    private void showAppDetails(ApplicationInfo actApp) {

        //set layout
        Context context = mContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(R.layout.dialog_app_details);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        dialog.show();

        //info action
        dialog.findViewById(R.id.info).setOnClickListener(view13 -> mContext.openAppDetails(actApp.packageName));

        //set name
        PackageManager pm = mContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mContext, actApp.packageName, actApp.loadLabel(pm));
        final EditText input = dialog.findViewById(R.id.app_name);
        input.setText(name);
        dialog.findViewById(R.id.ok).setOnClickListener(view12 -> {
            mSettings.setAppDisplayName(context, actApp, input.getText().toString());
            mContext.reloadUI();
            dialog.dismiss();
        });

        // load icon
        ImageView tempImage = dialog.findViewById(R.id.app_icon);
        AbstractPlatform platform = AbstractPlatform.getPlatform(actApp);
        platform.loadIcon(mContext, tempImage, actApp, name);

        tempImage.setOnClickListener(view1 -> {
            mTempIcon = actApp.loadIcon(pm);
            mTempPackage = actApp.packageName;
            mTempFile = AbstractPlatform.pkg2path(mContext, STYLES[style]+"."+actApp.packageName);
            if (mTempFile.exists()) {
                mTempFile.delete();
            }
            mContext.setSelectedImageView(tempImage);
            ImageUtils.showImagePicker(mContext, MainActivity.PICK_ICON_CODE);
        });
    }

    public String getSelectedPackage() {
        return mTempPackage;
    }
}
