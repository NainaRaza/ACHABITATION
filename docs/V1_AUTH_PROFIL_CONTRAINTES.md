# V1 — Authentification, profil utilisateur, guests et contraintes de voyage

## Objectif

La V1 repose sur quatre règles métier :

1. Les contraintes personnalisées sont déclarées au niveau du voyage.
2. Un compte utilisateur porte son profil RAV et ses contraintes personnelles.
3. Une personne de voyage peut rester un guest ou être liée à un compte utilisateur.
4. Quand une personne est liée à un compte, son profil financier ne doit pas être modifié librement par n’importe quel membre.

## Contraintes personnalisées liées au voyage

Le voyage possède une liste officielle de contraintes acceptées :

```text
TripEntity.customConstraints
```

Une personne ne peut cocher que les contraintes déclarées sur le voyage. Une dépense ne peut affecter un montant spécifique qu’à une contrainte déclarée sur le voyage.

La normalisation ignore la casse, les accents et les espaces multiples. Elle ne fait pas de traduction ni de synonymie métier automatique.

## Profil utilisateur

Le profil utilisateur contient :

```text
email
nom affiché
reste à vivre
mode LIVING_REST ou AVERAGE
calcul avancé du RAV
revenus et charges
végétarien
sans alcool
contraintes personnelles
RAV public ou privé
```

Le champ `livingRestPublic` indique si le RAV est affiché aux autres membres.

Même si le RAV est privé, il reste utilisé dans les calculs. Les autres peuvent donc parfois l’inférer indirectement, mais il n’est pas affiché directement dans l’interface ni renvoyé clairement par l’API de liste des personnes.

## Guests et liaison à un compte

On peut créer une personne manuellement dans un voyage. Cette personne est un guest.

Une personne connectée peut rejoindre le voyage et se lier à un guest existant :

```text
PersonEntity.linkedUser = UserEntity.id
```

La liaison ne remplace pas automatiquement les données financières et contraintes du guest. Le remplacement n’a lieu que si la requête envoie explicitement `applyProfileToGuest=true`.

Un compte ne peut être lié qu’à une seule personne par voyage. Une personne déjà liée à un autre compte ne peut pas être reprise.

## Ajout direct du compte comme personne

Le backend expose :

```http
POST /api/v1/trips/{tripId}/persons/current-user
```

Cette route crée une personne directement liée au compte authentifié.

Si `applyProfileToPerson=true`, le profil utilisateur est copié vers la personne. Sinon, la personne est créée en mode `AVERAGE`, ce qui permet un ajout valide même si le profil ne contient pas encore de RAV exploitable.

## Authentification V1

Routes publiques :

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Le backend génère une session opaque. Pour le web, le token est transmis en cookie `ACHABITATION_SESSION` `HttpOnly` et le champ `accessToken` est masqué lorsque la requête contient `X-Achabitation-Client: web`. Pour Android et les scripts, le backend peut encore renvoyer un `accessToken` opaque à envoyer ensuite dans :

```http
Authorization: Bearer <accessToken>
```

Le token brut n’est pas stocké en base. Seul son hash SHA-256 est conservé côté serveur.

Déconnexion :

```http
POST /api/v1/auth/logout
```

## Endpoints V1 principaux

```http
GET  /api/v1/auth/profile
PUT  /api/v1/auth/account
PUT  /api/v1/auth/profile
POST /api/v1/auth/profile/apply-to-linked-persons
PUT  /api/v1/trips/{tripId}/constraints
POST /api/v1/trips/{tripId}/join
POST /api/v1/trips/join-by-code
POST /api/v1/trips/{tripId}/persons/current-user
POST /api/v1/trips/{tripId}/persons/{personId}/link-current-user
```

## Règles de sécurité métier

- Une contrainte personnelle doit exister dans le voyage pour être cochée sur une personne du voyage.
- Une dépense ne peut pas utiliser une contrainte qui n’existe pas dans le voyage.
- Une personne liée à un compte cache son RAV si `livingRestPublic=false` et si la personne qui lit n’est pas le compte lié.
- Le calcul continue d’utiliser le RAV réel côté backend.
- Le remplacement d’un guest par un compte n’applique le profil utilisateur que sur confirmation explicite.
- Un compte ne peut pas remplacer deux guests dans le même voyage.
- `READ_ONLY` peut lire mais pas modifier.

## Propagation explicite du profil

La modification du profil central de l’utilisateur ne modifie pas automatiquement les voyages déjà liés.

Workflow retenu :

1. L’utilisateur ouvre le profil.
2. Il modifie son RAV, ses contraintes, son statut végétarien/sans alcool ou la confidentialité du RAV.
3. Il enregistre le profil central.
4. L’application affiche les personnes déjà liées à ce compte.
5. L’utilisateur choisit les personnes auxquelles appliquer le profil.
6. Le backend applique le profil uniquement aux personnes sélectionnées.

Endpoint :

```http
POST /api/v1/auth/profile/apply-to-linked-persons
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "personIds": ["<uuid-personne-liée>"]
}
```

Le backend refuse toute personne non liée au compte authentifié.

## Limites restantes

- La session actuelle est opaque et hashée en base. Le web utilise un cookie `HttpOnly`; Android et les scripts utilisent un token Bearer opaque. Ce n’est pas une architecture complète refresh/access token.
- Le rate limiting est en mémoire.
- La durée de session doit devenir configurable.
- La matrice de droits doit continuer à être testée à chaque nouvelle route.
- La normalisation des contraintes reste volontairement neutre : espaces, casse et accents, sans synonymes métier imposés.

## Clarification UI — contraintes de voyage

Depuis la correction V6.3, l'ajout d'une contrainte personnalisée ne doit plus être découvert uniquement depuis le formulaire personne.

Comportement attendu :

- Web : les contraintes peuvent être saisies dès la création du voyage, puis administrées dans `Paramètres du voyage > Contraintes du voyage`.
- Android : les contraintes sont administrées dans l'écran `Vue` du voyage, carte `Contraintes du voyage`, avec ajout unitaire et édition de la liste complète.
- Personnes : les contraintes déclarées sur le voyage sont affichées comme cases à cocher. Un utilisateur ne saisit plus librement `Vegan, PMR` dans le formulaire personne mobile.
- Dépenses : les options RAV affichent un champ montant par contrainte déclarée sur le voyage. Le libellé de type `Contraintes : Vegan=12, PMR=5` était uniquement un exemple de saisie technique et a été retiré de l'interface mobile.

Rappel métier : lorsqu'une dépense contient une part positive pour une contrainte, cette part est répartie entre les personnes actives qui ne portent pas cette contrainte.
