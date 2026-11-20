# sap-ccv2-devops-mcp-telegram

Monorepo permettant de piloter SAP Cloud Controller v2 (CCv2) depuis Telegram ou Slack, via un agent IA.

| Module | Rôle |
|---|---|
| [sap-ccv2-devops-mcp-server](./sap-ccv2-devops-mcp-server/) | Serveur MCP (port `8086`). Expose les outils CCv2 : créer/lister des builds, suivre leur progression, créer/lister des déploiements. Appelle l'API CCv2 avec un token OAuth du technical user. |
| [sap-ccv2-devops-mcp-client-telegram](./sap-ccv2-devops-mcp-client-telegram/) | Bot Telegram + agent Spring AI (OpenAI). Se connecte au serveur MCP et répond uniquement aux identifiants Telegram autorisés. |
| [sap-ccv2-devops-mcp-client-slack](./sap-ccv2-devops-mcp-client-slack/) | Bot Slack indépendant en Socket Mode, limité aux messages directs des utilisateurs autorisés. |
| [sap-ccv2-devops-mcp-client-common](./sap-ccv2-devops-mcp-client-common/) | Agent Spring AI partagé par les deux clients. |

## Architecture

```
Telegram ──► Client Telegram ──┐
                              ├──► Agent Spring AI ──MCP──► Serveur MCP ──OAuth──► SAP CCv2 API
Slack ─────► Client Slack ─────┘
```

## Prérequis

- JDK 21
- Maven 3.9+
- Un technical user CCv2 (client ID / secret) et le code de souscription
- Une clé API OpenAI
- Pour Telegram : un bot (token fourni par `@BotFather`)
- Pour Slack : une application Slack installée dans votre espace de travail

## Configuration

Les secrets ne sont jamais versionnés. Chaque module lit un fichier `secrets.properties` (ignoré par Git) importé depuis son `application.properties`.

```bash
cp sap-ccv2-devops-mcp-server/secrets.properties.example sap-ccv2-devops-mcp-server/secrets.properties
cp sap-ccv2-devops-mcp-client-telegram/secrets.properties.example sap-ccv2-devops-mcp-client-telegram/secrets.properties
cp sap-ccv2-devops-mcp-client-slack/secrets.properties.example sap-ccv2-devops-mcp-client-slack/secrets.properties
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

### Client Telegram (`sap-ccv2-devops-mcp-client-telegram/secrets.properties`)

| Clé | Description |
|---|---|
| `spring.ai.openai.api-key` | Clé API OpenAI |
| `api.telegram.token` | Token du bot Telegram |
| `allowed.client.ids` | Identifiants Telegram autorisés, séparés par des virgules |

L'URL du serveur MCP est définie dans `spring.ai.mcp.client.streamable-http.connections.mymcpserver.url` (client `application.properties`).

### Client Slack (`sap-ccv2-devops-mcp-client-slack/secrets.properties`)

1. Dans [Slack API](https://api.slack.com/apps), créez une application et activez **Socket Mode**.
2. Créez un token d'application `xapp-` avec le scope `connections:write`.
3. Dans **OAuth & Permissions**, ajoutez les scopes du bot `chat:write` et `im:history`. Dans **Event Subscriptions**, activez les événements et abonnez le bot à `message.im`.
4. Dans **App Home**, autorisez les utilisateurs à envoyer des messages au bot. Installez l'application dans l'espace de travail et récupérez son token `xoxb-`.
5. Renseignez les valeurs suivantes dans le fichier de secrets :

| Clé | Description |
|---|---|
| `spring.ai.openai.api-key` | Clé API OpenAI |
| `slack.app-token` | Token d'application `xapp-` |
| `slack.bot-token` | Token du bot `xoxb-` |
| `slack.allowed-user-ids` | Identifiants Slack des utilisateurs autorisés, séparés par des virgules |

L'URL MCP est configurable dans `spring.ai.mcp.client.streamable-http.connections.mymcpserver.url`. Les messages des canaux et des bots sont ignorés ; les conversations Slack sont séparées par utilisateur et par conversation directe.

## Lancer en local

D'abord, depuis la racine du dépôt, installez l'agent partagé dans le dépôt Maven local :

```bash
mvn -B -pl sap-ccv2-devops-mcp-client-common -am install
```

Démarrez ensuite le serveur, puis un ou les deux clients dans des terminaux séparés :

```bash
cd sap-ccv2-devops-mcp-server
mvn spring-boot:run
```

```bash
cd sap-ccv2-devops-mcp-client-telegram
mvn spring-boot:run
```

```bash
cd sap-ccv2-devops-mcp-client-slack
mvn spring-boot:run
```

## Outils MCP exposés

| Outil | Action |
|---|---|
| Create SAP Commerce Cloud Build | Lance un build |
| Get SAP Commerce Cloud Builds | Liste les builds |
| Get SAP Commerce Cloud Build Details | Récupère la branche et les propriétés du build à partir de son code |
| Get SAP Commerce Cloud Build Progress | Suit la progression d'un build |
| Create SAP Commerce Cloud Deployment | Lance un déploiement |
| Get SAP Commerce Cloud Deployments | Liste les déploiements, avec filtres par environnement et statut |
| Get SAP Commerce Cloud Deployed Build | Donne le code du build, la branche source et la date du dernier déploiement d'un environnement |

Depuis Slack ou Telegram, demandez par exemple « Quelle branche est déployée en prod, et depuis quand ? ». L'outil retrouve le `buildCode` du dernier déploiement `DEPLOYED` de l'environnement, puis consulte le détail du build pour obtenir sa branche. La date provient du déploiement, et le commit est indiqué uniquement si SAP le fournit dans les propriétés du build.

## Tests et qualité

Depuis la racine, les modules se compilent et se testent ensemble avec :

```bash
mvn -B verify
```

- Tests unitaires JUnit 5 et Mockito.
- Couverture Jacoco avec un seuil minimal de 80 % sur les lignes pour les modules Telegram et serveur (le modèle de données `server/model` est exclu côté serveur).
- Rapports : `target/surefire-reports/` (tests) et `target/site/jacoco/` (couverture).

## CI sur les Pull Requests

Le workflow [pull-request.yml](./.github/workflows/pull-request.yml) s'exécute sur chaque PR, pour le serveur et les deux clients :

1. Build Maven avec les dépendances du module (`-pl <module> -am`) : `verify` pour les PR de forks, `install` pour les PR du même dépôt. Les deux exécutent les tests et les contrôles de couverture configurés.
2. Analyse SonarCloud lancée depuis le dossier du module, sans filtrer le réacteur Maven, pour les PR du même dépôt.
3. Publication des résultats JUnit dans la PR (module serveur, PR depuis le même dépôt).

Secret GitHub requis pour l'analyse Sonar : `SONAR_TOKEN`. L'organisation SonarCloud (`yelomarialaoui`) est définie directement dans le workflow.

## Sécurité

- Ne commitez jamais `secrets.properties`. Seuls les fichiers `.example` sont versionnés.
- Si un secret a été exposé (logs, captures, historique de chat), faites-le tourner immédiatement.