package org.telegram.messenger.plugins;

import org.json.JSONObject;

public class PluginHookRequest {

    public final PluginHookType hookType;
    public final PluginInfo plugin;
    private final JSONObject data;
    private String mutatedText;
    private boolean blocked;
    private String blockedMessage;
    private boolean consumed;
    private boolean budgetExceeded;

    public PluginHookRequest(PluginHookType hookType, PluginInfo plugin, JSONObject data) {
        this.hookType = hookType;
        this.plugin = plugin;
        this.data = data != null ? data : new JSONObject();
    }

    public String getPluginId() {
        return plugin != null ? plugin.id : "";
    }

    public JSONObject getData() {
        return data;
    }

    public String getString(String key, String def) {
        return data.optString(key, def);
    }

    public long getLong(String key, long def) {
        return data.optLong(key, def);
    }

    public int getInt(String key, int def) {
        return data.optInt(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return data.optBoolean(key, def);
    }

    public void setMutatedText(String text) {
        this.mutatedText = text;
    }

    public String getMutatedText() {
        return mutatedText;
    }

    public boolean hasMutatedText() {
        return mutatedText != null;
    }

    public void setBlocked(boolean blocked, String message) {
        this.blocked = blocked;
        this.blockedMessage = message;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getBlockedMessage() {
        return blockedMessage;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setBudgetExceeded(boolean budgetExceeded) {
        this.budgetExceeded = budgetExceeded;
    }

    public boolean isBudgetExceeded() {
        return budgetExceeded;
    }

    public void reset() {
        mutatedText = null;
        blocked = false;
        blockedMessage = null;
        consumed = false;
        budgetExceeded = false;
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("hookType", hookType != null ? hookType.name() : "UNKNOWN");
            json.put("pluginId", getPluginId());
            json.put("data", data);
            if (mutatedText != null) {
                json.put("mutatedText", mutatedText);
            }
            if (blocked) {
                json.put("blocked", true);
                if (blockedMessage != null) {
                    json.put("blockedMessage", blockedMessage);
                }
            }
            if (consumed) {
                json.put("consumed", true);
            }
            if (budgetExceeded) {
                json.put("budgetExceeded", true);
            }
            return json;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}