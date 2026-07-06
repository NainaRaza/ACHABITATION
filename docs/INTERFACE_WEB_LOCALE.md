# Interface web locale

L’interface web est séparée du backend. Elle se trouve dans `frontend-web/`. Le backend API se trouve dans `backend-api/`.

## Lancement local

Terminal 1 : lancer le backend API.

```bash
cd achabitation-refonte/backend-api
./mvnw spring-boot:run
```

Terminal 2 : lancer le frontend web.

Windows :

```bat
cd achabitation-refonte\frontend-web
run-web.bat
```

Linux/macOS :

```bash
cd achabitation-refonte/frontend-web
./run-web.sh
```

Puis ouvrir :

```text
http://localhost:5173
```

## URL API appelée par le frontend

Par défaut, le frontend appelle :

```text
http://localhost:8080/api/v1
```

La configuration est dans `frontend-web/src/api.js` :

```js
export const API_BASE_URL = window.ACHABITATION_API_BASE_URL
    || localStorage.getItem("achabitation.apiBaseUrl")
    || "http://localhost:8080/api/v1";
```

## CORS

Le backend autorise les appels cross-origin depuis :

```text
http://localhost:5173
http://127.0.0.1:5173
```

Configuration : `backend-api/src/main/java/fr/achabitation/config/SecurityConfig.java`.

## Rôle de l’interface

Le frontend permet de créer ou connecter un compte, modifier le compte et le profil RAV, créer/rejoindre un voyage, gérer les contraintes, invitations, participant·es, guests, dépenses, résumé, exports CSV et historique.

Le frontend peut faire des validations ergonomiques, mais les règles critiques restent côté backend : authentification, droits d’accès, appartenance au voyage, unicité des noms, cohérence des dates, montants, confidentialité du RAV et calculs.

## Pourquoi cette séparation

Spring Boot ne sert plus directement l’interface web sur `/app`. Le serveur expose l’API, et le frontend web est servi par un serveur local distinct.

Architecture cible :

```text
frontend-web    → backend-api → base de données
mobile-android  → backend-api → base de données
mobile-ios      → backend-api → base de données future
```

## Parcours après invitation

Après avoir rejoint un voyage avec un code d’invitation, l’utilisateur peut choisir un guest existant et le lier à son compte, ou créer une nouvelle personne directement liée à son compte.

Endpoint de liaison :

```http
POST /api/v1/trips/{tripId}/persons/{personId}/link-current-user
```

Endpoint d’ajout direct :

```http
POST /api/v1/trips/{tripId}/persons/current-user
```

## Tests frontend

```bash
cd frontend-web
./run-tests.sh
```

Sous Windows :

```bat
cd frontend-web
run-tests.bat
```
