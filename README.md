# Penpot Server - Spring Boot Edition

Serveur pour Penpot utilisant Spring Boot, Spring AI et des LLMs via Ollama avec interface graphique Open WebUI.

<h3>
  Pour plus d'informations voir le dépôt Drive,
  <a href="https://drive.google.com/drive/folders/1VALAJD85jiV62pRaVgI-QgIPzLclRzu8?usp=drive_link"
  target="_blank">ici</a>
</h3>

<h4>⚠️ Je n'ai testé qu'avec Penpot distant ⚠️</h4>

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+**
- **Ollama** avec interface Open WebUI (configuré sur VM distante)
- Plugin Penpot connecté

## Configuration du LLM

Le serveur utilise une VM distante avec Ollama et Open WebUI déployés via Docker Compose :

- **Open WebUI** : http://10.130.163.62:3000/ (interface graphique avec authentification)
- **Ollama API** : http://10.130.163.62:11434/ (requêtes HTTP directes)

### Modèles disponibles

Vous pouvez gérer les modèles via l'interface Open WebUI. Le modèle par défaut configuré est `qwen3:8b` mais vous pouvez le changer via les variables d'environnement.

## Installation

### 1. Lancer le serveur

```bash
cd penpot-ai-server
mvn clean package spring-boot:run
```

Le serveur démarre sur `http://localhost:4401`

### 2. Lancer le plugin Penpot

```bash
cd penpot-plugin
npm install && npm run dev
```

### 3. Connecter le plugin à Penpot

Dans Penpot, installer le plugin en entrant :
```
http://localhost:4400/manifest.json
```

Puis cliquer sur le bouton de connexion dans le plugin.

## Architecture

```
          Client
            ↓
   REST API (AiController)
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

Le plugin est le point d’entrée principal pour l’utilisateur.

### Ce que fait le plugin
Il expose **deux modes complémentaires** :
1. **Mode conversationnel chat**
2. **Mode exécution JavaScript**

- Fournit une interface conversationnelle dans Penpot
- Transmet les messages utilisateur au serveur
- Reçoit les instructions générées par l’IA
- Exécute ces instructions directement dans Penpot
- Fournit une interface permettant d'écrire et d'éxecuter du code JavaScript directement dans Penpot


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
PENPOT_SERVER_ADDRESS=localhost
PENPOT_SERVER_LISTEN_ADDRESS=localhost

# Ollama (VM distante)
OLLAMA_BASE_URL=http://10.130.163.62:11434
OLLAMA_MODEL=qwen3:1.7b
OLLAMA_EMBEDDING_MODEL=mxbai-embed-large:latest
OLLAMA_TEMPERATURE=0.7
OLLAMA_MAX_TOKENS=4096

# Chat Memory
PENPOT_CHAT_MEMORY_MAX_MESSAGES=20

# Mode
PENPOT_REMOTE_MODE=false
PENPOT_MULTI_USER=false

# Logging
PENPOT_LOG_LEVEL=debug
PENPOT_LOG_DIR=logs
```

### Configuration des templates

Les templates marketing sont chargés depuis `src/main/resources/data/rag/templates/*.json`

Configuration RAG :
```yaml
penpot.rag:
  templates-path: classpath:data/rag/templates/*.json
  similarity-threshold: 0.6
  top-k: 3
```

### Base de données H2

Le serveur utilise H2 en mode fichier pour stocker l'historique des conversations :

- **Fichier** : `./data/penpot-chatmemory`
- **Console** : http://localhost:4401/h2-console
  - URL JDBC : `jdbc:h2:file:./data/penpot-chatmemory`
  - User : `sa`
  - Password : (vide)

## Mode Multi-Utilisateur

Pour activer le mode multi-utilisateur :

```bash
export PENPOT_MULTI_USER=true
mvn spring-boot:run
```

Ensuite, ajoutez le header `X-User-Token` dans vos requêtes :

```bash
curl -X POST http://localhost:4401/ai/execute-code \
  -H "Content-Type: application/json" \
  -H "X-User-Token: alice" \
  -d '{"code": "..."}'
```

## Timeouts

Le timeout par défaut pour l'exécution des tâches est de **30 secondes**. Configurable via :

```bash
export PENPOT_TASK_TIMEOUT_SECONDS=60
```

## Logs

Les logs sont écrits dans :
- **Console** : Sortie standard avec niveau configurable
- **Fichier** : `logs/penpot.log` (chemin configurable)

## Exemples d'Utilisation

### Workflow utilisateur mode chat
#### Déroulement :
1. L’utilisateur écrit un message dans le **chat du plugin**
2. Le plugin transmet le message au **serveur** via WebSocket
3. Le serveur :
   - maintient le contexte de la conversation (ChatMemory)
   - interroge le LLM (Ollama via Spring AI)
   - sélectionne éventuellement un template marketing (RAG)
   - génère du **code JavaScript compatible Penpot**
4. Le code est renvoyé au plugin
5. Le plugin **exécute le code dans Penpot**
6. Le design est créé ou modifié

### Workflow utilisateur mode JavaScript
**Principe :**
- L’utilisateur écrit directement du **JavaScript Penpot**
- Le code est exécuté dans le contexte du fichier Penpot courant

**Cas d’usage :**
- scripts utilitaires
- tests rapides
- accès précis à l’API Penpot

#### Déroulement :
1. L’utilisateur écrit du JavaScript et l'éxecute dans le plugin
2. Le plugin envoie le code au serveur
3. Le serveur :
   - valide
   - exécute le code
4. Le résultat est renvoyé au plugin
5. Le plugin applique le résultat dans Penpot

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
3. Vérifiez la configuration `penpot.ai.rag.templates-path`

### La console H2 ne s'affiche pas

La console H2 est disponible à : http://localhost:4401/h2-console

Paramètres de connexion :
- JDBC URL : `jdbc:h2:file:./data/penpot-chatmemory`
- Username : `sa`
- Password : (laisser vide)

## Développement

### Structure du projet

```
src/main/java/com/penpot/ai/
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

Pour toute question ou problème, consultez les logs dans `logs/penpot.log`