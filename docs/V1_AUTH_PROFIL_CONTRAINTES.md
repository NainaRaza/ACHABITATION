# V1 — Authentification, profil utilisateur, guests et contraintes de voyage

## Objectif

Cette évolution prépare la V1 autour de quatre règles métier :

1. Les contraintes personnalisées sont déclarées au niveau du voyage.
2. Un compte utilisateur porte son profil RAV et ses contraintes personnelles.
3. Une personne de voyage peut rester un guest, ou être liée à un compte utilisateur.
4. Quand une personne est liée à un compte, seul ce compte peut modifier son profil financier et ses contraintes personnelles.

## Contraintes personnalisées liées au voyage

Avant, chaque personne pouvait saisir librement ses contraintes. Cela permettait des doublons sémantiques entre plusieurs libellés proches.

Désormais, le voyage possède une liste officielle de contraintes acceptées :

```text
TRIP.customConstraints
```

Une personne ne peut cocher que les contraintes déclarées sur le voyage. Une dépense ne peut affecter un montant spécifique qu’à une contrainte déclarée sur le voyage.

Une normalisation minimale existe déjà :

```text
Les contraintes ne sont pas traduites automatiquement : le voyage impose une liste officielle commune.
```

La normalisation générique ignore aussi la casse, les accents et les espaces multiples.

## Profil utilisateur

Le profil utilisateur contient :

```text
- email
- nom affiché
- reste à vivre
- mode RAV ou moyenne
- calcul avancé du RAV
- végétarien
- sans alcool
- contraintes personnelles cochées
- RAV public ou privé
```

Le champ `livingRestPublic` permet à l’utilisateur de choisir si son RAV est affiché aux autres.

Même si le RAV est privé, il reste utilisé dans les calculs. Les autres peuvent donc théoriquement l’inférer par calcul, mais il n’est pas affiché directement dans l’interface ni renvoyé clairement par l’API de liste des personnes.

## Guests et liaison à un compte

On peut toujours créer une personne manuellement dans un voyage. Cette personne est un `guest`.

Une personne connectée peut ensuite rejoindre le voyage et se lier à un guest existant. Dans ce cas :

```text
PERSON.linkedUser = APP_USER.id
```

La liaison ne remplace pas automatiquement les données financières et contraintes du guest. L’application peut proposer une confirmation si le profil contient un RAV non nul. Le remplacement n’a lieu que si la requête envoie explicitement `applyProfileToGuest = true`.

Un compte ne peut être lié qu’à une seule personne par voyage. Une personne déjà liée à un autre compte ne peut pas être reprise.

## Authentification V1

L’authentification reste volontairement simple :

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Le backend renvoie un `devToken` stocké côté serveur. L’interface web l’envoie ensuite dans :

```http
Authorization: Bearer <devToken>
```

Ce n’est pas encore un JWT de production. C’est une étape intermédiaire propre pour imposer les règles de propriété en local.

## Endpoints ajoutés

```http
GET /api/v1/auth/profile
PUT /api/v1/auth/profile
PUT /api/v1/trips/{tripId}/constraints
POST /api/v1/trips/{tripId}/join
POST /api/v1/trips/{tripId}/persons/{personId}/link-current-user
```

## Règles de sécurité métier

- Une contrainte personnelle doit exister dans le voyage pour être cochée sur une personne du voyage.
- Une dépense ne peut pas utiliser une contrainte qui n’existe pas dans le voyage.
- Une personne liée à un compte cache son RAV si `livingRestPublic = false` et si la personne qui lit n’est pas le compte lié.
- Le calcul continue d’utiliser le RAV réel côté backend.
- Le remplacement d’un guest par un compte applique le profil utilisateur au guest.
- Un compte ne peut pas remplacer deux guests dans le même voyage.

## Limites restantes

- Le token actuel est un token de développement, pas un JWT production.
- Il n’y a pas encore de rôles complets OWNER / ADMIN / PARTICIPANT appliqués à toutes les routes.
- La liste des voyages reste visible en mode développement.
- La normalisation sémantique des contraintes reste limitée à quelques alias simples.

## Mise à jour V1 — profil et propagation explicite

Le profil utilisateur n'est plus un onglet de voyage. Il est accessible depuis la barre supérieure via le bouton **Profil / compte**, au même niveau que l'utilisateur connecté.

La modification du profil RAV est désormais dissociée des voyages déjà liés. Enregistrer le profil met uniquement à jour le profil central de l'utilisateur. Les personnes liées dans les voyages conservent leurs anciennes valeurs tant que l'utilisateur ne choisit pas explicitement les voyages à mettre à jour.

Workflow retenu :

1. L'utilisateur ouvre **Profil / compte**.
2. Il modifie son profil RAV, ses contraintes, son statut végétarien / sans alcool ou la confidentialité du RAV.
3. Il enregistre le profil.
4. L'application affiche la liste des voyages/personnes déjà liés à ce compte.
5. L'utilisateur coche les voyages/personnes auxquels appliquer le nouveau profil.
6. Le backend applique le profil uniquement aux personnes sélectionnées.

Endpoint ajouté :

```http
POST /api/v1/auth/profile/apply-to-linked-persons
Authorization: Bearer <token>
Content-Type: application/json

{
  "personIds": ["<uuid-personne-liée>"]
}
```

Le backend refuse toute personne non liée au compte authentifié.
