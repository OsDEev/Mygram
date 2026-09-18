package org.telegram.messenger;

import android.content.SharedPreferences;

public class MYgramConfig {

    private static final String PREF_NAME = "MYGRAM_CONFIG";

    public static final String SHOW_CHANNEL_VIEWS = "mygram_show_channel_views";
    public static final String HIDE_PHONE_NUMBER = "mygram_hide_phone_number";
    public static final String SHOW_ONLINE_STATUS = "mygram_show_online_status";
    public static final String ENABLE_NEARBY = "mygram_enable_nearby";
    public static final String USE_SYSTEM_EMOJI = "mygram_use_system_emoji";
    public static final String MATERIAL_YOU = "mygram_material_you";
    public static final String MATERIAL_YOU_LEVEL = "mygram_material_you_level";
    public static final String PLUGINS_ENABLED = "mygram_plugins_enabled";
    public static final String MODIFY_OUTGOING_MESSAGES = "mygram_modify_messages";
    public static final String OLED_BLACK = "mygram_oled_black";
    public static final String COMPACT_MODE = "mygram_compact_mode";
    public static final String BLUR_CHAT_LIST = "mygram_blur_chat_list";
    public static final String CONFIRM_SEND_VOICE = "mygram_confirm_send_voice";
    public static final String SKIP_SILENCE = "mygram_skip_silence";
    public static final String URL_SANITIZER = "mygram_url_sanitizer";
    public static final String CHANNEL_FILTER_ENABLED = "mygram_channel_filter";
    public static final String CHANNEL_FILTER_WORDS = "mygram_channel_filter_words";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, 0);
    }

    public static boolean get(String key, boolean def) {
        return prefs().getBoolean(key, def);
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static int getInt(String key, int def) {
        return prefs().getInt(key, def);
    }

    public static void setInt(String key, int value) {
        prefs().edit().putInt(key, value).apply();
    }

    public static void toggle(String key) {
        set(key, !get(key, false));
    }

    public static boolean isShowChannelViews() {
        return get(SHOW_CHANNEL_VIEWS, true);
    }

    public static boolean isHidePhoneNumber() {
        return get(HIDE_PHONE_NUMBER, false);
    }

    public static boolean isShowOnlineStatus() {
        return get(SHOW_ONLINE_STATUS, true);
    }

    public static boolean isNearbyEnabled() {
        return get(ENABLE_NEARBY, false);
    }

    public static boolean isSystemEmoji() {
        return get(USE_SYSTEM_EMOJI, false);
    }

    public static boolean isMaterialYou() {
        return get(MATERIAL_YOU, false);
    }

    public static int getMaterialYouLevel() {
        return isMaterialYou() ? getInt(MATERIAL_YOU_LEVEL, 1) : 0;
    }

    public static void setMaterialYouLevel(int level) {
        setInt(MATERIAL_YOU_LEVEL, Math.max(0, Math.min(3, level)));
        set(MATERIAL_YOU, level > 0);
    }

    public static boolean isPluginsEnabled() {
        return get(PLUGINS_ENABLED, true);
    }

    public static boolean isModifyMessagesEnabled() {
        return get(MODIFY_OUTGOING_MESSAGES, true);
    }

    public static void setUseSystemEmoji(boolean enabled) {
        set(USE_SYSTEM_EMOJI, enabled);
        SharedConfig.useSystemEmoji = enabled;
        SharedConfig.getPreferences().edit().putBoolean("useSystemEmoji", enabled).apply();
    }

    public static boolean isOledBlack() {
        return get(OLED_BLACK, false);
    }

    public static boolean isCompactMode() {
        return get(COMPACT_MODE, false);
    }

    public static boolean isBlurChatList() {
        return get(BLUR_CHAT_LIST, false);
    }

    public static boolean isSkipSilence() {
        return get(SKIP_SILENCE, false);
    }

    public static boolean isUrlSanitizer() {
        return get(URL_SANITIZER, false);
    }

    public static boolean isChannelFilterEnabled() {
        return get(CHANNEL_FILTER_ENABLED, false);
    }

    public static String getChannelFilterWords() {
        return prefs().getString(CHANNEL_FILTER_WORDS, "");
    }

    public static void setChannelFilterWords(String words) {
        prefs().edit().putString(CHANNEL_FILTER_WORDS, words == null ? "" : words.trim()).apply();
    }

    public static java.util.List<String> getChannelFilterWordsList() {
        java.util.List<String> result = new java.util.ArrayList<>();
        String raw = getChannelFilterWords();
        if (raw == null || raw.isEmpty()) return result;
        String[] parts = raw.split("[,;\\n]");
        for (String part : parts) {
            String word = part.trim().toLowerCase();
            if (!word.isEmpty()) result.add(word);
        }
        return result;
    }

    public static boolean matchesChannelFilter(CharSequence text) {
        if (text == null || !isChannelFilterEnabled()) return false;
        String lower = text.toString().toLowerCase();
        for (String word : getChannelFilterWordsList()) {
            if (word.length() >= 3 && lower.indexOf(word) >= 0) return true;
        }
        return false;
    }

    public static java.util.Map<String, ?> getAll() {
        return prefs().getAll();
    }

    public static void putAll(org.json.JSONObject json) {
        if (json == null) return;
        SharedPreferences.Editor editor = prefs().edit();
        for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                Object value = json.opt(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Double) {
                    editor.putFloat(key, ((Double) value).floatValue());
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                }
            } catch (Exception ignored) {
            }
        }
        editor.apply();
    }

    public static org.json.JSONObject exportToJson() {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            for (java.util.Map.Entry<String, ?> entry : prefs().getAll().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof String || value instanceof Float) {
                    json.put(entry.getKey(), value);
                }
            }
        } catch (Exception ignored) {
        }
        return json;
    }
}
