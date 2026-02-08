import { PluginOrchestrator } from './orchestration/PluginOrchestrator';
import { ExecuteCodeTaskHandler } from './handlers/code/ExecuteCodeTaskHandler';
import { PluginTaskRequest } from './common/types';

// Determine multi-user mode from build configuration
declare const IS_MULTI_USER_MODE: boolean;
const isMultiUserMode = typeof IS_MULTI_USER_MODE !== "undefined" ? IS_MULTI_USER_MODE : false;

console.log("[Plugin] Starting Penpot Plugin");
console.log("[Plugin] Multi-user mode:", isMultiUserMode);

// Create and initialize orchestrator
const orchestrator = new PluginOrchestrator();

// Register all handlers
orchestrator.initialize([
    new ExecuteCodeTaskHandler(),
]);

console.log("[Plugin] Registered handlers:", orchestrator.getRegisteredHandlers());

// Open UI
penpot.ui.open(
    "Penpot AI Plugin",
    `?theme=${penpot.theme}&multiUser=${isMultiUserMode}`,
    { width: 500, height: 800 }
);

// Handle messages from UI
penpot.ui.onMessage<PluginTaskRequest | any>((message) => {
    console.log("[Plugin] Received message:", message);

    // Type guard for task requests
    if (isTaskRequest(message)) {
        console.log("[Plugin] Processing task request");
        orchestrator.handleTaskRequest(message).catch((error) => {
            console.error("[Plugin] Unhandled error:", error);
        });
    } else {
        console.warn("[Plugin] Unknown message type");
    }
});

// Handle theme changes
penpot.on("themechange", (theme) => {
    penpot.ui.sendMessage({
        source: "penpot",
        type: "themechange",
        theme,
    });
});

/**
 * Type guard for task requests
 */
function isTaskRequest(message: any): message is PluginTaskRequest {
    return (
        typeof message === "object" &&
        message !== null &&
        typeof message.id === "string" &&
        typeof message.task === "string" &&
        "params" in message
    );
}