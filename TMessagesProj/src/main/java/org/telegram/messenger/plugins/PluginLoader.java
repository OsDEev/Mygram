package org.telegram.messenger.plugins;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginLoader {

    public static final int MAX_MANIFEST_SIZE = 256 * 1024;

    public static File getPluginsDir(Context context) {
        File dir = new File(context.getFilesDir(), "plugins");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static boolean extractPlugin(InputStream zipInputStream, String pluginId, Context context) throws Exception {
        File pluginsDir = getPluginsDir(context);
        File pluginDir = new File(pluginsDir, pluginId);
        if (pluginDir.exists()) {
            deleteRecursive(pluginDir);
        }
        if (!pluginDir.mkdirs() && !pluginDir.isDirectory()) {
            throw new Exception("Cannot create plugin dir " + pluginDir.getName());
        }

        String baseCanonical = pluginDir.getCanonicalPath() + File.separator;
        ZipInputStream zis = new ZipInputStream(zipInputStream);
        try {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            long totalBytes = 0;
            int entryCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                if (++entryCount > PluginRuntime.MAX_ARCHIVE_ENTRIES) {
                    throw new IOException("Too many entries in plugin archive");
                }

                String name = entry.getName();
                File target = resolveEntry(pluginDir, baseCanonical, name);

                if (entry.isDirectory()) {
                    if (!target.exists() && !target.mkdirs()) {
                        throw new IOException("Cannot create directory " + name);
                    }
                } else {
                    File parentDir = target.getParentFile();
                    if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("Cannot create directory for " + name);
                    }

                    long written = 0;
                    int len;
                    OutputStream fos = new FileOutputStream(target);
                    try {
                        while ((len = zis.read(buffer)) > 0) {
                            written += len;
                            if (written > PluginRuntime.MAX_ENTRY_SIZE) {
                                throw new IOException("Plugin entry too large: " + name);
                            }
                            fos.write(buffer, 0, len);
                        }
                    } finally {
                        fos.close();
                    }

                    totalBytes += written;
                    if (totalBytes > PluginRuntime.MAX_ARCHIVE_SIZE) {
                        throw new IOException("Plugin archive too large");
                    }
                }
                zis.closeEntry();
            }
        } finally {
            zis.close();
        }

        PluginInfo info = loadManifest(pluginDir);
        if (!pluginId.equals(info.id)) {
            String sanitizedId = info.id.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            File renamedDir = new File(pluginsDir, sanitizedId);
            if (!pluginId.equals(sanitizedId)) {
                if (pluginDir.renameTo(renamedDir)) {
                    pluginDir = renamedDir;
                }
            }
        }
        return true;
    }

    private static File resolveEntry(File pluginDir, String baseCanonical, String name) throws IOException {
        if (name == null || name.isEmpty()) {
            throw new IOException("Empty entry name");
        }
        if (name.contains("..")) {
            throw new IOException("Illegal path in plugin archive: " + name);
        }
        File target = new File(pluginDir, name);
        String targetCanonical = target.getCanonicalPath();
        if (!targetCanonical.startsWith(baseCanonical) && !targetCanonical.equals(pluginDir.getCanonicalPath())) {
            throw new IOException("Illegal path in plugin archive: " + name);
        }
        return target;
    }

    public static PluginInfo loadManifest(File pluginDir) throws Exception {
        File manifestFile = new File(pluginDir, "manifest.json");
        if (!manifestFile.exists()) {
            throw new Exception("manifest.json not found in " + pluginDir.getName());
        }
        String json = readFileChecked(manifestFile, MAX_MANIFEST_SIZE);
        JSONObject manifest = new JSONObject(json);
        return PluginInfo.fromManifest(manifest, pluginDir);
    }

    public static void processResult(PluginHookRequest request, Object rawResult) {
        request.reset();
        if (rawResult instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rawResult;
            String action = map.containsKey("action") ? String.valueOf(map.get("action")) : "allow";
            String text = map.containsKey("text") ? String.valueOf(map.get("text")) : null;
            String message = map.containsKey("message")
                    ? String.valueOf(map.get("message"))
                    : (map.containsKey("blockMessage") ? String.valueOf(map.get("blockMessage")) : null);

            if ("block".equals(action)) {
                request.setBlocked(true, message != null ? message : "");
            } else if ("mutate".equals(action)) {
                request.setMutatedText(text != null ? text : "");
            }
        }
    }

    static String readFileChecked(File file, int maxBytes) throws Exception {
        if (file.length() > maxBytes) {
            throw new Exception("File too large: " + file.getName());
        }
        try {
            StringBuilder sb = new StringBuilder((int) Math.min(file.length(), 65536));
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            char[] buf = new char[4096];
            int len;
            int total = 0;
            while ((len = reader.read(buf)) != -1) {
                total += len;
                if (total > maxBytes) {
                    reader.close();
                    throw new Exception("File too large: " + file.getName());
                }
                sb.append(buf, 0, len);
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("Cannot read " + file.getName(), e);
        }
    }

    static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}