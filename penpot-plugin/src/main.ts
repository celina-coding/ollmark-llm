import "./style.css";
import { UIOrchestrator } from './ui/orchestration/UIOrchestrator';

/**
 * Main entry point for the Penpot UI
 */

// ============================================================================
// Configuration
// ============================================================================

declare const PENPOT_WEBSOCKET_URL: string;

const WS_URL = typeof PENPOT_WEBSOCKET_URL !== 'undefined' 
    ? PENPOT_WEBSOCKET_URL 
    : "ws://localhost:4401/plugin";

const API_BASE_URL = WS_URL.replace(/^ws/, "http").replace(/\/plugin$/, "");

// ============================================================================
// Application Bootstrap
// ============================================================================

/**
 * Bootstrap the application
 */
function bootstrap(): void {
    // Create the UI orchestrator (Facade pattern)
    const orchestrator = new UIOrchestrator(WS_URL, API_BASE_URL);

    // Initialize the UI system
    orchestrator.initialize();

    // Cleanup on page unload
    window.addEventListener('beforeunload', () => {
        orchestrator.destroy();
    });

    console.log('[Main] Penpot Plugin UI initialized');
}

// Start the application
bootstrap();