package org.telegram.messenger.plugins;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PluginPermissions {

    public static final String MODIFY_OUTGOING_MESSAGES = "MODIFY_OUTGOING_MESSAGES";
    public static final String READ_MESSAGES = "READ_MESSAGES";
    public static final String LIFECYCLE = "LIFECYCLE";
    public static final String NETWORK = "NETWORK";
    public static final String STORAGE = "STORAGE";
    public static final String UI = "UI";

    private static final Set<String> KNOWN = new HashSet<>(Arrays.asList(
            MODIFY_OUTGOING_MESSAGES, READ_MESSAGES, LIFECYCLE, NETWORK, STORAGE, UI
    ));

    private PluginPermissions() {}

    public static boolean isKnown(String permission) {
        return permission != null && KNOWN.contains(permission);
    }
}