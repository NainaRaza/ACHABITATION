# Observabilité minimale

## Statut

Une première couche d'observabilité backend est ajoutée.

## Corrélation des requêtes

Chaque requête HTTP reçoit un identifiant `X-Request-ID` :

- si le client fournit déjà `X-Request-ID`, il est repris ;
- sinon, le backend génère un UUID ;
- la réponse expose le même en-tête ;
- l'identifiant est placé dans le MDC sous la clé `requestId`.

## Logs

Le fichier `backend-api/src/main/resources/logback-spring.xml` produit un format homogène incluant :

```text
timestamp, level, app, requestId, thread, logger, message
```

Objectif : retrouver une erreur utilisateur sans journaliser de token, de mot de passe, de RAV détaillé ou de données financières sensibles.

## Health checks

Endpoints existants :

```text
GET /api/v1/health
GET /api/v1/health/readiness
```

`/readiness` vérifie l'accès base via `SELECT 1`.

## À compléter avant exposition publique

- métriques HTTP 4xx/5xx ;
- latence par endpoint ;
- alerting minimal ;
- centralisation des logs ;
- sampling ou masquage strict des champs sensibles si un outil externe est branché.

## Métriques production

Spring Boot Actuator et Prometheus sont activés côté dépendances/configuration. Endpoints techniques attendus :

```text
GET /actuator/health
GET /actuator/prometheus
GET /actuator/metrics
```

Ils doivent être protégés au niveau réseau/reverse proxy en production. L'alerting réel reste à brancher dans l'infrastructure cible.

## Healthchecks production

Endpoints disponibles :

```text
GET /api/v1/health/liveness
GET /api/v1/health/readiness
```

`readiness` vérifie la connexion base de données. L'alerting externe reste à brancher dans l'infrastructure cible.
