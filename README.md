# Penpot MCP Server - Spring Boot Edition

Serveur MCP pour Penpot utilisant Spring Boot, Spring AI et des LLMs via Ollama avec interface graphique Open WebUI.

<h4>⚠️ Je n'ai testé qu'avec Penpot distant ⚠️</h4>

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Ollama** avec interface Open WebUI (configuré sur VM distante)
- Plugin Penpot MCP connecté

## Configuration du LLM

Le serveur utilise une VM distante avec Ollama et Open WebUI déployés via Docker Compose :

- **Open WebUI** : http://10.130.163.62:3000/ (interface graphique avec authentification)
- **Ollama API** : http://10.130.163.62:11434/ (requêtes HTTP directes)

### Modèles disponibles

Vous pouvez gérer les modèles via l'interface Open WebUI. Le modèle par défaut configuré est `qwen3:1.7b` mais vous pouvez le changer via les variables d'environnement.

## Installation

### 1. Lancer le serveur MCP

```bash
cd penpot-mcp-server
mvn clean package spring-boot:run
```

Le serveur démarre sur `http://localhost:4401`

### 2. Lancer le plugin Penpot

```bash
cd penpot-plugin
npm install && npm run build && npm run dev
```

### 3. Connecter le plugin à Penpot

Dans Penpot, installer le plugin en entrant :
```
http://localhost:4400/manifest.json
```

Puis cliquer sur le bouton de connexion dans le plugin.

## Architecture

```
        Client MCP
            ↓
   REST API (McpController)
            ↓
    ConversationChatUseCase
    (avec ChatMemory + H2)
            ↓
PenpotAiService (Spring AI + Ollama)
    ↓              ↓
Tools:         Templates:
- ExecuteCode  - Search
- APIInfo      - Filter by Type
               - Filter by Tag
            ↓
       PluginBridge
            ↓
   WebSocket → Plugin Penpot
```

## Fonctionnalités

### 🎨 Templates Marketing avec RAG

Le serveur inclut un système de templates marketing avec recherche sémantique :

- **Templates pré-configurés** : Instagram posts, emails, newsletters, stories
- **Recherche intelligente** : L'IA trouve automatiquement les templates pertinents
- **Génération de code** : Conversion automatique des templates en code Penpot

**Exemples de requêtes qui activent les templates :**
- "Crée un post Instagram pour l'offre du matin de ma boulangerie"
- "J'ai besoin d'un post sur les réseaux sociaux pour un lancement produit"
- "Aide-moi à créer une newsletter par email"

### 💬 Gestion des Conversations

Le système utilise **ChatMemory** avec stockage H2 pour maintenir le contexte :

- **Historique automatique** : Pas besoin de passer manuellement l'historique
- **Sessions persistantes** : Les conversations survivent aux redémarrages
- **Multi-utilisateurs** : Support optionnel via userId
- **Limite configurable** : 20 messages par défaut (ajustable)

## Endpoints API

### Gestion des Conversations

**Démarrer une nouvelle conversation**
```bash
curl -X POST http://localhost:4401/mcp/chat/new \
  -H "Content-Type: application/json" \
  -d '{"userId": "alice"}'
```

Réponse :
```json
{
  "success": true,
  "conversationId": "conv_abc123",
  "userId": "alice",
  "info": "New conversation started. Use this conversationId for subsequent messages."
}
```

**Envoyer un message (avec historique automatique)**
```bash
curl -X POST http://localhost:4401/mcp/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv_abc123",
    "message": "Comment créer un rectangle rouge?"
  }'
```

Réponse :
```json
{
  "success": true,
  "conversationId": "conv_abc123",
  "response": "Voici comment créer un rectangle rouge...",
  "info": "Conversation history managed automatically by ChatMemory"
}
```

**Effacer une conversation**
```bash
curl -X DELETE http://localhost:4401/mcp/chat/conv_abc123
```

### Exécution de Code

**Exécuter du code JavaScript dans Penpot**
```bash
curl -X POST http://localhost:4401/mcp/execute-code \
  -H "Content-Type: application/json" \
  -d '{"code": "return penpot.currentFile?.name || \"No file\";"}'
```

### Génération de Code avec IA

**Générer et exécuter du code automatiquement**
```bash
curl -X POST http://localhost:4401/mcp/generate \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Créer un rectangle rouge de 100x50 pixels",
    "context": "Page actuelle vide",
    "executeImmediately": true
  }'
```

### Templates Marketing

**Rechercher des templates**
```bash
curl -X POST http://localhost:4401/mcp/templates/search \
  -H "Content-Type: application/json" \
  -d '{"query": "instagram post bakery"}'
```

**Obtenir tous les templates**
```bash
curl -X GET http://localhost:4401/mcp/templates
```

**Filtrer par type**
```bash
curl -X GET http://localhost:4401/mcp/templates/type/instagram_post
```

**Filtrer par tag**
```bash
curl -X GET http://localhost:4401/mcp/templates/tag/food
```

### Informations API Penpot

**Obtenir l'aperçu de l'API**
```bash
curl -X GET http://localhost:4401/mcp/overview
```

**Obtenir des infos sur un type API**
```bash
curl -X GET "http://localhost:4401/mcp/api-info?type=penpot&member=createRectangle"
```

## WebSocket

**Connexion du plugin**
```
ws://localhost:4401/plugin
```

Pour le mode multi-utilisateur :
```
ws://localhost:4401/plugin?userToken=user123
```

## Configuration

### Variables d'environnement principales

```bash
# Serveur
PENPOT_MCP_SERVER_ADDRESS=localhost
PENPOT_MCP_SERVER_LISTEN_ADDRESS=localhost

# Ollama (VM distante)
OLLAMA_BASE_URL=http://10.130.163.62:11434
OLLAMA_MODEL=qwen3:1.7b
OLLAMA_EMBEDDING_MODEL=mxbai-embed-large:latest
OLLAMA_TEMPERATURE=0.7
OLLAMA_MAX_TOKENS=4096

# Chat Memory
PENPOT_MCP_CHAT_MEMORY_MAX_MESSAGES=20

# Mode
PENPOT_MCP_REMOTE_MODE=false
PENPOT_MCP_MULTI_USER=false

# Logging
PENPOT_MCP_LOG_LEVEL=debug
PENPOT_MCP_LOG_DIR=logs
```

### Configuration des templates

Les templates marketing sont chargés depuis `src/main/resources/data/rag/templates/*.json`

Configuration RAG :
```yaml
penpot.mcp.rag:
  templates-path: classpath:data/rag/templates/*.json
  similarity-threshold: 0.6
  top-k: 3
```

### Base de données H2

Le serveur utilise H2 en mode fichier pour stocker l'historique des conversations :

- **Fichier** : `./data/penpot-mcp-chatmemory`
- **Console** : http://localhost:4401/h2-console
  - URL JDBC : `jdbc:h2:file:./data/penpot-mcp-chatmemory`
  - User : `sa`
  - Password : (vide)

## Mode Multi-Utilisateur

Pour activer le mode multi-utilisateur :

```bash
export PENPOT_MCP_MULTI_USER=true
mvn spring-boot:run
```

Ensuite, ajoutez le header `X-User-Token` dans vos requêtes :

```bash
curl -X POST http://localhost:4401/mcp/execute-code \
  -H "Content-Type: application/json" \
  -H "X-User-Token: alice" \
  -d '{"code": "..."}'
```

## Timeouts

Le timeout par défaut pour l'exécution des tâches est de **30 secondes**. Configurable via :

```bash
export PENPOT_MCP_TASK_TIMEOUT_SECONDS=60
```

## Logs

Les logs sont écrits dans :
- **Console** : Sortie standard avec niveau configurable
- **Fichier** : `logs/penpot-mcp.log` (chemin configurable)

## Exemples d'Utilisation

### Workflow complet avec conversation

```bash
# 1. Démarrer une conversation
CONV_ID=$(curl -s -X POST http://localhost:4401/mcp/chat/new \
  -H "Content-Type: application/json" \
  -d '{"userId": "designer1"}' | jq -r '.conversationId')

# 2. Demander la création d'un design
curl -X POST http://localhost:4401/mcp/chat \
  -H "Content-Type: application/json" \
  -d "{
    \"conversationId\": \"$CONV_ID\",
    \"message\": \"Crée un post Instagram pour promouvoir notre nouveau croissant au chocolat\"
  }"

# 3. Affiner le design (l'IA se souviendra du contexte)
curl -X POST http://localhost:4401/mcp/chat \
  -H "Content-Type: application/json" \
  -d "{
    \"conversationId\": \"$CONV_ID\",
    \"message\": \"Rends le texte plus grand et ajoute un emoji\"
  }"

# 4. Nettoyer
curl -X DELETE http://localhost:4401/mcp/chat/$CONV_ID
```

### Utilisation des templates directement

```bash
# Chercher des templates pour une newsletter
curl -X POST http://localhost:4401/mcp/templates/search \
  -H "Content-Type: application/json" \
  -d '{"query": "email newsletter promotion"}'

# Obtenir tous les templates Instagram
curl -X GET http://localhost:4401/mcp/templates/type/instagram_post
```

## Troubleshooting

### Le serveur ne se connecte pas à Ollama

Vérifiez que la VM distante est accessible :
```bash
curl http://10.130.163.62:11434/api/tags
```

### Le plugin ne se connecte pas

1. Vérifiez que le WebSocket est actif : `ws://localhost:4401/plugin`
2. Consultez les logs du serveur pour les erreurs de connexion
3. Vérifiez que le plugin a bien l'URL correcte dans sa configuration

### Les templates ne sont pas trouvés

1. Vérifiez que les fichiers JSON sont présents dans `src/main/resources/data/rag/templates/`
2. Consultez les logs au démarrage pour voir si les templates sont chargés
3. Vérifiez la configuration `penpot.mcp.rag.templates-path`

### La console H2 ne s'affiche pas

La console H2 est disponible à : http://localhost:4401/h2-console

Paramètres de connexion :
- JDBC URL : `jdbc:h2:file:./data/penpot-mcp-chatmemory`
- Username : `sa`
- Password : (laisser vide)

## Développement

### Structure du projet

```
src/main/java/com/penpot/mcp/
├── adapters/
│   ├── in/
│   │   ├── web/          # REST controllers
│   │   └── websocket/    # WebSocket handlers
│   └── out/
│       ├── ai/           # Spring AI integration
│       └── persistence/  # H2 repositories
├── application/
│   └── service/          # Business logic
├── core/
│   ├── domain/           # Domain entities
│   └── ports/            # Use case interfaces
└── infrastructure/       # Technical infrastructure
```

### Ajouter de nouveaux templates

1. Créer un fichier JSON dans `src/main/resources/data/rag/templates/`
2. Suivre le format :
```json
{
  "id": "unique-id",
  "name": "Template Name",
  "type": "instagram_post",
  "description": "Description...",
  "tags": ["tag1", "tag2"],
  "dimensions": {"width": 1080, "height": 1080},
  "codeTemplate": "// JavaScript code..."
}
```
3. Redémarrer le serveur

## Support

Pour toute question ou problème, consultez les logs dans `logs/penpot-mcp.log`