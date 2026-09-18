package org.telegram.messenger.plugins;

public enum PluginHookType {
    ON_MESSAGE_SEND("onSendMessage", PluginPermissions.MODIFY_OUTGOING_MESSAGES, false),
    ON_MESSAGE_RECEIVE("onReceiveMessage", PluginPermissions.READ_MESSAGES, false),
    ON_STARTUP("onStartup", PluginPermissions.LIFECYCLE, true),
    ON_SETTINGS_OPEN("onSettingsOpen", PluginPermissions.LIFECYCLE, true),
    ON_CHAT_OPEN("onChatOpen", PluginPermissions.LIFECYCLE, true),
    ON_OUTGOING_PREPARED("onOutgoingPrepared", PluginPermissions.MODIFY_OUTGOING_MESSAGES, false),
    ON_INPUT_CHANGED("onInputChanged", PluginPermissions.READ_MESSAGES, true);

    public final String jsFunctionName;
    public final String requiredPermission;
    public final boolean async;

    PluginHookType(String jsFunctionName, String requiredPermission, boolean async) {
        this.jsFunctionName = jsFunctionName;
        this.requiredPermission = requiredPermission;
        this.async = async;
    }

    public boolean requiresPermission() {
        return requiredPermission != null;
    }
}