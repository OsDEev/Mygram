// =============================
//  MYgram Test Plugin — ALL permissions
//  Demonstrates every hook and every permission.
// =============================
//
// Permissions used:
//   MODIFY_OUTGOING_MESSAGES  -> onSendMessage, onOutgoingPrepared
//   READ_MESSAGES             -> onReceiveMessage, onInputChanged
//   LIFECYCLE                 -> onStartup, onSettingsOpen, onChatOpen
//
// Hook API (provided by MYgram):
//   getData()          return data for current hook
//   allow()            continue unmodified
//   block(reason)      cancel the operation
//   mutate(text)       change the text that will be sent
//

function onStartup() {
    log("[LIFECYCLE] onStartup fired — plugin loaded at app start");
}

function onSettingsOpen() {
    log("[LIFECYCLE] onSettingsOpen fired");
}

function onChatOpen() {
    var data = getData();
    log("[LIFECYCLE] onChatOpen fired, chatId=" + (data.chatId || "?"));
}

function onSendMessage() {
    var data = getData();
    var text = data.text || "";
    log("[MODIFY] onSendMessage fired, text='" + text + "', chatId=" + data.chatId);

    // Demonstrate mutate(): append a marker
    if (text.length > 0 && text.indexOf("[TEST-ALL]") < 0) {
        mutate(text + " [TEST-ALL]");
    }

    // Uncomment to demonstrate block():
    // if (text.indexOf("spam") >= 0) { block("blocked: message looks like spam"); }
}

function onOutgoingPrepared() {
    var data = getData();
    log("[MODIFY] onOutgoingPrepared fired for message " + (data.text || ""));
}

function onReceiveMessage() {
    var data = getData();
    log("[READ] onReceiveMessage fired, text='" + (data.text || "") + "', chatId=" + data.chatId);
}

function onInputChanged() {
    var data = getData();
    log("[READ] onInputChanged fired");
}

// Export hooks so the loader can find them explicitly.
if (typeof module !== "undefined" && module.exports) {
    module.exports = {
        onStartup: onStartup,
        onSettingsOpen: onSettingsOpen,
        onChatOpen: onChatOpen,
        onSendMessage: onSendMessage,
        onOutgoingPrepared: onOutgoingPrepared,
        onReceiveMessage: onReceiveMessage,
        onInputChanged: onInputChanged
    };
}