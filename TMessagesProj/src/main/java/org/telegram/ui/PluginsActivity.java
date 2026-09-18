package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MYgramConfig;
import org.telegram.messenger.R;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PluginsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int pluginsHeaderRow;
    private int pluginsEnabledRow;
    private int pluginsInfoRow;
    private int permissionsHeaderRow;
    private int modifyMessagesRow;
    private int permissionsInfoRow;
    private int installedHeaderRow;
    private int installRow;
    private int emptyPluginsRow;
    private int pluginsListStartRow;
    private int rowCount;

    private final PluginManager pluginManager = PluginManager.getInstance();
    private final List<PluginInfo> plugins = new ArrayList<>();
    private boolean refreshing;

    private static final int REQUEST_INSTALL_PLUGIN = 51;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        refreshPlugins();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (plugins.size() != pluginManager.getInstalledPlugins().size()) {
            refreshPlugins();
        } else {
            for (int i = 0; i < plugins.size(); i++) {
                PluginInfo updated = pluginManager.getPlugin(plugins.get(i).id);
                if (updated == null || updated.enabled != plugins.get(i).enabled) {
                    refreshPlugins();
                    break;
                }
            }
        }
    }

    private void refreshPlugins() {
        if (refreshing) return;
        refreshing = true;
        plugins.clear();
        plugins.addAll(pluginManager.getInstalledPlugins());
        refreshing = false;
        updateRows();
    }

    private void updateRows() {
        rowCount = 0;
        pluginsHeaderRow = rowCount++;
        pluginsEnabledRow = rowCount++;
        pluginsInfoRow = rowCount++;

        permissionsHeaderRow = rowCount++;
        modifyMessagesRow = rowCount++;
        permissionsInfoRow = rowCount++;

        installedHeaderRow = rowCount++;
        installRow = rowCount++;

        if (plugins.isEmpty()) {
            emptyPluginsRow = rowCount++;
            pluginsListStartRow = -1;
        } else {
            pluginsListStartRow = rowCount;
            rowCount += plugins.size();
            emptyPluginsRow = -1;
        }

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.MYgramPlugins));
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
            if (position == pluginsEnabledRow) {
                boolean enabled = !MYgramConfig.isPluginsEnabled();
                MYgramConfig.set(MYgramConfig.PLUGINS_ENABLED, enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                listAdapter.notifyItemChanged(modifyMessagesRow);
            } else if (position == modifyMessagesRow) {
                boolean enabled;
                if (MYgramConfig.isModifyMessagesEnabled()) {
                    pluginManager.revokePermission("MODIFY_OUTGOING_MESSAGES");
                    MYgramConfig.set(MYgramConfig.MODIFY_OUTGOING_MESSAGES, false);
                    enabled = false;
                } else {
                    pluginManager.grantPermission("MODIFY_OUTGOING_MESSAGES");
                    MYgramConfig.set(MYgramConfig.MODIFY_OUTGOING_MESSAGES, true);
                    enabled = true;
                }
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (position == installRow) {
                choosePluginFile();
            } else if (position >= pluginsListStartRow && pluginsListStartRow >= 0) {
                int index = position - pluginsListStartRow;
                if (index >= 0 && index < plugins.size()) {
                    presentFragment(new PluginInfoActivity(plugins.get(index).id));
                }
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position >= pluginsListStartRow && pluginsListStartRow >= 0) {
                int index = position - pluginsListStartRow;
                if (index >= 0 && index < plugins.size()) {
                    confirmUninstall(plugins.get(index));
                    return true;
                }
            }
            return false;
        });

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);

        return fragmentView;
    }

    private void choosePluginFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/zip");
            if (Build.VERSION.SDK_INT >= 18) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            }
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_INSTALL_PLUGIN);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INSTALL_PLUGIN) {
            if (data == null || data.getData() == null || getParentActivity() == null) {
                return;
            }
            Uri uri = data.getData();
            try {
                String displayName = queryDisplayName(uri);
                if (displayName == null) {
                    displayName = "plugin";
                }
                InputStream is = getParentActivity().getContentResolver().openInputStream(uri);
                if (is != null) {
                    boolean ok = pluginManager.installPluginFromStream(is, displayName);
                    is.close();
                    if (ok) {
                        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramPluginInstalled), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramPluginInstallFailed), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.MYgramPluginInstallFailed), Toast.LENGTH_SHORT).show();
            }
            refreshPlugins();
        }
    }

    private String queryDisplayName(Uri uri) {
        if (getParentActivity() == null) return null;
        try (Cursor cursor = getParentActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private void confirmUninstall(PluginInfo plugin) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(plugin.name);
        builder.setMessage(LocaleController.getString(R.string.MYgramUninstallPluginConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
            pluginManager.uninstallPlugin(plugin.id);
            refreshPlugins();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
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
                    if (position == installRow) {
                        textCell.setIcon(0);
                        textCell.setTextAndValue(LocaleController.getString(R.string.MYgramInstallPlugin), "", true);
                    }
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == pluginsHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramPlugins));
                    } else if (position == permissionsHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramPermissionHooks));
                    } else if (position == installedHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.MYgramInstalledPlugins));
                    }
                    break;
                }
                case 3: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == pluginsEnabledRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramLoadPlugins), MYgramConfig.isPluginsEnabled(), true);
                    } else if (position == modifyMessagesRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.MYgramModifyMessages), MYgramConfig.isModifyMessagesEnabled(), false);
                        checkCell.setEnabled(MYgramConfig.isPluginsEnabled());
                        checkCell.setAlpha(MYgramConfig.isPluginsEnabled() ? 1.0f : 0.4f);
                    }
                    break;
                }
                case 4: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == pluginsInfoRow) {
                        cell.setText(LocaleController.getString(R.string.MYgramPluginsInfo));
                    } else if (position == permissionsInfoRow) {
                        cell.setText(LocaleController.getString(R.string.MYgramModifyMessagesInfo));
                    } else if (position == emptyPluginsRow) {
                        cell.setText(LocaleController.getString(R.string.MYgramNoPlugins));
                    }
                    break;
                }
                case 5: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setCanDisable(false);
                    int index = position - pluginsListStartRow;
                    if (index >= 0 && index < plugins.size()) {
                        PluginInfo plugin = plugins.get(index);
                        textCell.setTextAndValue(plugin.name, plugin.version, index != plugins.size() - 1);
                        textCell.setEnabled(plugin.enabled);
                        textCell.setAlpha(plugin.enabled ? 1.0f : 0.5f);
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == modifyMessagesRow) {
                return MYgramConfig.isPluginsEnabled();
            }
            if (position >= pluginsListStartRow && pluginsListStartRow >= 0) {
                int index = position - pluginsListStartRow;
                return index >= 0 && index < plugins.size() && plugins.get(index).enabled;
            }
            return position == pluginsEnabledRow || position == installRow;
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
                case 5:
                default:
                    view = new TextSettingsCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == pluginsHeaderRow || position == permissionsHeaderRow || position == installedHeaderRow) {
                return 2;
            } else if (position == pluginsEnabledRow || position == modifyMessagesRow) {
                return 3;
            } else if (position == pluginsInfoRow || position == permissionsInfoRow || position == emptyPluginsRow) {
                return 4;
            } else if (position == installRow) {
                return 1;
            } else if (position >= pluginsListStartRow && pluginsListStartRow >= 0) {
                return 5;
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
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextCheckCell.class, HeaderCell.class, TextSettingsCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
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