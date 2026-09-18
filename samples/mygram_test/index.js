// MYgram demo/test plugin.
// Magic commands in outgoing messages:
//   #test   -> append "[+MYgram test]" to the message
//   #upper  -> send the message in UPPERCASE
//   #block  -> block the message
// Incoming:
//   #mark   -> mark the received message with "[Y]"
//
// Also demonstrates m.ui panels, m.notify, m.httpGet and m.setState/m.getState.

var panelId = null;
var sendCount = 0;
var blockedCount = 0;
var lastInputAt = 0;
var lastInputText = "";

function stamp() {
    return new Date().toLocaleTimeString();
}

function panelHtml() {
    var status = 'running';
    return '<html><head><style>'
        + 'body{background:#0e0e12;color:#eee;font-family:sans-serif;padding:10px;margin:0}'
        + '.card{background:#1b1b22;border-radius:8px;padding:10px;margin-bottom:8px}'
        + 'b{color:#40c4ff}i{color:#ffd740}'
        + '</style></head><body>'
        + '<div class="card">Status: <b>' + status + '</b><br/>MYgram demo v1.0.0</div>'
        + '<div class="card">Sent: <b>' + sendCount + '</b><br/>Blocked: <b>' + blockedCount + '</b></div>'
        + '<div class="card">Last input: <i>' + (lastInputText ? lastInputText : '(empty)') + '</i><br/><small>updated ' + stamp() + '</small></div>'
        + '</body></html>';
}

function updatePanel() {
    if (panelId !== null) {
        m.ui.update(panelId, { html: panelHtml() });
    }
}

// LIFECYCLE hooks ---------------------------------------------------------
function onStartup() {
    m.log("demo: plugin started");
    if (m.ui) {
        panelId = m.ui.open({
            title: "MYgram Demo",
            html: panelHtml(),
            width: 320,
            height: 250,
            x: 10,
            y: 10,
            visible: true
        });
        m.log("demo: panel opened id=" + panelId);
    }
    m.httpGet("https://httpbin.org/get", function (status, text) {
        m.log("demo: httpGet status=" + status + " bodyLength=" + String(text).length);
    });
}

// Message hooks (sync, return-based) ---------------------------------------
function onOutgoingPrepared() {
    var d = getData() || {};
    var text = d.text || "";
    sendCount++;
    var s = m.getState() || {};
    s.sent = sendCount;
    m.setState(s);

    if (text.indexOf("#block") >= 0) {
        blockedCount++;
        updatePanel();
        return { action: "block", message: "Blocked by MYgram demo plugin (#block)" };
    }
    if (text.indexOf("#upper") >= 0) {
        updatePanel();
        return { action: "mutate", text: text.toUpperCase() };
    }
    if (text.indexOf("#test") >= 0) {
        updatePanel();
        return { action: "mutate", text: text + " [+MYgram test]" };
    }
    updatePanel();
    return null; // allow
}

function onSendMessage() {
    var d = getData() || {};
    m.log("demo: message sent to chat " + d.chatId + ": " + (d.text || ""));
    return null; // allow
}

function onReceiveMessage() {
    var d = getData() || {};
    var text = d.text || "";
    if (text.indexOf("#mark") >= 0) {
        return { action: "mutate", text: text.replace(/#mark/g, " ").replace(/\s+/g, " ").trim() + " [Y]" };
    }
    return null;
}

// Lifecycle notifications --------------------------------------------------
function onSettingsOpen() {
    m.notify("MYgram Demo", "Settings screen opened (onSettingsOpen hook)");
    updatePanel();
}

function onChatOpen() {
    var d = getData() || {};
    m.log("demo: chat opened, id=" + d.chatId);
    var s = m.getState() || {};
    if (!s.lastChat || s.lastChat !== d.chatId) {
        s.lastChat = d.chatId;
        m.setState(s);
        m.notify("MYgram Demo", "Chat opened: id=" + d.chatId);
    }
}

function onInputChanged() {
    var d = getData() || {};
    var t = d.text || "";
    var now = new Date().getTime();
    if (now - lastInputAt < 500) {
        return;
    }
    lastInputAt = now;
    lastInputText = t;
    if (lastInputText.length > 60) {
        lastInputText = lastInputText.substring(0, 60) + "...";
    }
    updatePanel();
}