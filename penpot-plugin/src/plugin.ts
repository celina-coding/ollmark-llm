import { ExecuteCodeTaskHandler } from "./task-handlers/ExecuteCodeTaskHandler";
import { Task, TaskHandler } from "./TaskHandler";

/**
 * Registry of all available task handlers.
 */
const taskHandlers: TaskHandler[] = [new ExecuteCodeTaskHandler()];

// Determine whether multi-user mode is enabled based on build-time configuration
declare const IS_MULTI_USER_MODE: boolean;
const isMultiUserMode = typeof IS_MULTI_USER_MODE !== "undefined" ? IS_MULTI_USER_MODE : false;

console.log("[Plugin] Starting Penpot MCP Plugin, multi-user mode:", isMultiUserMode);

// Open the plugin UI (main.ts)
penpot.ui.open("Penpot MCP Plugin", `?theme=${penpot.theme}&multiUser=${isMultiUserMode}`, { 
    width: 158, 
    height: 200 
});

// Handle messages from UI
penpot.ui.onMessage<string | { id: string; task: string; params: any }>((message) => {
    console.log("[Plugin] Received message from UI:", message);
    
    // Handle plugin task requests
    if (typeof message === "object" && message.task && message.id) {
        handlePluginTaskRequest(message).catch((error) => {
            console.error("[Plugin] Error in handlePluginTaskRequest:", error);
        });
    }
});

/**
 * Handles plugin task requests received from the MCP server via WebSocket.
 *
 * @param request - The task request containing ID, task type and parameters
 */
async function handlePluginTaskRequest(request: { id: string; task: string; params: any }): Promise<void> {
    console.log("[Plugin] Executing plugin task:", request.task, "with params:", request.params);
    const task = new Task(request.id, request.task, request.params);

    // Find the appropriate handler
    const handler = taskHandlers.find((h) => h.isApplicableTo(task));
    
    if (handler) {
        try {
            console.log("[Plugin] Processing task with handler:", handler.taskType);
            await handler.handle(task);
            
            // check whether a response was sent and send a generic success if not
            if (!task.isResponseSent) {
                console.warn("[Plugin] Handler did not send a response, sending generic success.");
                task.sendSuccess("Task completed without a specific response.");
            }
            console.log("[Plugin] Task handled successfully:", task.requestId);
        } catch (error) {
            console.error("[Plugin] Error handling task:", error);
            const errorMessage = error instanceof Error ? error.message : "Unknown error";
            // FIXED: Proper function call syntax
            task.sendError(`Error handling task: ${errorMessage}`);
        }
    } else {
        console.error("[Plugin] Unknown plugin task:", request.task);
        // FIXED: Proper function call syntax
        task.sendError(`Unknown task type: ${request.task}`);
    }
}

// Handle theme change in the iframe
penpot.on("themechange", (theme) => {
    penpot.ui.sendMessage({
        source: "penpot",
        type: "themechange",
        theme,
    });
});