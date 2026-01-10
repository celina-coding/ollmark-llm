# Penpot MCP Server - Spring Boot Edition

Serveur MCP pour Penpot utilisant Spring Boot, Spring AI et le modèle Phi-3 via Ollama.

<h4>⚠️ Je n'ai testé qu'avec Penpot distant ⚠️</h4>

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Ollama** avec le modèle Phi-3
- Plugin Penpot MCP connecté

## Installation

### 1. Se connecter à la VM

```bash
# Doit être connecté sur la FAC
# Donc en dehors de la FAC utiliser le VPN

chmod +x start-tunnel.sh
./start-tunnel.sh
```

### 2. Lancer le serveur MCP

```bash
cd penpot-mcp-server
mvn clean package spring-boot:run
```

### 3. Lancer le plugin Penpot

```bash
cd penpot-plugin
npm install && npm run build && npm run dev
```

### 4. Faire la connexion entre les deux sur Penpot

Rentrer : <code>http://localhost:4400/manifest.json</code> pour le plugin.

et ensuite le connecter en cliquant sur le bouton.

## Architecture

```
        Client MCP
            ↓
   REST API (McpController)
            ↓
PenpotAiService (Spring AI + Phi-3)
            ↓
   Tools (ExecuteCodeTool, etc.)
            ↓
       PluginBridge
            ↓
   WebSocket → Plugin Penpot
```

## Endpoints API

### REST Endpoints

- `POST /mcp/execute-code` - Exécuter du code JavaScript dans Penpot
- `GET /mcp/overview` - Obtenir l'aperçu de l'API Penpot
- `GET /mcp/api-info?type={type}&member={member}` - Info sur un type API
- `POST /mcp/chat` - Conversation avec l'IA
- `POST /mcp/generate` - Générer du code avec l'IA et l'éxecute dans Penpot

### WebSocket

- `ws://localhost:4402/plugin` - Connexion pour le plugin Penpot

## Utilisation des outils

### Exécuter du code

```bash
curl -X POST http://localhost:4401/mcp/execute-code \
  -H "Content-Type: application/json" \
  -d '{"code": "return penpot.currentFile?.name || \"No file\";"}'
```

### Chat avec l'IA

```bash
curl -X POST http://localhost:4401/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Comment créer un rectangle rouge?",
    "history": []
  }'
```

### Générer du code

```bash
curl -X POST http://localhost:4401/mcp/generate \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Créer un rectangle rouge de 100x50 pixels",
    "context": "Page actuelle vide"
  }'
```