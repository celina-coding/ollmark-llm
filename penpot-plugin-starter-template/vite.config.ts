import { defineConfig } from "vite";
import livePreview from "vite-live-preview";

export default defineConfig({
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
  server: {
    port: 4400,
    
    // Configuration du proxy pour rediriger les appels API vers Penpot
    proxy: {
      // Proxy pour l'API RPC Penpot (export de fichiers)
      '/api/rpc': {
        target: 'http://localhost:9001',
        changeOrigin: true,
        secure: false,
        // Préserver les cookies de session
        configure: (proxy, _options) => {
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('[PROXY RPC] Forwarding:', req.method, req.url, '→ http://localhost:9001' + req.url);
          });
        }
      }
    }
  },
  build: {
    rollupOptions: {
      input: {
        plugin: "src/plugin.ts",
        index: "./index.html",
      },
      output: {
        entryFileNames: "[name].js",
      },
    },
  },
  preview: {
    port: 4400,
    cors: true,
  },
});