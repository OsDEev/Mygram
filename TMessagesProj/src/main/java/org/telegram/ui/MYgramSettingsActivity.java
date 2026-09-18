package org.telegram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MYgramConfig;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.plugins.PluginManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MYgramSettingsActivity extends BaseFragment {

    private static final int REQUEST_IMPORT_SETTINGS = 52;

    private static final int TYPE_SHADOW = 0;
    private static final int TYPE_SETTINGS = 1;
    private static final int TYPE_CHECK = 3;
    private static final int TYPE_INFO = 4;

    private static final int TAB_APPEARANCE = 0;
    private static final int TAB_PRIVACY = 1;
    private static final int TAB_FUNCTIONALITY = 2;
    private static final int TAB_MEDIA_AUDIO = 3;
    private static final int TAB_CHANNEL_FILTER = 4;
    private static final int TAB_ADVANCED = 5;
    private static final int TAB_ABOUT = 6;

    private static final int KEY_SHOW_ONLINE = 0;
    private static final int KEY_SHOW_VIEWS = 1;
    private static final int KEY_USE_SYSTEM_EMOJI = 2;
    private static final int KEY_BIG_EMOJI = 3;
    private static final int KEY_BUBBLE_RADIUS = 4;
    private static final int KEY_FONT_SIZE = 5;
    private static final int KEY_OLED = 6;
    private static final int KEY_BLUR_CHAT_LIST = 7;
    private static final int KEY_AUTOPLAY_GIFS = 8;
    private static final int KEY_AUTOPLAY_VIDEOS = 9;
    private static final int KEY_ARCHIVE_HIDDEN = 10;
    private static final int KEY_SORT_CONTACTS = 11;
    private static final int KEY_STREAM_MEDIA = 12;
    private static final int KEY_SKIP_SILENCE = 13;
    private static final int KEY_VOICE_SPEED = 14;
    private static final int KEY_URL_SANITIZER = 15;
    private static final int KEY_CHANNEL_FILTER = 16;
    private static final int KEY_CHANNEL_FILTER_WORDS = 17;
    private static final int KEY_CHANNEL_FILTER_INFO = 18;
    private static final int KEY_PLUGINS = 19;
    private static final int KEY_PLUGINS_INFO = 20;
    private static final int KEY_MATERIAL_YOU = 21;
    private static final int KEY_MATERIAL_YOU_INFO = 22;
    private static final int KEY_EXPORT = 23;
    private static final int KEY_IMPORT = 24;
    private static final int KEY_CLEAR_CACHE = 25;
    private static final int KEY_MYGRAM_VERSION = 26;
    private static final int KEY_TELEGRAM_VERSION = 27;
    private static final int KEY_ABOUT = 28;
    private static final int KEY_VOICE_EFFECTS = 29;
    private static final int KEY_PHOTO_VIEWER_BLUR = 30;
    private static final int KEY_RAISE_TO_LISTEN = 31;
    private static final int KEY_USE_NEW_BLUR = 32;

    private static final int[] BUBBLE_RADII = {9, 13, 17, 21};
    private static final int[] FONT_SIZES = {14, 16, 18, 20};
    private static final float[] VOICE_SPEEDS = {0.5f, 0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f, 2.5f, 3.0f};

    private static class RowItem {
        int type;
        int key;

        RowItem(int type, int key) {
            this.type = type;
            this.key = key;
        }
    }

    private final ArrayList<RowItem> rows = new ArrayList<>();
    private int selectedTab;

    private int eggTapCount;
    private long lastEggTapTime;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        return true;
    }

    private void buildRows() {
        rows.clear();
        switch (selectedTab) {
            case TAB_APPEARANCE: {
                addCheck(KEY_OLED);
                addCheck(KEY_BLUR_CHAT_LIST);
                addCheck(KEY_USE_SYSTEM_EMOJI);
                addCheck(KEY_BIG_EMOJI);
                addSettings(KEY_BUBBLE_RADIUS);
                addSettings(KEY_FONT_SIZE);
                addSettings(KEY_MATERIAL_YOU);
                rows.add(new RowItem(TYPE_INFO, KEY_MATERIAL_YOU_INFO));
                break;
            }
            case TAB_PRIVACY: {
                addCheck(KEY_SHOW_ONLINE);
                addCheck(KEY_SHOW_VIEWS);
                addCheck(KEY_URL_SANITIZER);
                break;
            }
            case TAB_FUNCTIONALITY: {
                addCheck(KEY_AUTOPLAY_GIFS);
                addCheck(KEY_AUTOPLAY_VIDEOS);
                addCheck(KEY_ARCHIVE_HIDDEN);
                addCheck(KEY_SORT_CONTACTS);
                addCheck(KEY_STREAM_MEDIA);
                break;
            }
            case TAB_MEDIA_AUDIO: {
                addCheck(KEY_SKIP_SILENCE);
                addSettings(KEY_VOICE_SPEED);
                addCheck(KEY_VOICE_EFFECTS);
                addCheck(KEY_PHOTO_VIEWER_BLUR);
                addCheck(KEY_USE_NEW_BLUR);
                addCheck(KEY_RAISE_TO_LISTEN);
                break;
            }
            case TAB_CHANNEL_FILTER: {
                addCheck(KEY_CHANNEL_FILTER);
                addSettings(KEY_CHANNEL_FILTER_WORDS);
                rows.add(new RowItem(TYPE_INFO, KEY_CHANNEL_FILTER_INFO));
                break;
            }
            case TAB_ADVANCED: {
                addSettings(KEY_PLUGINS);
                rows.add(new RowItem(TYPE_INFO, KEY_PLUGINS_INFO));
                addSettings(KEY_EXPORT);
                addSettings(KEY_IMPORT);
                addSettings(KEY_CLEAR_CACHE);
                break;
            }
            case TAB_ABOUT: {
                rows.add(new RowItem(TYPE_SETTINGS, KEY_MYGRAM_VERSION));
                rows.add(new RowItem(TYPE_SETTINGS, KEY_TELEGRAM_VERSION));
                rows.add(new RowItem(TYPE_SETTINGS, KEY_ABOUT));
                break;
            }
        }
        rows.add(new RowItem(TYPE_SHADOW, -1));
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void addCheck(int key) {
        rows.add(new RowItem(TYPE_CHECK, key));
    }

    private void addSettings(int key) {
        rows.add(new RowItem(TYPE_SETTINGS, key));
    }

    private int rowIndexOf(int key) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).key == key) {
                return i;
            }
        }
        return -1;
    }

    private void notifyRowChanged(int key) {
        int index = rowIndexOf(key);
        if (listAdapter != null && index >= 0) {
            listAdapter.notifyItemChanged(index);
        }
    }

    private void setChecked(View view, boolean checked) {
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(checked);
        }
    }

    private int nextValue(int[] values, int current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] >= current) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private float nextFloatValue(float[] values, float current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] > current + 0.001f) {
                return values[i];
            }
        }
        return values[0];
    }

    private String formatSpeed(float speed) {
        return String.format(java.util.Locale.US, "%.1fx", speed);
    }

    private String getMaterialYouLabel() {
        int level = MYgramConfig.getMaterialYouLevel();
        switch (level) {
            case 1: return "Level 1";
            case 2: return "Level 2";
            case 3: return "Level 3";
            default: return LocaleController.getString(R.string.NotificationsOff);
        }
    }

    private String getMaterialYouDescription() {
        int level = MYgramConfig.getMaterialYouLevel();
        switch (level) {
            case 1: return "Material Components theme (predefined palette)";
            case 2: return "Material 3 with static color scheme";
            case 3: return "Dynamic color \u2014 system wallpaper palette";
            default: return "Disabled (default Telegram theme)";
        }
    }

    private void setOled(boolean enabled) {
        MYgramConfig.set(MYgramConfig.OLED_BLACK, enabled);
        int[] keys = {
                Theme.key_windowBackgroundGray,
                Theme.key_windowBackgroundWhite,
                Theme.key_windowBackgroundGrayShadow,
                Theme.key_chat_wallpaper,
                Theme.key_actionBarDefault,
                Theme.key_actionBarDefaultArchived,
                Theme.key_chat_messagePanelBackground,
                Theme.key_chat_inBubble,
                Theme.key_chat_outBubble,
                Theme.key_chat_serviceBackground
        };
        if (enabled) {
            Theme.ThemeInfo night = Theme.getCurrentNightTheme();
            if (night != null && !Theme.isCurrentThemeDark() && night.isDark()) {
                Theme.applyTheme(night, false, true);
            }
            for (int key : keys) {
                Theme.setColor(key, 0xff000000, false);
            }
        } else {
            for (int key : keys) {
                Theme.setColor(key, 0, true);
            }
        }
    }

    private void setBubbleRadius(int radius) {
        SharedConfig.bubbleRadius = radius;
        MessagesController.getGlobalMainSettings().edit().putInt("bubbleRadius", radius).apply();
        Theme.createChatResources(ApplicationLoader.applicationContext, false);
    }

    private void setFontSize(int size) {
        SharedConfig.fontSize = size;
        SharedConfig.fontSizeIsDefault = false;
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Context.MODE_PRIVATE).edit().putInt("fons_size", size).apply();
        Theme.createCommonMessageResources();
    }

    private void exportSettings() {
        try {
            File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (dir == null) {
                dir = ApplicationLoader.applicationContext.getFilesDir();
            }
            File out = new File(dir, "mygram_settings_" + System.currentTimeMillis() + ".json");
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(MYgramConfig.exportToJson().toString().getBytes("UTF-8"));
            fos.close();
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramSettingsExported) + "\n" + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramPluginInstallFailed), Toast.LENGTH_SHORT).show();
        }
    }

    private void importSettings() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_IMPORT_SETTINGS);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMPORT_SETTINGS) {
            if (data == null || data.getData() == null || getParentActivity() == null) {
                return;
            }
            try {
                Uri uri = data.getData();
                InputStream is = getParentActivity().getContentResolver().openInputStream(uri);
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[8192];
                int len;
                while (is != null && (len = is.read(buf)) != -1) {
                    sb.append(new String(buf, 0, len, "UTF-8"));
                }
                if (is != null) is.close();
                JSONObject json = new JSONObject(sb.toString());
                MYgramConfig.putAll(json);
                buildRows();
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramSettingsImported), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                FileLog.e(e);
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramPluginInstallFailed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearCache() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.MYgramClearCache));
        builder.setMessage(LocaleController.getString(R.string.MYgramClearCacheConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
            runOnSubThread(() -> {
                try {
                    File cacheDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
                    deleteFiles(cacheDir);
                    File imageDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_IMAGE);
                    deleteFiles(imageDir);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    org.telegram.messenger.ImageLoader.getInstance().clearMemory();
                    Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramCacheCleared), Toast.LENGTH_SHORT).show();
                });
            });
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void deleteFiles(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteFiles(file);
            }
            file.delete();
        }
    }

    private void runOnSubThread(Runnable runnable) {
        org.telegram.messenger.Utilities.globalQueue.postRunnable(runnable);
    }

    private void editChannelFilterWords() {
        if (getParentActivity() == null) return;
        final EditText editText = new EditText(getParentActivity());
        editText.setText(MYgramConfig.getChannelFilterWords());
        editText.setSingleLine(false);
        editText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString(R.string.MYgramChannelFilterWords))
                .setMessage(LocaleController.getString(R.string.MYgramChannelFilterWordsHint))
                .setView(editText)
                .setPositiveButton(LocaleController.getString(R.string.OK), (d, which) -> {
                    MYgramConfig.setChannelFilterWords(editText.getText().toString());
                    notifyRowChanged(KEY_CHANNEL_FILTER_WORDS);
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .create();
        dialog.show();
    }

    private void showAboutDialog() {
        if (getParentActivity() == null) return;
        new android.app.AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString(R.string.MYgramAbout))
                .setMessage(LocaleController.getString(R.string.MYgramEasterEgg))
                .setPositiveButton(LocaleController.getString(R.string.OK), null)
                .show();
    }

    private void handleAndroidEasterEgg() {
        long now = System.currentTimeMillis();
        if (now - lastEggTapTime > 2500) {
            eggTapCount = 0;
        }
        lastEggTapTime = now;
        eggTapCount++;
        if (eggTapCount >= 7) {
            eggTapCount = 0;
            showEasterEggAnimation();
        }
    }

    private void showEasterEggAnimation() {
        if (getParentActivity() == null || fragmentView == null) return;
        final EasterEggView overlay = new EasterEggView(getParentActivity());
        ((android.widget.FrameLayout) fragmentView).addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramEasterToast), Toast.LENGTH_SHORT).show();
    }

    private static final class EasterEggView extends View {

        private final ValueAnimator animator;
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint eggPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] dotColors = {0xFFF44336, 0xFF4CAF50, 0xFF2196F3};
        private float progress;
        private boolean leaving;

        EasterEggView(Context context) {
            super(context);
            setBackgroundColor(0xE6000000);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(AndroidUtilities.dp(3));
            ringPaint.setColor(0x66FFFFFF);
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(AndroidUtilities.dp(44));
            textPaint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
            eggPaint.setColor(0xFFFFFFFF);
            eggPaint.setTextAlign(Paint.Align.CENTER);
            eggPaint.setTextSize(AndroidUtilities.dp(56));
            hintPaint.setColor(0x99FFFFFF);
            hintPaint.setTextAlign(Paint.Align.CENTER);
            hintPaint.setTextSize(AndroidUtilities.dp(14));
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(3000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                progress = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
            animator.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && !leaving) {
                leaving = true;
                animator.cancel();
                animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(300).withEndAction(() -> {
                    ViewGroup parent = (ViewGroup) getParent();
                    if (parent != null) {
                        parent.removeView(EasterEggView.this);
                    }
                }).start();
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            canvas.drawText("MYgram", cx, cy - AndroidUtilities.dp(120), textPaint);

            canvas.save();
            canvas.rotate(360f * progress, cx, cy - 12);
            canvas.drawCircle(cx, cy - 12, AndroidUtilities.dp(90), ringPaint);
            for (int i = 0; i < 3; i++) {
                double angle = 2.0 * Math.PI * (progress + (double) i / 3.0);
                float x = cx + (float) (Math.cos(angle) * AndroidUtilities.dp(90));
                float y = cy - 12 + (float) (Math.sin(angle) * AndroidUtilities.dp(90));
                ringDotPaint.setColor(dotColors[i]);
                canvas.drawCircle(x, y, AndroidUtilities.dp(9), ringDotPaint);
            }
            canvas.restore();

            canvas.drawOval(cx - AndroidUtilities.dp(32), cy - AndroidUtilities.dp(44), cx + AndroidUtilities.dp(32), cy + AndroidUtilities.dp(42), eggPaint);
            eggPaint.setColor(0xFF111111);
            canvas.drawCircle(cx - AndroidUtilities.dp(11), cy - AndroidUtilities.dp(6), AndroidUtilities.dp(3.5f), eggPaint);
            canvas.drawCircle(cx + AndroidUtilities.dp(11), cy - AndroidUtilities.dp(6), AndroidUtilities.dp(3.5f), eggPaint);
            canvas.drawArc(cx - AndroidUtilities.dp(8), cy + AndroidUtilities.dp(8), cx + AndroidUtilities.dp(8), cy + AndroidUtilities.dp(20), 0f, 180f, true, eggPaint);
            eggPaint.setColor(0xFFFFFFFF);

            canvas.drawText("v0.0.1", cx, cy + AndroidUtilities.dp(86), hintPaint);
            canvas.drawText(LocaleController.getString(R.string.MYgramEasterEggHint), cx, getHeight() - AndroidUtilities.dp(64), hintPaint);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.MYgramSettings));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        if (parentLayout != null && parentLayout.isRightLayout()) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_close);
        }

        selectedTab = TAB_APPEARANCE;
        listAdapter = new ListAdapter(context);
        buildRows();

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        FrameLayout tabContainer = new FrameLayout(context);
        tabContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        frameLayout.addView(tabContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.TOP | Gravity.LEFT));

        scrollSlidingTextTabStrip = new ScrollSlidingTextTabStrip(context);
        scrollSlidingTextTabStrip.setUseSameWidth(true);
        tabContainer.addView(scrollSlidingTextTabStrip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        frameLayout.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP | Gravity.LEFT, 0, 44, 0, 0));

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 45, 0, 0));
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            int key = position >= 0 && position < rows.size() ? rows.get(position).key : -1;
            if (key == KEY_SHOW_ONLINE) {
                MYgramConfig.toggle(MYgramConfig.SHOW_ONLINE_STATUS);
                setChecked(view, MYgramConfig.isShowOnlineStatus());
            } else if (key == KEY_SHOW_VIEWS) {
                MYgramConfig.toggle(MYgramConfig.SHOW_CHANNEL_VIEWS);
                setChecked(view, MYgramConfig.isShowChannelViews());
            } else if (key == KEY_USE_SYSTEM_EMOJI) {
                boolean next = !MYgramConfig.isSystemEmoji();
                MYgramConfig.setUseSystemEmoji(next);
                setChecked(view, next);
            } else if (key == KEY_BIG_EMOJI) {
                SharedConfig.toggleBigEmoji();
                setChecked(view, SharedConfig.allowBigEmoji);
            } else if (key == KEY_BUBBLE_RADIUS) {
                setBubbleRadius(nextValue(BUBBLE_RADII, SharedConfig.bubbleRadius));
                notifyRowChanged(KEY_BUBBLE_RADIUS);
            } else if (key == KEY_FONT_SIZE) {
                setFontSize(nextValue(FONT_SIZES, SharedConfig.fontSize));
                notifyRowChanged(KEY_FONT_SIZE);
            } else if (key == KEY_OLED) {
                boolean next = !MYgramConfig.isOledBlack();
                setOled(next);
                setChecked(view, next);
            } else if (key == KEY_BLUR_CHAT_LIST) {
                MYgramConfig.toggle(MYgramConfig.BLUR_CHAT_LIST);
                setChecked(view, MYgramConfig.isBlurChatList());
                NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
            } else if (key == KEY_AUTOPLAY_GIFS) {
                SharedConfig.toggleAutoplayGifs();
                setChecked(view, SharedConfig.isAutoplayGifs());
            } else if (key == KEY_AUTOPLAY_VIDEOS) {
                SharedConfig.toggleAutoplayVideo();
                setChecked(view, SharedConfig.isAutoplayVideo());
            } else if (key == KEY_ARCHIVE_HIDDEN) {
                SharedConfig.toggleArchiveHidden();
                setChecked(view, SharedConfig.archiveHidden);
            } else if (key == KEY_SORT_CONTACTS) {
                SharedConfig.toggleSortContactsByName();
                setChecked(view, SharedConfig.sortContactsByName);
            } else if (key == KEY_STREAM_MEDIA) {
                SharedConfig.toggleStreamMedia();
                setChecked(view, SharedConfig.streamMedia);
            } else if (key == KEY_SKIP_SILENCE) {
                MYgramConfig.toggle(MYgramConfig.SKIP_SILENCE);
                setChecked(view, MYgramConfig.isSkipSilence());
            } else if (key == KEY_VOICE_SPEED) {
                float current = MediaController.getInstance().getPlaybackSpeed(false);
                MediaController.getInstance().setPlaybackSpeed(false, nextFloatValue(VOICE_SPEEDS, current));
                notifyRowChanged(KEY_VOICE_SPEED);
            } else if (key == KEY_VOICE_EFFECTS) {
                SharedConfig.toggleDisableVoiceAudioEffects();
                setChecked(view, SharedConfig.disableVoiceAudioEffects);
            } else if (key == KEY_PHOTO_VIEWER_BLUR) {
                SharedConfig.togglePhotoViewerBlur();
                setChecked(view, SharedConfig.photoViewerBlur);
            } else if (key == KEY_USE_NEW_BLUR) {
                SharedConfig.toggleUseNewBlur();
                setChecked(view, SharedConfig.useNewBlur);
            } else if (key == KEY_RAISE_TO_LISTEN) {
                SharedConfig.toggleRaiseToListen();
                setChecked(view, SharedConfig.raiseToListen);
            } else if (key == KEY_URL_SANITIZER) {
                MYgramConfig.toggle(MYgramConfig.URL_SANITIZER);
                setChecked(view, MYgramConfig.isUrlSanitizer());
            } else if (key == KEY_CHANNEL_FILTER) {
                MYgramConfig.toggle(MYgramConfig.CHANNEL_FILTER_ENABLED);
                setChecked(view, MYgramConfig.isChannelFilterEnabled());
            } else if (key == KEY_CHANNEL_FILTER_WORDS) {
                editChannelFilterWords();
            } else if (key == KEY_PLUGINS) {
                presentFragment(new PluginsActivity());
            } else if (key == KEY_MATERIAL_YOU) {
                int current = MYgramConfig.getMaterialYouLevel();
                int next = (current >= 3) ? 0 : current + 1;
                MYgramConfig.setMaterialYouLevel(next);
                notifyRowChanged(KEY_MATERIAL_YOU);
                notifyRowChanged(KEY_MATERIAL_YOU_INFO);
            } else if (key == KEY_EXPORT) {
                exportSettings();
            } else if (key == KEY_IMPORT) {
                importSettings();
            } else if (key == KEY_CLEAR_CACHE) {
                clearCache();
            } else if (key == KEY_MYGRAM_VERSION) {
                handleAndroidEasterEgg();
            } else if (key == KEY_ABOUT) {
                showAboutDialog();
            }
        });

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);

        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
            @Override
            public void onPageSelected(int id, boolean forward) {
                if (id == selectedTab) {
                    return;
                }
                selectedTab = id;
                buildRows();
                listView.scrollToPosition(0);
            }

            @Override
            public void onPageScrolled(float progress) {
            }
        });
        scrollSlidingTextTabStrip.addTextTab(TAB_APPEARANCE, LocaleController.getString(R.string.MYgramAppearance));
        scrollSlidingTextTabStrip.addTextTab(TAB_PRIVACY, LocaleController.getString(R.string.MYgramPrivacy));
        scrollSlidingTextTabStrip.addTextTab(TAB_FUNCTIONALITY, LocaleController.getString(R.string.MYgramFunctionality));
        scrollSlidingTextTabStrip.addTextTab(TAB_MEDIA_AUDIO, LocaleController.getString(R.string.MYgramMediaAudio));
        scrollSlidingTextTabStrip.addTextTab(TAB_CHANNEL_FILTER, LocaleController.getString(R.string.MYgramChannelFilter));
        scrollSlidingTextTabStrip.addTextTab(TAB_ADVANCED, LocaleController.getString(R.string.MYgramAdvanced));
        scrollSlidingTextTabStrip.addTextTab(TAB_ABOUT, LocaleController.getString(R.string.MYgramAbout));
        scrollSlidingTextTabStrip.finishAddingTabs();
        scrollSlidingTextTabStrip.setInitialTabId(TAB_APPEARANCE);

        PluginManager.getInstance().triggerSettingsOpen();

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            RowItem row = rows.get(position);
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setCanDisable(false);
                    if (row.key == KEY_BUBBLE_RADIUS) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramBubbleRadius), String.valueOf(SharedConfig.bubbleRadius), false);
                    } else if (row.key == KEY_FONT_SIZE) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramFontSize), String.valueOf(SharedConfig.fontSize), false);
                    } else if (row.key == KEY_VOICE_SPEED) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramVoiceSpeed), formatSpeed(MediaController.getInstance().getPlaybackSpeed(false)), false);
                    } else if (row.key == KEY_CHANNEL_FILTER_WORDS) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramChannelFilterWords), MYgramConfig.getChannelFilterWordsList().size() + " " + LocaleController.getString(R.string.MYgramChannelFilterWordsCount), false);
                    } else if (row.key == KEY_PLUGINS) {
                        textCell.setText(LocaleController.getString(R.string.MYgramPlugins), false);
                    } else if (row.key == KEY_MATERIAL_YOU) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramMaterial3), getMaterialYouLabel(), false);
                    } else if (row.key == KEY_EXPORT) {
                        textCell.setText(LocaleController.getString(R.string.MYgramExportSettings), false);
                    } else if (row.key == KEY_IMPORT) {
                        textCell.setText(LocaleController.getString(R.string.MYgramImportSettings), false);
                    } else if (row.key == KEY_CLEAR_CACHE) {
                        textCell.setText(LocaleController.getString(R.string.MYgramClearCache), true);
                    } else if (row.key == KEY_MYGRAM_VERSION) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramMygramVersion), BuildVars.MYGRAM_VERSION_STRING, false);
                    } else if (row.key == KEY_TELEGRAM_VERSION) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramTelegramVersion), BuildVars.BUILD_VERSION_STRING, false);
                    } else if (row.key == KEY_ABOUT) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramAbout), BuildVars.BUILD_VERSION_STRING, true);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (row.key == KEY_SHOW_ONLINE) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramShowOnlineStatus), MYgramConfig.isShowOnlineStatus(), false);
                    } else if (row.key == KEY_SHOW_VIEWS) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramShowChannelViews), MYgramConfig.isShowChannelViews(), false);
                    } else if (row.key == KEY_USE_SYSTEM_EMOJI) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramUseSystemEmoji), MYgramConfig.isSystemEmoji(), false);
                    } else if (row.key == KEY_BIG_EMOJI) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramBigEmoji), SharedConfig.allowBigEmoji, false);
                    } else if (row.key == KEY_OLED) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramOledBlack), MYgramConfig.isOledBlack(), true);
                    } else if (row.key == KEY_BLUR_CHAT_LIST) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramBlurChatList), MYgramConfig.isBlurChatList(), true);
                    } else if (row.key == KEY_AUTOPLAY_GIFS) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramAutoplayGifs), SharedConfig.isAutoplayGifs(), false);
                    } else if (row.key == KEY_AUTOPLAY_VIDEOS) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramAutoplayVideos), SharedConfig.isAutoplayVideo(), false);
                    } else if (row.key == KEY_ARCHIVE_HIDDEN) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramHideArchive), SharedConfig.archiveHidden, false);
                    } else if (row.key == KEY_SORT_CONTACTS) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramSortContacts), SharedConfig.sortContactsByName, false);
                    } else if (row.key == KEY_STREAM_MEDIA) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramStreamMedia), SharedConfig.streamMedia, true);
                    } else if (row.key == KEY_SKIP_SILENCE) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramSkipSilence), MYgramConfig.isSkipSilence(), false);
                    } else if (row.key == KEY_URL_SANITIZER) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramUrlSanitizer), MYgramConfig.isUrlSanitizer(), true);
                    } else if (row.key == KEY_CHANNEL_FILTER) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramChannelFilterEnabled), MYgramConfig.isChannelFilterEnabled(), true);
                    } else if (row.key == KEY_VOICE_EFFECTS) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramVoiceEffects), SharedConfig.disableVoiceAudioEffects, false);
                    } else if (row.key == KEY_PHOTO_VIEWER_BLUR) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramPhotoViewerBlur), SharedConfig.photoViewerBlur, false);
                    } else if (row.key == KEY_USE_NEW_BLUR) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramUseNewBlur), SharedConfig.useNewBlur, false);
                    } else if (row.key == KEY_RAISE_TO_LISTEN) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramRaiseToListen), SharedConfig.raiseToListen, true);
                    }
                    break;
                }
                case TYPE_INFO: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (row.key == KEY_PLUGINS_INFO) {
                        cell.setText(LocaleController.getString(R.string.MYgramPluginsInfo));
                    } else if (row.key == KEY_MATERIAL_YOU_INFO) {
                        cell.setText(getMaterialYouDescription() + ". " + LocaleController.getString(R.string.MYgramRestartRequired));
                    } else if (row.key == KEY_CHANNEL_FILTER_INFO) {
                        cell.setText(LocaleController.getString(R.string.MYgramChannelFilterHint));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= rows.size()) {
                return false;
            }
            int key = rows.get(position).key;
            if (rows.get(position).type == TYPE_CHECK || rows.get(position).type == TYPE_SETTINGS) {
                return key != KEY_TELEGRAM_VERSION;
            }
            return false;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext);
                    break;
                case TYPE_SETTINGS:
                    view = new TextSettingsCell(mContext);
                    break;
                case TYPE_CHECK:
                    view = new TextCheckCell(mContext);
                    break;
                case TYPE_INFO:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= rows.size()) {
                return TYPE_SHADOW;
            }
            return rows.get(position).type;
        }
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        listView.setPadding(0, 0, 0, bottom);
        listView.setClipToPadding(false);
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        return themeDescriptions;
    }
}