import { defineConfig } from "vite";
import livePreview from "vite-live-preview";

// ============================================================================
// Environment Configuration
// ============================================================================

const serverAddress = process.env.PENPOT_SERVER_ADDRESS || "localhost";
const websocketPort = process.env.PENPOT_WEBSOCKET_PORT || "4401";
const websocketUrl = `ws://${serverAddress}:${websocketPort}/plugin`;

const isMultiUserMode = process.env.MULTI_USER_MODE === "true";

// Preview server configuration
const previewPort = parseInt(process.env.PENPOT_PLUGIN_SERVER_PORT || "4400", 10);
const allowedHosts = process.env.PENPOT_PLUGIN_SERVER_LISTEN_ADDRESS
  ? process.env.PENPOT_PLUGIN_SERVER_LISTEN_ADDRESS.split(",").map((h) => h.trim())
  : [];

// ============================================================================
// Configuration Logging
// ============================================================================

console.log("=".repeat(60));
console.log("Vite Build Configuration");
console.log("=".repeat(60));
console.log(`Environment:       ${process.env.NODE_ENV || 'development'}`);
console.log(`Multi-user mode:   ${isMultiUserMode}`);
console.log(`WebSocket URL:     ${websocketUrl}`);
console.log(`Preview port:      ${previewPort}`);
console.log(`Allowed hosts:     ${allowedHosts.length > 0 ? allowedHosts.join(', ') : 'none'}`);
console.log("=".repeat(60));

// ============================================================================
// Vite Configuration
// ============================================================================

export default defineConfig({
  // Plugins
  plugins: [
    livePreview({
      reload: true,
      config: {
        build: {
          sourcemap: true,
        },
      },
    }),
  ],

  // Build configuration
  build: {
    // Output directory
    outDir: "dist",
    
    // Enable source maps for debugging
    sourcemap: true,
    
    // Target modern browsers
    target: "es2022",
    
    // Minification
    minify: "esbuild",
    
    // Rollup options
    rollupOptions: {
      input: {
        plugin: "src/plugin.ts",
        index: "./index.html",
      },
      output: {
        entryFileNames: "[name].js",
        chunkFileNames: "chunks/[name]-[hash].js",
        assetFileNames: "assets/[name]-[hash][extname]",
      },
    },
    
    // Don't emit on errors
    emptyOutDir: true,
  },

  // Preview server configuration
  preview: {
    port: previewPort,
    cors: true,
    strictPort: true,
    host: allowedHosts.length > 0 ? allowedHosts[0] : "localhost",
  },

  // Development server configuration
  server: {
    port: previewPort,
    cors: true,
    strictPort: false,
  },

  // Define global constants
  define: {
    IS_MULTI_USER_MODE: JSON.stringify(isMultiUserMode),
    PENPOT_WEBSOCKET_URL: JSON.stringify(websocketUrl),
  },

  // Resolve configuration
  resolve: {
    alias: {
      "@": "/src",
    },
  },

  // Optimizations
  optimizeDeps: {
    include: ["@penpot/plugin-types", "@penpot/plugin-styles"],
  },
});