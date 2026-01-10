import "./style.css";

// get the current theme from the URL
const searchParams = new URLSearchParams(window.location.search);
document.body.dataset.theme = searchParams.get("theme") ?? "light";

// Determine whether multi-user mode is enabled based on URL parameters
const isMultiUserMode = searchParams.get("multiUser") === "true";
console.log("[UI] Penpot MCP multi-user mode:", isMultiUserMode);
console.log("[UI] WebSocket URL will be:", PENPOT_MCP_WEBSOCKET_URL);

// WebSocket connection management
let ws: WebSocket | null = null;
const statusElement = document.getElementById("connection-status");

/**
 * Updates the connection status display element.
 */
function updateConnectionStatus(status: string, isConnectedState: boolean, message?: string): void {
    if (statusElement) {
        const displayText = message ? `${status}: ${message}` : status;
        statusElement.textContent = displayText;
        statusElement.style.color = isConnectedState ? "var(--accent-primary)" : "var(--error-700)";
    }
    console.log(`[UI] Connection status: ${status}${message ? ' - ' + message : ''}`);
}

/**
 * Sends a task response back to the MCP server via WebSocket.
 */
function sendTaskResponse(response: any): void {
    if (ws && ws.readyState === WebSocket.OPEN) {
        const jsonResponse = JSON.stringify(response);
        ws.send(jsonResponse);
        console.log("[UI] ✅ Sent response to MCP server:", jsonResponse);
    } else {
        console.error("[UI] ❌ WebSocket not connected, cannot send response. State:", ws?.readyState);
    }
}

/**
 * Establishes a WebSocket connection to the MCP server.
 */
function connectToMcpServer(): void {
    if (ws?.readyState === WebSocket.OPEN) {
        updateConnectionStatus("Already connected", true);
        return;
    }

    try {
        let wsUrl = PENPOT_MCP_WEBSOCKET_URL;
        
        if (isMultiUserMode) {
            // TODO obtain proper userToken from penpot
            const userToken = "dummyToken";
            wsUrl += `?userToken=${encodeURIComponent(userToken)}`;
        }

        console.log("[UI] 🔌 Connecting to WebSocket at:", wsUrl);
        ws = new WebSocket(wsUrl);
        updateConnectionStatus("Connecting...", false);

        ws.onopen = () => {
            console.log("[UI] ✅ Connected to MCP server");
            updateConnectionStatus("Connected to MCP server", true);
        };

        ws.onmessage = (event) => {
            console.log("[UI] 📩 Received from MCP server:", event.data);
            
            try {
                const request = JSON.parse(event.data);
                console.log("[UI] 📤 Forwarding task request to plugin:", request);
                
                // Forward the task request to the plugin for execution
                parent.postMessage(request, "*");
                console.log("[UI] ✅ Task request forwarded to plugin");
                
            } catch (error) {
                console.error("[UI] ❌ Failed to parse WebSocket message:", error);
            }
        };

        ws.onclose = (event: CloseEvent) => {
            console.log("[UI] 🔌 Disconnected from MCP server. Code:", event.code, "Reason:", event.reason);
            const message = event.reason || `Code ${event.code}`;
            updateConnectionStatus("Disconnected", false, message);
            ws = null;
        };

        ws.onerror = (error) => {
            console.error("[UI] ❌ WebSocket error:", error);
            updateConnectionStatus("Connection error", false);
        };
        
    } catch (error) {
        console.error("[UI] ❌ Failed to connect to MCP server:", error);
        const message = error instanceof Error ? error.message : undefined;
        updateConnectionStatus("Connection failed", false, message);
    }
}

// Auto-connect on load
console.log("[UI] Auto-connecting to MCP server...");
connectToMcpServer();

// Also allow manual connection
document.querySelector("[data-handler='connect-mcp']")?.addEventListener("click", () => {
    console.log("[UI] Manual connect button clicked");
    connectToMcpServer();
});

// Listen to messages from plugin.ts
window.addEventListener("message", (event) => {
    console.log("[UI] 📨 Received message from plugin:", event.data);
    
    if (event.data.source === "penpot") {
        // Theme change from plugin
        document.body.dataset.theme = event.data.theme;
        console.log("[UI] Theme updated to:", event.data.theme);
        
    } else if (event.data.type === "task-response") {
        // Task response from plugin - forward to MCP server
        console.log("[UI] 📨 Task response from plugin, forwarding to server:", event.data);
        sendTaskResponse(event.data);  // ← Envoyer le message COMPLET avec type
    }
});