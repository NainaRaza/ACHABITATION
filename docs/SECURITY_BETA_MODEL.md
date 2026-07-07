# Modèle de sécurité bêta

Ce document décrit le modèle de sécurité actuellement implémenté. Il est adapté à une bêta locale ou fermée, pas à une production publique sans durcissement supplémentaire.

## Authentification

L’authentification utilise une session opaque stockée serveur sous forme hashée.

Flux web :

1. l’utilisateur crée un compte ou se connecte ;
2. le backend génère un token brut aléatoire ;
3. le backend écrit `ACHABITATION_SESSION` en cookie `HttpOnly` avec attribut `SameSite` ;
4. si le client envoie `X-Achabitation-Client: web`, le champ `accessToken` de la réponse JSON est masqué ;
5. les requêtes mutantes web utilisent le cookie de session et le jeton CSRF `XSRF-TOKEN` / `X-XSRF-TOKEN`.

Flux Android, scripts et clients non navigateur :

```http
Authorization: Bearer <accessToken>
```

Le filtre accepte aussi `X-Session-Token`, mais il ne doit pas être privilégié dans les nouveaux clients. Le filtre valide le token, qu’il provienne du cookie, du header Bearer ou du header transitoire, puis alimente le `SecurityContext`.

## Stockage du token

Côté backend :

```text
app_user.session_token_hash
app_user.session_token_issued_at
```

Le token brut n’est pas stocké en base.

Côté web : le token n’est plus stocké dans `localStorage`. Le frontend conserve seulement les informations non sensibles de compte et s’appuie sur le cookie `HttpOnly`.

Côté Android : token et informations de session stockés via `EncryptedSharedPreferences`.

## Durée de validité

Le filtre considère une session valide pendant 30 jours à partir de `session_token_issued_at`. Cette durée doit devenir configurable avant production.

## Routes publiques

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/health/**
GET  /h2-console/** en développement local
```

Toutes les autres routes `/api/v1/**` exigent une authentification.

## Logout

```http
POST /api/v1/auth/logout
```

Le logout supprime le hash de session côté serveur. Le web et Android doivent appeler cet endpoint avant de nettoyer leur stockage local. Android conserve la session locale si l’appel serveur échoue pour une raison réseau, afin d’éviter une fausse déconnexion côté client alors que le token reste valide côté serveur.

## Rate limiting

`LoginRateLimiter` protège `register` et `login` :

```text
12 tentatives par IP et par action sur 10 minutes
```

Le rate limiting est mémoire. Il n’est pas partagé entre plusieurs instances et ne doit pas être considéré comme suffisant en production.

## Autorisations métier

Les rôles de voyage sont : `OWNER`, `ADMIN`, `PARTICIPANT`, `READ_ONLY`.

Règles actuelles : lecture pour les membres, écriture pour `OWNER/ADMIN/PARTICIPANT`, administration pour `OWNER/ADMIN`, `READ_ONLY` en lecture seule.

Le backend vérifie aussi l’appartenance au voyage, l’appartenance d’une personne au voyage, l’appartenance d’une dépense au voyage, l’unicité de liaison compte/personne et l’interdiction de reprendre une personne liée à un autre compte.

## Confidentialité du RAV

Une personne liée à un compte peut avoir `livingRestPublic=false`.

Dans ce cas, pour les autres membres :

```text
livingRest = null
livingRestHidden = true
```

Le backend continue d’utiliser le RAV réel pour les calculs. Il faut donc préciser à l’utilisateur que le RAV privé n’est pas affiché directement, mais qu’il peut parfois être inféré indirectement à partir des soldes.

## CORS

Origines autorisées en développement :

```text
http://localhost:5173
http://127.0.0.1:5173
```

Le header `Content-Disposition` est exposé pour permettre le téléchargement des exports CSV côté web.

## Android

La variante debug autorise le HTTP local pour `10.0.2.2`, `localhost` et `127.0.0.1`.

La variante release définit `usesCleartextTraffic=false` et le ViewModel/API refuse les URL `http://` en release. Le logout Android appelle `POST /api/v1/auth/logout`; une réponse 401 nettoie la session locale et ramène vers la connexion.

## Limites avant production

- Pas de refresh token.
- Pas de rotation avancée de session.
- Gestion multi-session existante, mais UX et supervision à compléter.
- Reset de mot de passe présent.
- Vérification email présente.
- Rate limiting non distribué.
- CORS non externalisé par environnement.
- Durée de session non configurable.
- Logs de sécurité à structurer.
- Pas encore d’audit RGPD complet.
