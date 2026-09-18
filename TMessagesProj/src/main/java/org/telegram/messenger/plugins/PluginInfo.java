package org.telegram.messenger.plugins;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PluginInfo {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1,64}$");
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 512;
    private static final int MAX_AUTHOR_LENGTH = 64;
    private static final int MAX_VERSION_LENGTH = 32;

    public String id;
    public String name;
    public String description;
    public String version;
    public String author;
    public String iconPath;
    public String entryPoint;
    public String[] permissions;
    public String[] locales;
    public boolean enabled;
    public File pluginDir;
    public File iconFile;
    private JSONObject manifest;

    public PluginInfo() {}

    public static PluginInfo fromManifest(JSONObject manifest, File pluginDir) throws Exception {
        String id = manifest.optString("id", "unknown").trim();
        if (id.isEmpty() || !ID_PATTERN.matcher(id).matches()) {
            throw new Exception("Invalid plugin id: " + id);
        }

        PluginInfo info = new PluginInfo();
        info.id = id;
        info.name = clip(manifest.optString("name", info.id), MAX_NAME_LENGTH);
        info.description = clip(manifest.optString("description", ""), MAX_DESCRIPTION_LENGTH);
        info.version = clip(manifest.optString("version", "1.0.0"), MAX_VERSION_LENGTH);
        info.author = clip(manifest.optString("author", "Unknown"), MAX_AUTHOR_LENGTH);
        info.entryPoint = validateEntryPoint(manifest.optString("entry", "index.js"));

        info.permissions = readStringArray(manifest, "permissions", true);
        info.locales = readStringArray(manifest, "locales", false);

        info.iconPath = manifest.optString("icon", "icon.png");
        info.pluginDir = pluginDir;
        info.iconFile = new File(pluginDir, new File(info.iconPath).getName());
        info.manifest = manifest;
        return info;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String validateEntryPoint(String entry) throws Exception {
        String value = entry == null ? "index.js" : entry.trim();
        if (value.isEmpty()) {
            value = "index.js";
        }
        String name = new File(value).getName();
        if (!name.endsWith(".js") || name.contains("..")) {
            throw new Exception("Invalid entry point: " + value);
        }
        return name;
    }

    private static String[] readStringArray(JSONObject manifest, String key, boolean filterPermissions) throws Exception {
        List<String> result = new ArrayList<>();
        if (manifest.has(key)) {
            JSONArray arr = manifest.getJSONArray(key);
            int max = Math.min(arr.length(), 32);
            for (int i = 0; i < max; i++) {
                String value = arr.getString(i);
                if (value == null || value.isEmpty()) {
                    continue;
                }
                if (filterPermissions) {
                    if (!PluginPermissions.isKnown(value)) {
                        continue;
                    }
                }
                if (value.length() <= 64 && !result.contains(value)) {
                    result.add(value);
                }
            }
        }
        return result.toArray(new String[0]);
    }

    public JSONObject getManifest() {
        return manifest;
    }

    public boolean hasPermission(String perm) {
        if (permissions == null || perm == null) {
            return false;
        }
        for (String p : permissions) {
            if (perm.equals(p)) {
                return true;
            }
        }
        return false;
    }
}