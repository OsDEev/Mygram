package org.telegram.messenger.plugins;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

final class PluginContext {

    private static final String BOOTSTRAP =
            "var __hookResult__ = { action: 'allow', text: null, message: null };\n"
                    + "function allow() { __hookResult__.action = 'allow'; __hookResult__.text = null; __hookResult__.message = null; }\n"
                    + "function block(msg) { __hookResult__.action = 'block'; __hookResult__.message = msg || ''; }\n"
                    + "function mutate(text) { __hookResult__.action = 'mutate'; __hookResult__.text = text; }\n"
                    + "function getData() { return pluginData; }\n";

    private static final int HTTP_TIMEOUT = 5000;
    private static final int HTTP_MAX_BODY = 64 * 1024;

    private final PluginInfo info;
    private final PluginManager manager;
    private final Object lock = new Object();

    private volatile Scriptable scope;
    private volatile boolean loaded;
    private volatile long entryModified;
    private volatile long entrySize;
    private volatile boolean disposed;

    PluginContext(PluginInfo info, PluginManager manager) {
        this.info = info;
        this.manager = manager;
    }

    Object callHook(String functionName, JSONObject data) {
        synchronized (lock) {
            if (disposed || !ensureLoaded()) {
                return null;
            }
            if (functionName == null || functionName.isEmpty()) {
                return null;
            }
            PluginRuntime.pushBudget(PluginRuntime.BUDGET_HOOK);
            Context cx = null;
            try {
                cx = Context.enter();
                configureContext(cx);
                cx.evaluateString(scope, "__hookResult__ = { action: 'allow', text: null, message: null };", "mygram_reset", 1, null);

                Object dataObj = cx.evaluateString(scope, "(" + data.toString() + ")", "mygram_data", 1, null);
                scope.put("pluginData", scope, dataObj);

                Object fn = scope.get(functionName, scope);
                if (!(fn instanceof Function)) {
                    return null;
                }

                Object returnValue = ((Function) fn).call(cx, scope, scope, new Object[]{});
                Map<String, Object> returnMap = toMap(returnValue);
                if (returnMap != null && returnMap.containsKey("action")) {
                    return returnMap;
                }

                Object resultObject = scope.get("__hookResult__", scope);
                return toMap(resultObject);
            } catch (PluginRuntime.BudgetExceededException e) {
                FileLog.e("PluginContext: budget exceeded in " + info.id + " hook '" + functionName + "'");
                return null;
            } catch (Exception e) {
                FileLog.e("PluginContext: hook '" + functionName + "' failed in " + info.id + ": " + e.getMessage());
                return null;
            } finally {
                if (cx != null) {
                    Context.exit();
                }
                PluginRuntime.popBudget();
            }
        }
    }

    private boolean ensureLoaded() {
        File entry = new File(info.pluginDir, info.entryPoint);
        if (!entry.exists()) {
            FileLog.e("PluginContext: entry missing for " + info.id + ": " + entry.getAbsolutePath());
            return false;
        }
        long modified = entry.lastModified();
        long size = entry.length();
        if (loaded && modified == entryModified && size == entrySize) {
            return true;
        }
        if (!buildScope(entry)) {
            return false;
        }
        entryModified = modified;
        entrySize = size;
        loaded = true;
        return true;
    }

    private boolean buildScope(File entry) {
        PluginRuntime.pushBudget(PluginRuntime.BUDGET_EVALUATE);
        Context cx = null;
        try {
            cx = Context.enter();
            configureContext(cx);

            Scriptable newScope = cx.initStandardObjects();
            scope = newScope;

            Object api = Context.javaToJS(new PluginApi(this), newScope);
            ScriptableObject.putProperty(newScope, "m", api);

            cx.evaluateString(newScope, BOOTSTRAP, "mygram_bootstrap", 1, null);

            File utilsDir = new File(info.pluginDir, "utils");
            File[] utils = utilsDir.listFiles((dir, name) -> name.endsWith(".js"));
            if (utils != null) {
                java.util.Arrays.sort(utils, Comparator.comparing(File::getName));
                for (File util : utils) {
                    cx.evaluateString(newScope, PluginLoader.readFileChecked(util, PluginRuntime.MAX_SCRIPT_SIZE), util.getName(), 1, null);
                }
            }

            File helper = new File(info.pluginDir, "helper.js");
            if (helper.exists()) {
                cx.evaluateString(newScope, PluginLoader.readFileChecked(helper, PluginRuntime.MAX_SCRIPT_SIZE), "helper.js", 1, null);
            }

            cx.evaluateString(newScope, PluginLoader.readFileChecked(entry, PluginRuntime.MAX_SCRIPT_SIZE), entry.getName(), 1, null);
            return true;
        } catch (PluginRuntime.BudgetExceededException e) {
            FileLog.e("PluginContext: load budget exceeded for " + info.id);
            dispose();
            return false;
        } catch (Exception e) {
            FileLog.e("PluginContext: failed to load plugin " + info.id + ": " + e.getMessage());
            dispose();
            return false;
        } finally {
            if (cx != null) {
                Context.exit();
            }
            PluginRuntime.popBudget();
        }
    }

    private static void configureContext(Context cx) {
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setLocale(Locale.ENGLISH);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object object) {
        if (object instanceof Scriptable) {
            try {
                return (Map<String, Object>) Context.jsToJava(object, Map.class);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    void dispose() {
        synchronized (lock) {
            disposed = true;
            scope = null;
            loaded = false;
        }
    }

    boolean isDisposed() {
        return disposed;
    }

    String getPluginId() {
        return info.id;
    }

    private void deliverHttp(Object callback, int status, String text) {
        synchronized (lock) {
            if (disposed) {
                return;
            }
            PluginRuntime.pushBudget(PluginRuntime.BUDGET_HTTP);
            Context cx = null;
            try {
                cx = Context.enter();
                configureContext(cx);
                if (callback instanceof Function) {
                    ((Function) callback).call(cx, scope, scope, new Object[]{status, text});
                }
            } catch (Exception e) {
                FileLog.e("PluginContext: http delivery failed in " + info.id + ": " + e.getMessage());
            } finally {
                if (cx != null) {
                    Context.exit();
                }
                PluginRuntime.popBudget();
            }
        }
    }

    private static Object[] doHttpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            if (protocol == null || (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol))) {
                return new Object[]{400, "unsupported protocol"};
            }
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HTTP_TIMEOUT);
            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "MYgram/1.0");
            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = "";
            if (is != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int len;
                int total = 0;
                while ((len = is.read(buf)) != -1) {
                    total += len;
                    if (total > HTTP_MAX_BODY) {
                        baos.write("...truncated".getBytes(StandardCharsets.UTF_8));
                        break;
                    }
                    baos.write(buf, 0, len);
                }
                is.close();
                body = baos.toString("UTF-8");
            }
            return new Object[]{status, body};
        } catch (Exception e) {
            return new Object[]{0, e.getMessage() != null ? e.getMessage() : "network error"};
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static final class PluginApi {

        private final PluginContext owner;

        PluginApi(PluginContext owner) {
            this.owner = owner;
        }

        public void log(Object msg) {
            owner.manager.logPlugin(owner.info.id, "log", String.valueOf(msg));
        }

        public void warn(Object msg) {
            owner.manager.logPlugin(owner.info.id, "warn", String.valueOf(msg));
        }

        public void error(Object msg) {
            owner.manager.logPlugin(owner.info.id, "error", String.valueOf(msg));
        }

        public Object notify(Object title, Object text) {
            owner.manager.notifyPlugin(title != null ? title.toString() : "", text != null ? text.toString() : "");
            return null;
        }

        public Object getState() {
            Context cx = Context.getCurrentContext();
            if (cx == null) {
                return null;
            }
            String json = owner.manager.getPluginState(owner.info.id);
            try {
                return cx.evaluateString(owner.scope, "(" + json + ")", "mygram_state", 1, null);
            } catch (Exception e) {
                try {
                    return cx.evaluateString(owner.scope, "({})", "mygram_state", 1, null);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }

        public Object setState(Object obj) {
            Context cx = Context.getCurrentContext();
            if (cx == null) {
                return null;
            }
            try {
                Object json = NativeJSON.stringify(cx, owner.scope, obj, null, null);
                owner.manager.setPluginState(owner.info.id, json != null ? json.toString() : "{}");
            } catch (Exception e) {
                FileLog.e("PluginContext: setState failed in " + owner.info.id + ": " + e.getMessage());
            }
            return null;
        }

        public Object httpGet(Object url, Object callback) {
            if (!owner.manager.canUseNetworkPermission(owner.info.id)) {
                owner.manager.logPlugin(owner.info.id, "warn", "httpGet blocked: NETWORK permission not declared or not granted");
                return null;
            }
            final String urlStr = String.valueOf(url);
            final Object cb = callback;
            PluginRuntime.NETWORK_EXECUTOR.submit(() -> {
                final Object[] result = doHttpGet(urlStr);
                final Object safeCb = cb;
                PluginRuntime.HOOK_EXECUTOR.submit(() -> {
                    if (safeCb != null) {
                        owner.deliverHttp(safeCb, (Integer) result[0], String.valueOf(result[1]));
                    }
                });
            });
            return null;
        }

        public Object uiOpen(Object options) {
            if (!owner.manager.canUseUiPermission(owner.info.id)) {
                owner.manager.logPlugin(owner.info.id, "warn", "ui.open blocked: UI permission not declared in manifest or not granted");
                return null;
            }
            try {
                JSONObject opts = toUiOptions(options);
                if (opts != null) {
                    int panelId = owner.manager.pluginUiNext(owner.info.id);
                    owner.manager.pluginUiOpen(owner.info.id, panelId, opts);
                    return panelId;
                }
            } catch (Exception e) {
                FileLog.e("PluginContext: ui.open failed in " + owner.info.id + ": " + e.getMessage());
            }
            return null;
        }

        public Object uiUpdate(Object panelId, Object options) {
            if (!owner.manager.canUseUiPermission(owner.info.id)) {
                return null;
            }
            Integer id = toInt(panelId);
            if (id == null) {
                return null;
            }
            try {
                JSONObject opts = toUiOptions(options);
                if (opts != null) {
                    owner.manager.pluginUiUpdate(owner.info.id, id, opts);
                }
            } catch (Exception e) {
                FileLog.e("PluginContext: ui.update failed in " + owner.info.id + ": " + e.getMessage());
            }
            return null;
        }

        public Object uiClose(Object panelId) {
            if (!owner.manager.canUseUiPermission(owner.info.id)) {
                return null;
            }
            Integer id = toInt(panelId);
            if (id == null) {
                return null;
            }
            owner.manager.pluginUiClose(owner.info.id, id);
            return null;
        }

        private JSONObject toUiOptions(Object options) throws Exception {
            if (options instanceof CharSequence) {
                JSONObject obj = new JSONObject();
                obj.put("html", options.toString());
                return obj;
            }
            Context cx = Context.getCurrentContext();
            if (cx == null) {
                return null;
            }
            Object json = NativeJSON.stringify(cx, owner.scope, options, null, null);
            if (json == null) {
                return null;
            }
            return new JSONObject(json.toString());
        }

        private Integer toInt(Object value) {
            if (value == null) {
                return null;
            }
            try {
                Object converted = Context.jsToJava(value, Integer.class);
                if (converted instanceof Integer) {
                    return (Integer) converted;
                }
            } catch (Exception ignored) {
            }
            try {
                double d = Context.toNumber(value);
                if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                    return (int) d;
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }
}