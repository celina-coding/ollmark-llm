# README – OllMark POC LLM

## Sommaire
1. [Présentation générale du projet](#1-présentation-générale-du-projet)
2. [Pré‑requis](#2-pré-requis)
3. [Installation complète du projet](#3-installation-complète-du-projet)
4. [Lancer l'infrastructure via Docker](#4-lancer-linfrastructure-via-docker)
5. [Structure du projet](#5-structure-du-projet)
6. [Fonctionnement général](#6-fonctionnement-général)
7. [Utilisation du service via API](#7-utilisation-du-service-via-api)
8. [Exemples de prompts de test](#8-exemples-de-prompts-de-test)
9. [Accès aux données de test](#9-accès-aux-données-de-test)
10. [Logs et Debug](#10-logs-et-debug)
11. [Gestion de l'environnement](#11-gestion-de-lenvironnement)
12. [Stack technologique](#12-stack-technologique)
13. [Ressources et liens utiles](#13-ressources-et-liens-utiles)

## 1. Présentation générale du projet

Ce POC vise à évaluer et comparer plusieurs modèles d'intelligence artificielle pour générer automatiquement du code compatible avec Penpot via un microservice Spring Boot. Le modèle principal testé est **Gemini 2.5 Flash** (API Google), avec possibilité d'extension vers d'autres modèles.

Le POC permet :
- d'envoyer un prompt à l'IA
- de recevoir du code Penpot prêt à copier-coller dans un plugin
- d'évaluer la qualité du code généré à l'aide de plusieurs métriques
- de réaliser des tests comparatifs de stabilité
- d'exécuter tout le système localement grâce à Docker

Ce document explique chaque étape, de l'installation jusqu'à l'utilisation complète du service.

---

## 2. Pré‑requis

Avant de commencer, vous devez disposer des éléments suivants :

### 2.1. Logiciels requis
- **Docker** et **Docker Compose**  
  Téléchargement : https://www.docker.com/get-started/
- **Java 21+**  
  (nécessaire pour compiler le projet)
- **Maven 3.6+**  
  Téléchargement : https://maven.apache.org/download.cgi
- **Accès API Google Gemini**  
  Obtenez votre clé API : https://ai.google.dev/

### 2.2. Ports utilisés
- Microservice Spring Boot : **8080**
- PostgreSQL : **5432**

Assurez‑vous qu'ils sont libres avant de démarrer.

---

## 3. Installation complète du projet

### 3.1. Cloner le repository
```bash
git clone git@gitlab-dpt-info-sciences.univ-rouen.fr:m2gil/ollmark/ollmark-poc/ollmark-poc-llm.git
cd ollmark-poc-llm
```

### 3.2. Configuration des variables d'environnement

Créez un fichier `.env` à la racine du projet avec le contenu suivant :

```env
OPENAI_API_KEY=gsk_...

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ollca
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password

# Prompt Système
PROMPT_SYSTEM_SECTION=[SYSTÈME]...
PROMPT_CRITICAL_WARNINGS=[AVERTISSEMENT CRITIQUE]...
PROMPT_ALLOWED_IMAGE_METHODS=[MÉTHODES IMAGES AUTORISÉES UNIQUEMENT]...
PROMPT_OUTPUT_FORMAT=[FORMAT DE SORTIE]...
PROMPT_CORRECTION_TEMPLATE=...
PROMPT_CORRECTION_INSTRUCTIONS=[INSTRUCTIONS DE CORRECTION STRICTES]...
```

⚠️ **Important** : Ne committez JAMAIS le fichier `.env` !

### 3.3. Configuration de la base de données

Le fichier `init.sql` sera automatiquement exécuté au démarrage de PostgreSQL. Il contient :
- La structure des tables `boutique` et `produit`
- Des données de test pour deux boutiques (boucherie et fromagerie)
- Des produits associés

---

## 4. Lancer l'infrastructure via Docker

### 4.1. Compiler le projet
Avant de lancer Docker, vous devez compiler le JAR de l'application :

```bash
mvn clean package -DskipTests
```

Cela génère le fichier `target/aiPoc-0.0.1-SNAPSHOT.jar` qui sera utilisé par Docker.

### 4.2. Démarrer les services
Depuis la racine du projet :

```bash
docker compose up --build
```

Les services démarreront dans l'ordre :
1. **PostgreSQL** (ollca-postgres) - Base de données
2. **Spring Boot App** (ollca-spring-app) - Microservice

### 4.3. Vérifier que tout fonctionne
```bash
docker ps
```

Vous devez voir :

| Container         | Port        | Statut |
|------------------|-------------|--------|
| ollca-postgres    | 5432:5432   | Up     |
| ollca-spring-app  | 8080:8080   | Up     |

Vérifiez les logs du microservice :
```bash
docker logs ollca-spring-app
```

Vous devriez voir :
```
Started AiPocApplication in X.XXX seconds
```

### 4.4. Démarrer Penpot

### 4.4.1. Démarrer le studio

Depuis la racine du projet
```
cd penpot-plugin-starter-template/
docker compose up --build
``` 

### 4.4.2. Démarrer le plugin
```
npm install && npm run dev
```

### 4.4.3. Vérifier que tout fonctionne
```bash
docker ps
```

Vous devez voir :

| Container         | Port        | Statut |
|------------------|-------------|--------|
| penpotapp/frontend  | 9001:8080   | Up     |


### 4.5. Tester l'API

### 4.5.1. Depuis le Microservice

1. Ouvrez votre navigateur : http://localhost:8080
2. Vous devriez voir la page d'accueil du projet.

### 4.5.2. Depuis le Studio Penpot
1. Ouvrez le studio Penpot : http://localhost:9001
2. Créez un projet.
3. Cliquez sur le menu "Extensions" <i>(Ctrl+Alt+P)</i>
4. Rentrez cette URL : "http://localhost:4400/manifest.json"
5. Ouvrez le plugin : "Générateur AI Penpot"


---

## 5. Structure du projet

```
ollmark-poc-llm/
├── src/main/java/com/example/aiPoc/
│   ├── AiPocApplication.java              # Point d'entrée Spring Boot
│   ├── config/                            # Configuration (CORS, AI Provider)
│   ├── controllers/                       # Endpoints REST
│   │   ├── EvaluationController.java      # Tests d'évaluation
│   │   ├── HomeController.java            # Page d'accueil
│   │   └── PenpotCodeController.java      # Génération de code Penpot
│   ├── dto/                               # Data Transfer Objects
│   │   ├── request/                       # Objets de requête
│   │   └── response/                      # Objets de réponse
│   ├── models/                            # Entités JPA et modèles
│   │   ├── Boutique.java                  # Entité boutique
│   │   ├── Produit.java                   # Entité produit
│   │   ├── EvaluationResult.java          # Résultat d'évaluation
│   │   └── PromptStrategy.java            # Stratégies de prompt
│   ├── services/
│   │   ├── ai/                            # Services IA
│   │   │   ├── OpenAIService.java         # Intégration OpenAI
│   │   │   ├── PromptBuilderService.java  # Construction des prompts
│   │   │   └── PromptStrategyService.java # Gestion des stratégies
│   │   ├── evaluation/                    # Service d'évaluation
│   │   ├── orchestration/                 # Orchestration de génération
│   │   └── penpot/                        # Services Penpot
│   │       ├── CodeCleanerService.java    # Nettoyage du code
│   │       └── CodeValidationService.java # Validation du code
│   └── repositories/                      # Repositories JPA
├── src/main/resources/
│   ├── application.properties             # Configuration Spring
│   ├── static/                            # Pages HTML statiques
│   └── templates/                         # Templates JSON
├── docker-compose.yml                     # Configuration Docker
├── Dockerfile                             # Image Docker de l'app
├── init.sql                               # Initialisation PostgreSQL
├── .env                                   # (à créer)
└── pom.xml
```

---

## 6. Fonctionnement général

Le microservice suit le workflow suivant :

1. **Réception de la requête** : Un prompt est envoyé via l'API REST
2. **Sélection de la stratégie** : Choix du type de génération (création, modification, etc.)
3. **Construction du prompt** : Enrichissement avec contexte et exemples
4. **Appel à l'IA** : Communication avec Gemini via l'API Google
5. **Nettoyage du code** : Extraction et formatage du code Penpot
6. **Validation** : Vérification de la syntaxe et de la structure
7. **Évaluation** : Calcul de métriques de qualité
8. **Retour de la réponse** : Code prêt à l'emploi + métadonnées

---

## 7. Utilisation du service via API

### 7.1. Générer du code Penpot

**Endpoint** : `POST /api/code/generate`

**Exemple de requête** :
```json
{
  "prompt": "Crée une carte de visite professionnelle pour Boucherie Huet avec nom, adresse et téléphone",
  "strategy": "creation",
  "cleanCode": true,
  "includeValidation": true
}
```

**Réponse** :
```json
{
  "generatedCode": "// Code Penpot TypeScript prêt à l'emploi",
  "rawResponse": "// Réponse brute de l'IA",
  "enrichedPrompt": "// Prompt enrichi envoyé à l'IA",
  "valid": true,
  "validationErrors": [],
  "generationTimeMs": 2345,
  "strategy": "creation"
}
```

### 7.2. Tester la stabilité d'un prompt

**Endpoint** : `POST /api/evaluation/test-prompt`

**Exemple de requête** :
```json
{
  "model": "gemini-2.5-flash",
  "promptId": "carte_visite_v1",
  "prompt": "Crée une carte de visite pour une boucherie",
  "strategy": "creation"
}
```

Cette requête exécute **5 fois** le même prompt et évalue :
- La stabilité des résultats
- La cohérence du code généré
- Le respect du format Penpot
- Le temps de réponse moyen

### 7.3. Exporter les résultats d'évaluation

**CSV** : `GET /api/evaluation/export/csv`  
**JSON** : `GET /api/evaluation/export/json`

---

### 8. Exemples de prompts de test

**Prompt 1 - Carte de visite** :
```
Crée une carte de visite professionnelle pour Boucherie Huet avec :
- Nom de la boutique
- Slogan
- Adresse complète
- Téléphone et email
```

**Prompt 2 - Email marketing** :
```
Génère un template d'email marketing pour La Fromagerie du Marché avec :
- Un header attractif
- Une section promotion
- Une liste de produits
- Un footer avec coordonnées
```

**Prompt 3 - Affiche promotionnelle** :
```
Crée une affiche A4 pour une promotion :
- Titre accrocheur
- Visuels de produits
- Prix barrés/promotions
- Call-to-action
```

---

## 9. Accès aux données de test

Le microservice inclut des données de test pré-chargées :

### 9.1. Boutiques disponibles
- **Boucherie Huet** (Versailles)
  - 3 produits : Côte de Boeuf, Filet Mignon, Saucisses
- **La Fromagerie du Marché** (Paris)
  - 2 produits : Comté 18 mois, Camembert AOP

### 9.2. Endpoints d'accès aux données
```
GET /api/boutiques           # Liste toutes les boutiques
GET /api/boutiques/{id}      # Détails d'une boutique
GET /api/produits            # Liste tous les produits
GET /api/produits/{id}       # Détails d'un produit
```

---

## 10. Logs et Debug

### 10.1. Consulter les logs en temps réel
```bash
docker logs -f ollca-spring-app
```

### 10.2. Logs PostgreSQL
```bash
docker logs -f ollca-postgres
```

### 10.3. Niveaux de logging
Configurés dans `application.properties` :
```properties
logging.level.root=INFO
logging.level.com.example.aiPoc=INFO
logging.level.org.springframework.ai=DEBUG
```

---

## 11. Gestion de l'environnement

### 11.1. Arrêter les services
```bash
docker compose down
```

### 11.2. Redémarrer après modifications
```bash
# Recompiler le JAR
mvn clean package -DskipTests

# Redémarrer Docker
docker compose down && docker compose up --build
```

### 11.3. Nettoyer complètement (⚠️ efface les données)
```bash
docker compose down -v
```

### 11.4. Accéder à la base de données
```bash
docker exec -it ollca-postgres psql -U user -d ollca
```

Commandes SQL utiles :
```sql
-- Lister les boutiques
SELECT * FROM boutique;

-- Lister les produits avec leur boutique
SELECT b.nom as boutique, p.nom, p.prix 
FROM produit p 
JOIN boutique b ON p.id_boutique = b.id_boutique;
```

---

## 12. Stack technologique
- **Backend** : Spring Boot 3.5.6
- **Java** : 21
- **Base de données** : PostgreSQL 15
- **IA** : OpenAI (via SpringAI)
- **ORM** : Spring Data JPA / Hibernate
- **Build** : Maven
- **Conteneurisation** : Docker + Docker Compose

---

## 13. Ressources et liens utiles

- **Documentation Gemini** : https://ai.google.dev/docs
- **Penpot API** : https://penpot-plugins-api-doc.pages.dev/
- **Spring AI** : https://docs.spring.io/spring-ai/reference/index.html