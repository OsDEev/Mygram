// MYgram plugin entry point.
// Hooks must be regular top-level functions. The loader calls them by name.
// Available helpers:
//   getData()          -> object with hook data (text, chatId, isOutgoing, ...)
//   allow()            -> let the operation continue unmodified
//   block(message)     -> stop the operation (message = reason)
//   mutate(text)       -> replace the text before it is sent

// utils/helper.js is preloaded automatically before this file.

function onStartup() {
    // Called once when the app starts, if LIFECYCLE permission is granted and
    // "Auto-load plugins" is enabled.
    log("MYGRAM plugin started");
}

function onSendMessage() {
    var data = getData();
    var text = data.text || "";
    // Demo: append a tag to outgoing messages that contain "myp"
    if (text.toLowerCase().indexOf("myp") >= 0) {
        mutate(text + "\n[modified by MYGRAM plugin]");
    }
    // To refuse sending a message instead, use: block("blocked by plugin");
}

function onReceiveMessage() {
    // Called for incoming messages, if READ_MESSAGES permission is granted.
    var data = getData();
    // data.text, data.chatId, data.isOutgoing === false
}

function onSettingsOpen() {
    // Called when the MYgram settings screen is opened (LIFECYCLE permission).
    log("settings opened");
}