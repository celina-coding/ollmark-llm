# README – OllMark POC LLM

## Sommaire
1. [Présentation générale du projet](#1-présentation-générale-du-projet)
2. [Pré‑requis](#2-pré-requis)
3. [Installation complète du projet](#3-installation-complète-du-projet)
4. [Lancer l'infrastructure via Docker](#4-lancer-linfrastructure-via-docker)
5. [Structure du projet](#5-structure-du-projet)
6. [Fonctionnement général](#6-fonctionnement-général)
7. [Utilisation du service via API](#7-utilisation-du-service-via-api)
8. [Intégration avec Penpot](#8-intégration-avec-penpot)
9. [Exemples de prompts de test](#9-exemples-de-prompts-de-test)
10. [Accès aux données de test](#10-accès-aux-données-de-test)
11. [Logs et Debug](#11-logs-et-debug)
12. [Gestion de l'environnement](#12-gestion-de-lenvironnement)
13. [Dépannage](#13-dépannage)
14. [Stack technologique](#14-stack-technologique)
15. [Ressources et liens utiles](#15-ressources-et-liens-utiles)

## 1. Présentation générale du projet

Ce projet constitue une preuve de concept (POC) visant à évaluer et comparer plusieurs modèles d'intelligence artificielle pour générer automatiquement du code compatible avec Penpot via un microservice Spring Boot. Le modèle principal testé est **Gemini 2.5 Flash** (API Google), avec possibilité d'extension vers d'autres modèles.

Le POC permet :
- d'envoyer un prompt à l'IA Gemini
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
git clone <URL_DU_REPO>
cd ollmark-poc-llm
```

### 3.2. Configuration des variables d'environnement

Créez un fichier `.env` à la racine du projet avec le contenu suivant :

```env
# Spring AI Vertex AI Gemini Configuration
SPRING_AI_VERTEXAI_GEMINI_PROJECT_ID=ollmark
SPRING_AI_VERTEXAI_GEMINI_LOCATION=us-central1

# Gemini Configuration
GEMINI_API_KEY=<CLE_API>
GEMINI_MODEL=gemini-2.5-flash

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ollca
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
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

### 4.4. Tester l'API
Ouvrez votre navigateur : http://localhost:8080

Vous devriez voir la page d'accueil du projet.

---

## 5. Structure du projet

```
ollmark-poc-llm/
├── src/main/java/com/example/aiPoc/
│   ├── AiPocApplication.java              # Point d'entrée Spring Boot
│   ├── config/                            # Configuration (CORS, AI Provider)
│   ├── controllers/                       # Endpoints REST
│   │   ├── ChatController.java            # Chat avec l'IA
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
│   │   │   ├── GeminiAIService.java       # Intégration Gemini
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
├── pom.xml
└── README.md
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

## 8. Intégration avec Penpot

### 8.1. Télécharger le template plugin Penpot
```bash
git clone https://github.com/penpot/penpot-plugin-starter-template
cd penpot-plugin-starter-template
```

### 8.2. Suivre instructions
Ensuite il suffit de suivre les indications de ce document : [Consulter le guide PDF](./RUN_PENPOT_PLUGIN.pdf)

---

### 9. Exemples de prompts de test

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

## 10. Accès aux données de test

Le microservice inclut des données de test pré-chargées :

### 10.1. Boutiques disponibles
- **Boucherie Huet** (Versailles)
  - 3 produits : Côte de Boeuf, Filet Mignon, Saucisses
- **La Fromagerie du Marché** (Paris)
  - 2 produits : Comté 18 mois, Camembert AOP

### 10.2. Endpoints d'accès aux données
```
GET /api/boutiques           # Liste toutes les boutiques
GET /api/boutiques/{id}      # Détails d'une boutique
GET /api/produits            # Liste tous les produits
GET /api/produits/{id}       # Détails d'un produit
```

---

## 11. Logs et Debug

### 11.1. Consulter les logs en temps réel
```bash
docker logs -f ollca-spring-app
```

### 11.2. Logs PostgreSQL
```bash
docker logs -f ollca-postgres
```

### 11.3. Niveaux de logging
Configurés dans `application.properties` :
```properties
logging.level.root=INFO
logging.level.com.example.aiPoc=INFO
logging.level.org.springframework.ai=DEBUG
```

### 11.4. Fichiers de logs
Les logs sont également sauvegardés dans :
```
logs/app.log
```

---

## 12. Gestion de l'environnement

### 12.1. Arrêter les services
```bash
docker compose down
```

### 12.2. Redémarrer après modifications
```bash
# Recompiler le JAR
mvn clean package -DskipTests

# Redémarrer Docker
docker compose down
docker compose up --build
```

### 12.3. Nettoyer complètement (⚠️ efface les données)
```bash
docker compose down -v
```

### 12.4. Accéder à la base de données
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

## 13. Dépannage

### Problème : Le conteneur Spring Boot ne démarre pas
**Solution** :
1. Vérifiez que le JAR existe : `ls -lh target/*.jar`
2. Si absent, recompilez : `mvn clean package -DskipTests`
3. Vérifiez les logs : `docker logs ollca-spring-app`

### Problème : Erreur "Vertex AI project-id must be set"
**Solution** :
Assurez-vous que `application.properties` contient :
```properties
spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration
```

### Problème : Connexion refusée à PostgreSQL
**Solution** :
1. Vérifiez que PostgreSQL est démarré : `docker ps | grep postgres`
2. Attendez 10-15 secondes après le démarrage
3. Vérifiez les variables d'environnement dans `.env`

### Problème : API Gemini retourne une erreur 401
**Solution** :
1. Vérifiez votre clé API dans `.env`
2. Testez la clé sur https://ai.google.dev/
3. Vérifiez les quotas de votre compte Google AI

---

## 14. Stack technologique
- **Backend** : Spring Boot 3.5.6
- **Java** : 21
- **Base de données** : PostgreSQL 15
- **IA** : Google Gemini 2.5 Flash (via SDK Java)
- **ORM** : Spring Data JPA / Hibernate
- **Build** : Maven
- **Conteneurisation** : Docker + Docker Compose

---

## 15. Ressources et liens utiles

- **Documentation Gemini** : https://ai.google.dev/docs
- **Penpot API** : https://penpot-plugins-api-doc.pages.dev/
- **Spring AI** : https://docs.spring.io/spring-ai/reference/index.html