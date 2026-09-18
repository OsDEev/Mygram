package org.telegram.messenger.plugins;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MYgramConfig;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PluginManager {

    private static volatile PluginManager instance;

    private final ConcurrentHashMap<String, PluginInfo> loadedPlugins = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PluginContext> contexts = new ConcurrentHashMap<>();
    private final Set<String> activePermissions = ConcurrentHashMap.newKeySet();
    private File pluginsDir;

    private PluginManager() {}

    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        PluginRuntime.ensureFactory();
        pluginsDir = PluginLoader.getPluginsDir(context);
        activePermissions.clear();
        activePermissions.add(PluginPermissions.LIFECYCLE);
        activePermissions.add(PluginPermissions.READ_MESSAGES);
        if (MYgramConfig.isModifyMessagesEnabled()) {
            activePermissions.add(PluginPermissions.MODIFY_OUTGOING_MESSAGES);
        }
        loadAllPlugins();
        if (MYgramConfig.isPluginsEnabled()) {
            triggerHookTyped(PluginHookType.ON_STARTUP, null);
        }
    }

    public void loadAllPlugins() {
        for (PluginContext ctx : contexts.values()) {
            ctx.dispose();
        }
        contexts.clear();
        loadedPlugins.clear();
        if (pluginsDir == null || !pluginsDir.exists()) {
            return;
        }
        File[] dirs = pluginsDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return;
        }
        for (File dir : dirs) {
            try {
                PluginInfo info = PluginLoader.loadManifest(dir);
                info.enabled = isPluginEnabled(info.id);
                loadedPlugins.put(info.id, info);
            } catch (Exception e) {
                FileLog.e("PluginManager: failed to load plugin from " + dir.getName() + ": " + e.getMessage());
            }
        }
        refreshPermissionGrants();
    }

    private void refreshPermissionGrants() {
        boolean network = false;
        boolean storage = false;
        boolean ui = false;
        for (PluginInfo info : loadedPlugins.values()) {
            if (info.enabled && info.hasPermission(PluginPermissions.NETWORK)) {
                network = true;
            }
            if (info.enabled && info.hasPermission(PluginPermissions.STORAGE)) {
                storage = true;
            }
            if (info.enabled && info.hasPermission(PluginPermissions.UI)) {
                ui = true;
            }
        }
        if (network) {
            activePermissions.add(PluginPermissions.NETWORK);
        } else {
            activePermissions.remove(PluginPermissions.NETWORK);
        }
        if (storage) {
            activePermissions.add(PluginPermissions.STORAGE);
        } else {
            activePermissions.remove(PluginPermissions.STORAGE);
        }
        if (ui) {
            activePermissions.add(PluginPermissions.UI);
        } else {
            activePermissions.remove(PluginPermissions.UI);
        }
    }

    public boolean installPlugin(Context context, File mypFile) {
        try (FileInputStream fis = new FileInputStream(mypFile)) {
            return installPluginFromStream(fis, mypFile.getName());
        } catch (Exception e) {
            FileLog.e("PluginManager: install failed: " + e.getMessage());
            return false;
        }
    }

    public boolean installPluginFromStream(java.io.InputStream inputStream, String fileName) {
        try {
            String name = fileName;
            if (name.endsWith(".myp")) {
                name = name.substring(0, name.length() - 4);
            }
            int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
            String pluginId = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");

            PluginLoader.extractPlugin(inputStream, pluginId, ApplicationLoader.applicationContext);

            File pluginDir = new File(pluginsDir, pluginId);
            PluginInfo info = PluginLoader.loadManifest(pluginDir);
            File actualDir = new File(pluginsDir, info.id);
            if (!actualDir.exists() || !actualDir.getCanonicalPath().equals(pluginDir.getCanonicalPath())) {
                actualDir = pluginDir;
            }
            info = PluginLoader.loadManifest(actualDir);
            info.pluginDir = actualDir;

            PluginContext oldContext = contexts.get(info.id);
            if (oldContext != null) {
                oldContext.dispose();
            }
            contexts.remove(info.id);

            info.enabled = true;
            loadedPlugins.put(info.id, info);
            setPluginEnabled(info.id, true);

            FileLog.d("PluginManager: installed plugin " + info.name + " v" + info.version);
            return true;
        } catch (Exception e) {
            FileLog.e("PluginManager: install failed: " + e.getMessage());
            return false;
        }
    }

    public boolean uninstallPlugin(String pluginId) {
        PluginInfo info = loadedPlugins.remove(pluginId);
        PluginContext ctx = contexts.remove(pluginId);
        if (ctx != null) {
            ctx.dispose();
        }
        PluginUiOverlay.closeForPlugin(pluginId);
        if (info == null) {
            return false;
        }
        File pluginDir = info.pluginDir != null ? info.pluginDir : new File(pluginsDir, pluginId);
        if (pluginDir.exists()) {
            PluginLoader.deleteRecursive(pluginDir);
        }
        setPluginEnabled(pluginId, false);
        refreshPermissionGrants();
        FileLog.d("PluginManager: uninstalled plugin " + pluginId);
        return true;
    }

    public void setPluginEnabled(String pluginId, boolean enabled) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("MYGRAM_PLUGINS", 0);
        prefs.edit().putBoolean(pluginId, enabled).apply();
        PluginInfo info = loadedPlugins.get(pluginId);
        if (info != null) {
            info.enabled = enabled;
        }
        if (!enabled) {
            PluginContext ctx = contexts.remove(pluginId);
            if (ctx != null) {
                ctx.dispose();
            }
        }
        refreshPermissionGrants();
    }

    public boolean isPluginEnabled(String pluginId) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("MYGRAM_PLUGINS", 0);
        return prefs.getBoolean(pluginId, false);
    }

    public void reloadPlugin(String pluginId) {
        PluginContext ctx = contexts.remove(pluginId);
        if (ctx != null) {
            ctx.dispose();
        }
        PluginInfo info = loadedPlugins.get(pluginId);
        if (info != null) {
            info.enabled = isPluginEnabled(info.id);
        }
    }

    public List<PluginInfo> getInstalledPlugins() {
        return new ArrayList<>(loadedPlugins.values());
    }

    public List<PluginInfo> getEnabledPlugins() {
        List<PluginInfo> result = new ArrayList<>();
        for (PluginInfo info : loadedPlugins.values()) {
            if (info.enabled) {
                result.add(info);
            }
        }
        result.sort(Comparator.comparing(a -> a.id));
        return result;
    }

    public PluginInfo getPlugin(String pluginId) {
        return loadedPlugins.get(pluginId);
    }

    public void grantPermission(String permission) {
        activePermissions.add(permission);
    }

    public void revokePermission(String permission) {
        activePermissions.remove(permission);
    }

    public boolean hasPermission(String permission) {
        return activePermissions.contains(permission);
    }

    public PluginHookResult triggerHook(PluginHookType hookType, JSONObject data) {
        if (!MYgramConfig.isPluginsEnabled()) {
            return PluginHookResult.allow();
        }

        PluginHookResult finalResult = PluginHookResult.allow();
        for (PluginInfo plugin : getEnabledPlugins()) {
            if (!canHook(plugin, hookType)) {
                continue;
            }
            try {
                JSONObject hookData = data != null ? new JSONObject(data.toString()) : new JSONObject();
                hookData.put("_hookType", hookType.name());
                hookData.put("_pluginId", plugin.id);

                PluginContext context = getContext(plugin);
                Object raw = context.callHook(hookType.jsFunctionName, hookData);

                PluginHookRequest request = new PluginHookRequest(hookType, plugin, hookData);
                PluginLoader.processResult(request, raw);

                if (request.isBlocked()) {
                    return PluginHookResult.block(request.getBlockedMessage());
                }
                if (request.hasMutatedText()) {
                    finalResult = PluginHookResult.mutate(request.getMutatedText());
                }
            } catch (Exception e) {
                FileLog.e("PluginManager: hook execution error in " + plugin.id + ": " + e.getMessage());
            }
        }
        return finalResult;
    }

    private boolean canHook(PluginInfo plugin, PluginHookType hookType) {
        if (!plugin.enabled) {
            return false;
        }
        if (hookType.requiresPermission()) {
            if (!plugin.hasPermission(hookType.requiredPermission)) {
                return false;
            }
            return activePermissions.contains(hookType.requiredPermission);
        }
        return true;
    }

    private PluginContext getContext(PluginInfo plugin) {
        PluginContext context = contexts.compute(plugin.id, (id, old) ->
                old == null || old.isDisposed() ? new PluginContext(plugin, this) : old);
        return context;
    }

    private void triggerHookTyped(PluginHookType hookType, JSONObject data) {
        if (hookType.async) {
            triggerHookAsync(hookType, data);
        } else {
            triggerHook(hookType, data);
        }
    }

    public void triggerHookAsync(final PluginHookType hookType, final JSONObject data) {
        if (!MYgramConfig.isPluginsEnabled()) {
            return;
        }
        PluginRuntime.HOOK_EXECUTOR.submit(() -> {
            try {
                triggerHook(hookType, data);
            } catch (Exception e) {
                FileLog.e("PluginManager: async hook " + hookType.name() + " failed: " + e.getMessage());
            }
        });
    }

    public String triggerMessageHook(String text, long chatId, boolean isOutgoing) {
        if (!MYgramConfig.isPluginsEnabled() || text == null) {
            return text;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("text", text);
            data.put("chatId", chatId);
            data.put("isOutgoing", isOutgoing);

            PluginHookType hookType = isOutgoing ? PluginHookType.ON_MESSAGE_SEND : PluginHookType.ON_MESSAGE_RECEIVE;
            PluginHookResult result = triggerHook(hookType, data);

            if (result.isBlocked()) {
                return null;
            }
            if (result.isMutated() && result.getMutatedText() != null) {
                return result.getMutatedText();
            }
            return text;
        } catch (Exception e) {
            FileLog.e("PluginManager: message hook error: " + e.getMessage());
            return text;
        }
    }

    public String triggerMessageReceived(String text, long chatId) {
        if (!MYgramConfig.isPluginsEnabled() || text == null) {
            return null;
        }
        return triggerMessageHook(text, chatId, false);
    }

    public String triggerOutgoingPrepared(String text, long chatId) {
        if (!MYgramConfig.isPluginsEnabled() || text == null) {
            return text;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("text", text);
            data.put("chatId", chatId);
            data.put("isOutgoing", true);

            PluginHookResult result = triggerHook(PluginHookType.ON_OUTGOING_PREPARED, data);

            if (result.isBlocked()) {
                return null;
            }
            if (result.isMutated() && result.getMutatedText() != null) {
                return result.getMutatedText();
            }
            return text;
        } catch (Exception e) {
            FileLog.e("PluginManager: outgoing prepared error: " + e.getMessage());
            return text;
        }
    }

    public void triggerSettingsOpen() {
        triggerHookTyped(PluginHookType.ON_SETTINGS_OPEN, null);
    }

    public void triggerChatOpen(long chatId) {
        try {
            JSONObject data = new JSONObject();
            data.put("chatId", chatId);
            triggerHookTyped(PluginHookType.ON_CHAT_OPEN, data);
        } catch (Exception e) {
            FileLog.e("PluginManager: onChatOpen error: " + e.getMessage());
        }
    }

    public void triggerInputChanged(String text, long chatId) {
        if (!MYgramConfig.isPluginsEnabled() || text == null) {
            return;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("text", text);
            data.put("chatId", chatId);
            data.put("isOutgoing", true);
            triggerHookTyped(PluginHookType.ON_INPUT_CHANGED, data);
        } catch (Exception e) {
            FileLog.e("PluginManager: onInputChanged error: " + e.getMessage());
        }
    }

    public void triggerStartup() {
        triggerHookTyped(PluginHookType.ON_STARTUP, null);
    }

    void logPlugin(String pluginId, String level, String message) {
        if (message != null && message.length() > 512) {
            message = message.substring(0, 512);
        }
        FileLog.d("Plugin[" + pluginId + "] " + level + ": " + message);
    }

    void notifyPlugin(String title, String text) {
        String full = (title != null && !title.isEmpty() ? title + ": " : "") + (text != null ? text : "");
        AndroidUtilities.runOnUIThread(() -> {
            try {
                android.widget.Toast.makeText(ApplicationLoader.applicationContext, full, android.widget.Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                FileLog.e("PluginManager: notify failed: " + e.getMessage());
            }
        });
    }

    String getPluginState(String pluginId) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("MYGRAM_PLUGIN_STATE", 0);
        return prefs.getString(pluginId, "{}");
    }

    void setPluginState(String pluginId, String json) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("MYGRAM_PLUGIN_STATE", 0);
        prefs.edit().putString(pluginId, json).apply();
    }

    boolean canUseNetworkPermission(String pluginId) {
        PluginInfo info = loadedPlugins.get(pluginId);
        if (info == null || !info.hasPermission(PluginPermissions.NETWORK)) {
            return false;
        }
        return activePermissions.contains(PluginPermissions.NETWORK);
    }

    boolean canUseUiPermission(String pluginId) {
        PluginInfo info = loadedPlugins.get(pluginId);
        if (info == null || !info.hasPermission(PluginPermissions.UI)) {
            return false;
        }
        return activePermissions.contains(PluginPermissions.UI);
    }

    int pluginUiNext(String pluginId) {
        return PluginUiOverlay.nextId();
    }

    void pluginUiOpen(String pluginId, int panelId, JSONObject options) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                PluginUiOverlay.open(ApplicationLoader.applicationContext, pluginId, panelId, options);
            } catch (Exception e) {
                FileLog.e("PluginManager: pluginUiOpen failed: " + e.getMessage());
            }
        });
    }

    void pluginUiUpdate(String pluginId, int panelId, JSONObject options) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                PluginUiOverlay.update(panelId, options);
            } catch (Exception e) {
                FileLog.e("PluginManager: pluginUiUpdate failed: " + e.getMessage());
            }
        });
    }

    void pluginUiClose(String pluginId, int panelId) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                PluginUiOverlay.close(panelId);
            } catch (Exception e) {
                FileLog.e("PluginManager: pluginUiClose failed: " + e.getMessage());
            }
        });
    }
}