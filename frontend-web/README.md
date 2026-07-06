# Frontend web ACHABITATION

Ce dossier contient l'interface web locale séparée du backend.

Le frontend est actuellement une application HTML/CSS/JavaScript sans framework. Elle appelle l'API REST exposée par `backend-api`.

## Lancement local

Terminal 1 : lancer l'API.

```bash
cd ../backend-api
mvn spring-boot:run
```

Terminal 2 : servir le frontend web.

Windows :

```bat
run-web.bat
```

Linux/macOS :

```bash
./run-web.sh
```

Puis ouvrir :

```text
http://localhost:5173
```

## Configuration API

Par défaut, le frontend appelle :

```text
http://localhost:8080/api/v1
```

La constante est définie dans `app.js` via `API_BASE_URL`. Elle peut être surchargée par :

```js
window.ACHABITATION_API_BASE_URL
```

ou par le localStorage :

```js
localStorage.setItem("achabitation.apiBaseUrl", "http://localhost:8080/api/v1")
```

## Règle d'architecture

Le frontend ne doit pas contenir de logique métier critique. Les validations ergonomiques peuvent exister côté écran, mais les règles d'autorité restent dans `backend-api`.

## Parcours d'ajout de soi-même

Depuis l'onglet **Participant·es**, le bouton **+ M’ajouter moi-même** appelle l'API `POST /api/v1/trips/{tripId}/persons/current-user`.

Après avoir rejoint un voyage avec un code d'invitation, l'interface affiche aussi un bloc de rattachement permettant :

- de choisir un guest existant avec **C’est moi** ;
- ou de créer directement une nouvelle personne liée au compte connecté.

Ce parcours évite de devoir créer manuellement un guest puis de le lier au compte.
