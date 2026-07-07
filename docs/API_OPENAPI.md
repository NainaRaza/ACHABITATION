# Documentation API / OpenAPI

## Statut

La génération OpenAPI est préparée côté backend avec Springdoc.

Endpoints locaux attendus :

```text
GET /v3/api-docs
GET /swagger-ui.html
```

En profil `prod`, `application-prod.yml` désactive Swagger UI et `/v3/api-docs` par défaut. L'objectif est d'éviter d'exposer le contrat API publiquement par inadvertance.

## Commande d'export conseillée

Lancer le backend en local ou en profil test, puis exporter le contrat :

```bash
curl http://localhost:8080/v3/api-docs -o docs/openapi.json
```

Le fichier `docs/openapi.json` peut ensuite être versionné au moment d'un jalon stable, ou publié comme artefact CI.

## Endpoints nouvellement documentés au jalon P1

```text
PUT    /api/v1/auth/password
GET    /api/v1/auth/export
DELETE /api/v1/auth/account
```

## Limites

La génération n'a pas été validée dans cet environnement, car `./mvnw clean test` et le téléchargement des dépendances Maven sont bloqués par l'accès réseau. Le câblage est néanmoins présent dans le `pom.xml` et les profils applicatifs.

## Auth production ajoutée après retour équipe

Endpoints multi-session :

```text
GET    /api/v1/auth/sessions
DELETE /api/v1/auth/sessions/{sessionId}
DELETE /api/v1/auth/sessions
```

Endpoints reset mot de passe :

```text
POST /api/v1/auth/password/reset-request
POST /api/v1/auth/password/reset
```

Le token de reset est hashé en base, expire en 30 minutes et est à usage unique. L'envoi du jeton brut doit être fourni par un adaptateur SMTP/service email externe avant production publique.
