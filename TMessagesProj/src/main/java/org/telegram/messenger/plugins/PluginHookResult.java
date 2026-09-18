package org.telegram.messenger.plugins;

public class PluginHookResult {

    public static final int ACTION_ALLOW = 0;
    public static final int ACTION_BLOCK = 1;
    public static final int ACTION_MUTATE = 2;

    private int action = ACTION_ALLOW;
    private String mutatedText;
    private String message;

    public PluginHookResult(int action) {
        this.action = action;
    }

    public static PluginHookResult allow() {
        return new PluginHookResult(ACTION_ALLOW);
    }

    public static PluginHookResult block(String message) {
        PluginHookResult result = new PluginHookResult(ACTION_BLOCK);
        result.message = message;
        return result;
    }

    public static PluginHookResult mutate(String text) {
        PluginHookResult result = new PluginHookResult(ACTION_MUTATE);
        result.mutatedText = text;
        return result;
    }

    public int getAction() {
        return action;
    }

    public String getMutatedText() {
        return mutatedText;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAllowed() {
        return action == ACTION_ALLOW;
    }

    public boolean isBlocked() {
        return action == ACTION_BLOCK;
    }

    public boolean isMutated() {
        return action == ACTION_MUTATE;
    }
}
