package org.telegram.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MYgramConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.plugins.PluginHookType;
import org.telegram.messenger.plugins.PluginInfo;
import org.telegram.messenger.plugins.PluginManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import androidx.recyclerview.widget.DefaultItemAnimator;

import java.io.File;
import java.util.ArrayList;

public class PluginInfoActivity extends BaseFragment {

    private final String pluginId;
    private PluginInfo plugin;
    private final PluginManager pluginManager = PluginManager.getInstance();

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int iconHeaderRow;
    private int infoHeaderRow;
    private int nameRow;
    private int descriptionRow;
    private int permissionsHeaderRow;
    private int permissionModifyRow;
    private int permissionReadRow;
    private int permissionLifecycleRow;
    private int permissionsInfoRow;
    private int behaviorHeaderRow;
    private int enabledRow;
    private int behaviorsInfoRow;
    private int dangerHeaderRow;
    private int uninstallRow;
    private int dangerInfoRow;
    private int rowCount;

    public PluginInfoActivity(String pluginId) {
        super();
        this.pluginId = pluginId;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        plugin = pluginManager.getPlugin(pluginId);
        updateRows();
        return plugin != null;
    }

    private void updateRows() {
        rowCount = 0;
        iconHeaderRow = rowCount++;

        infoHeaderRow = rowCount++;
        nameRow = rowCount++;
        descriptionRow = rowCount++;

        behaviorHeaderRow = rowCount++;
        enabledRow = rowCount++;
        behaviorsInfoRow = rowCount++;

        permissionsHeaderRow = rowCount++;
        permissionModifyRow = rowCount++;
        permissionReadRow = rowCount++;
        permissionLifecycleRow = rowCount++;
        permissionsInfoRow = rowCount++;

        dangerHeaderRow = rowCount++;
        uninstallRow = rowCount++;
        dangerInfoRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(plugin != null ? plugin.name : LocaleController.getString(R.string.MYgramPlugins));
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

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == enabledRow) {
                if (plugin == null) return;
                boolean enabled = !plugin.enabled;
                pluginManager.setPluginEnabled(plugin.id, enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (position == uninstallRow) {
                if (plugin == null) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(plugin.name);
                builder.setMessage(LocaleController.getString(R.string.MYgramUninstallPluginConfirm));
                builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                    pluginManager.uninstallPlugin(plugin.id);
                    finishFragment();
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                builder.show();
            }
        });

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);

        return fragmentView;
    }

    private Bitmap loadIcon() {
        if (plugin == null || plugin.iconFile == null) return null;
        try {
            File iconFile = plugin.iconFile;
            if (iconFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                int size = AndroidUtilities.dp(96);
                options.inSampleSize = 1;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(iconFile.getAbsolutePath(), options);
                if (bitmap != null) {
                    return Bitmap.createScaledBitmap(bitmap, size, size, false);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    break;
                }
                case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setCanDisable(false);
                    if (position == nameRow) {
                        if (plugin != null) {
                            String value = LocaleController.getString(R.string.MYgramPluginVersion) + " " + plugin.version;
                            if (plugin.author != null && !plugin.author.isEmpty()) {
                                value = value + " \u00b7 " + plugin.author;
                            }
                            textCell.setTextAndValue(LocaleController.getString(R.string.MYgramPluginName), value, true);
                        }
                    } else if (position == uninstallRow) {
                        textCell.setText(LocaleController.getString(R.string.MYgramUninstallPlugin), true);
                        textCell.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                    }
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == infoHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramPluginAbout));
                    } else if (position == behaviorHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramPluginBehavior));
                    } else if (position == permissionsHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramPermissionHooks));
                    } else if (position == dangerHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramPluginDanger));
                    }
                    break;
                }
                case 3: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == enabledRow) {
                        boolean enabled = plugin != null && plugin.enabled;
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramEnablePlugin), enabled, false);
                    } else if (position == permissionModifyRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramModifyMessages), plugin != null && plugin.hasPermission("MODIFY_OUTGOING_MESSAGES"), false);
                        checkCell.setEnabled(false);
                    } else if (position == permissionReadRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramReadMessages), plugin != null && plugin.hasPermission("READ_MESSAGES"), false);
                        checkCell.setEnabled(false);
                    } else if (position == permissionLifecycleRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramLifecycle), plugin != null && plugin.hasPermission("LIFECYCLE"), false);
                        checkCell.setEnabled(false);
                    }
                    break;
                }
                case 4: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == descriptionRow) {
                        String desc = plugin != null && plugin.description != null ? plugin.description : "";
                        cell.setText(desc);
                    } else if (position == behaviorsInfoRow) {
                        cell.setText(LocaleController.getString(R.string.MYgramPluginBehaviorInfo));
                    } else if (position == permissionsInfoRow) {
                        cell.setText(LocaleController.getString(R.string.MYgramPermissionHooksInfo));
                    } else if (position == dangerInfoRow) {
                        cell.setText(LocaleController.getString(R.string.MYgramPluginDangerInfo));
                    }
                    break;
                }
                case 6: {
                    View cell = holder.itemView;
                    ImageView imageView = cell.findViewById(R.id.plugin_icon_view);
                    if (imageView != null) {
                        Bitmap bitmap = loadIcon();
                        if (bitmap != null) {
                            imageView.setImageDrawable(new BitmapDrawable(mContext.getResources(), bitmap));
                        } else {
                            imageView.setImageDrawable(null);
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == enabledRow || position == uninstallRow;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 1:
                    view = new TextSettingsCell(mContext);
                    break;
                case 2:
                    view = new HeaderCell(mContext, 22);
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    break;
                case 4:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 6: {
                    FrameLayout frameLayout = new FrameLayout(mContext);
                    ImageView imageView = new ImageView(mContext);
                    imageView.setId(R.id.plugin_icon_view);
                    frameLayout.addView(imageView, LayoutHelper.createFrame(96, 96, Gravity.CENTER, 0, 16, 0, 16));
                    view = frameLayout;
                    break;
                }
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == iconHeaderRow) {
                return 6;
            } else if (position == infoHeaderRow || position == behaviorHeaderRow || position == permissionsHeaderRow || position == dangerHeaderRow) {
                return 2;
            } else if (position == enabledRow || position == permissionModifyRow || position == permissionReadRow || position == permissionLifecycleRow) {
                return 3;
            } else if (position == descriptionRow || position == behaviorsInfoRow || position == permissionsInfoRow || position == dangerInfoRow) {
                return 4;
            } else if (position == nameRow || position == uninstallRow) {
                return 1;
            }
            return 0;
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
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, HeaderCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        return themeDescriptions;
    }
}