# Interface web locale

L’interface web est maintenant séparée du backend.

Elle se trouve dans :

```text
frontend-web/
├── index.html
├── app.js
├── styles.css
├── run-web.bat
└── run-web.sh
```

Le backend API se trouve dans :

```text
backend-api/
```

## Lancement local

Terminal 1 : lancer le backend API.

```bash
cd achabitation-refonte/backend-api
mvn spring-boot:run
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

Par défaut, `frontend-web/app.js` appelle :

```text
http://localhost:8080/api/v1
```

La configuration est au début de `app.js` :

```js
const API_BASE_URL = window.ACHABITATION_API_BASE_URL
    || localStorage.getItem("achabitation.apiBaseUrl")
    || "http://localhost:8080/api/v1";
```

Cela permet de changer l’URL API sans modifier toute l’application.

## CORS

Comme le frontend tourne sur `localhost:5173` et l’API sur `localhost:8080`, le backend doit autoriser les appels cross-origin.

La configuration CORS est dans :

```text
backend-api/src/main/java/fr/achabitation/config/SecurityConfig.java
```

Origines autorisées en développement :

```text
http://localhost:5173
http://127.0.0.1:5173
```

## Rôle de l’interface

Le frontend web sert à manipuler l’application depuis un navigateur :

- créer ou connecter un compte ;
- créer et rejoindre un voyage ;
- gérer les participant·es ;
- gérer les contraintes du voyage ;
- ajouter les dépenses ;
- consulter le résumé ;
- exporter les CSV ;
- consulter l’historique.

Le frontend peut faire des validations ergonomiques, mais les règles critiques restent côté backend.

Exemples de règles qui doivent rester côté backend :

- droits d’accès ;
- appartenance au voyage ;
- unicité des noms ;
- cohérence des dates ;
- validation des périodes de présence ;
- validation des montants ;
- confidentialité du RAV ;
- calculs de soldes.

## Pourquoi cette séparation

Avant la refonte, Spring Boot servait directement les fichiers statiques sur `/app`. C’était acceptable pour un POC, mais moins clair pour une architecture multi-client.

Désormais :

```text
frontend-web  →  backend-api  →  base de données
mobile-android → backend-api  →  base de données
mobile-ios     → backend-api  →  base de données
```

La même API pourra donc être utilisée par l’interface web, Android et iOS.

## Parcours après invitation : choisir son identité dans le voyage

Après avoir rejoint un voyage avec un code d’invitation, l’interface affiche un bloc de rattachement.

L’utilisateur connecté peut alors :

- choisir un guest existant et cliquer sur **C’est moi** ;
- ou choisir **Autre personne : me créer dans ce voyage**.

Dans le premier cas, le compte est lié au guest choisi. Si le profil utilisateur contient un RAV exploitable, l’interface demande si le profil doit être appliqué au guest. Sans confirmation, les données existantes du guest sont conservées.

Dans le second cas, l’application crée une nouvelle personne directement liée au compte connecté. Par défaut, la période de présence proposée est l’ensemble du voyage. Si le profil utilisateur est exploitable, l’interface propose de créer la personne avec ce profil ; sinon elle crée la personne en mode moyenne, sans RAV personnel.

Le même ajout direct existe dans l’onglet **Participant·es** via le bouton **+ M’ajouter moi-même**. Cela évite le parcours lourd : créer un guest, puis le lier manuellement au compte.
