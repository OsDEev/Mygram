var __logs__ = [];

function log(msg) {
    try { __logs__.push(String(msg)); } catch (e) {}
}

function getLogs() {
    try { return __logs__.join("\n"); } catch (e) { return ""; }
}