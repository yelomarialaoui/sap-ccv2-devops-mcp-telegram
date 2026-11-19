# sap-ccv2-devops-mcp-telegram

Monorepo de deux modules Spring Boot qui permettent de piloter SAP Cloud Controller v2 (CCv2) depuis Telegram, via un agent IA.

| Module | Rôle |
|---|---|
| [sap-ccv2-devops-mcp-server](./sap-ccv2-devops-mcp-server/) | Serveur MCP (port `8086`). Expose les outils CCv2 : créer/lister des builds, suivre leur progression, créer/lister des déploiements. Appelle l'API CCv2 avec un token OAuth du technical user. |
| [sap-ccv2-devops-mcp-client-telegram](./sap-ccv2-devops-mcp-client-telegram/) | Bot Telegram + agent Spring AI (OpenAI). Se connecte au serveur MCP et répond uniquement aux identifiants Telegram autorisés. |

## Architecture

```
Telegram ──► Client (Spring AI agent) ──MCP (streamable HTTP)──► Serveur MCP ──OAuth──► SAP CCv2 API
```

## Prérequis

- JDK 21
- Maven 3.9+
- Un technical user CCv2 (client ID / secret) et le code de souscription
- Une clé API OpenAI
- Un bot Telegram (token fourni par `@BotFather`)

## Configuration

Les secrets ne sont jamais versionnés. Chaque module lit un fichier `secrets.properties` (ignoré par Git) importé depuis son `application.properties`.

```bash
cp sap-ccv2-devops-mcp-server/secrets.properties.example sap-ccv2-devops-mcp-server/secrets.properties
cp sap-ccv2-devops-mcp-client-telegram/secrets.properties.example sap-ccv2-devops-mcp-client-telegram/secrets.properties
```

### Serveur (`sap-ccv2-devops-mcp-server/secrets.properties`)

| Clé | Description |
|---|---|
| `sap.ccv2.api.clientId` | Client ID du technical user CCv2 |
| `sap.ccv2.api.clientSecret` | Secret du technical user CCv2 |
| `sap.ccv2.api.subscriptionCode` | Code de souscription CCv2 (projet hybris ciblé) |
| `sap.ccv2.api.tokenEndpoint` | Endpoint OAuth (`https://ycloud.accounts.ondemand.com/oauth2/token`) |
| `sap.ccv2.api.resource` | Ressource OAuth CCv2 |

Changer de projet hybris revient à modifier `subscriptionCode`, et si besoin le technical user (`clientId` / `clientSecret`) associé à ce projet.

### Client (`sap-ccv2-devops-mcp-client-telegram/secrets.properties`)

| Clé | Description |
|---|---|
| `spring.ai.openai.api-key` | Clé API OpenAI |
| `api.telegram.token` | Token du bot Telegram |
| `allowed.client.ids` | Identifiants Telegram autorisés, séparés par des virgules |

L'URL du serveur MCP est définie dans `spring.ai.mcp.client.streamable-http.connections.mymcpserver.url` (client `application.properties`).

## Lancer en local

Démarrer d'abord le serveur, puis le client :

```bash
cd sap-ccv2-devops-mcp-server
mvn spring-boot:run
```

```bash
cd sap-ccv2-devops-mcp-client-telegram
mvn spring-boot:run
```

## Outils MCP exposés

| Outil | Action |
|---|---|
| Create SAP Commerce Cloud Build | Lance un build |
| Get SAP Commerce Cloud Builds | Liste les builds |
| Get SAP Commerce Cloud Build Progress | Suit la progression d'un build |
| Create SAP Commerce Cloud Deployment | Lance un déploiement |
| Get SAP Commerce Cloud Deployments | Liste les déploiements |

## Tests et qualité

Chaque module se compile et se teste avec :

```bash
mvn -B verify
```

- Tests unitaires JUnit 5 et Mockito.
- Couverture Jacoco avec un seuil minimal de 80 % sur les lignes (le modèle de données `server/model` est exclu côté serveur).
- Rapports : `target/surefire-reports/` (tests) et `target/site/jacoco/` (couverture).

## CI sur les Pull Requests

Le workflow [pull-request.yml](./.github/workflows/pull-request.yml) s'exécute sur chaque PR, pour les deux modules :

1. `mvn -B verify` (tests et contrôle de couverture)
2. Publication des résultats JUnit dans la PR (module serveur, PR depuis le même dépôt)
3. Analyse SonarCloud (PR depuis le même dépôt)

Secret GitHub requis pour l'analyse Sonar : `SONAR_TOKEN`. L'organisation SonarCloud (`yelomarialaoui`) est définie directement dans le workflow.

## Sécurité

- Ne commitez jamais `secrets.properties`. Seuls les fichiers `.example` sont versionnés.
- Si un secret a été exposé (logs, captures, historique de chat), faites-le tourner immédiatement.