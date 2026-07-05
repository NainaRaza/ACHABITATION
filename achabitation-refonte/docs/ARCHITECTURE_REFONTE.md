# Architecture cible - ACHABITATION

## Objectif

La refonte transforme l'application locale en une architecture compatible avec un produit cloud/mobile.

L'ancien modèle Swing + fichier `.ser` était suffisant pour un MVP local, mais insuffisant pour :

- plusieurs voyages ;
- plusieurs utilisateurs ;
- synchronisation cloud ;
- historique robuste ;
- application mobile ;
- tickets / OCR ;
- multi-devises.

La nouvelle architecture place le backend au centre.

```text
Mobile / Web / Desktop
        ↓
API REST Spring Boot
        ↓
Services applicatifs
        ↓
Moteur de calcul métier
        ↓
Base SQL : H2 en dev, PostgreSQL en cible
```

## Choix technique

Backend : Spring Boot + Java 21.

Persistance : JPA/Hibernate.

Base de développement : H2 fichier.

Base cible : PostgreSQL.

Sécurité cible : Spring Security + JWT ou session sécurisée.

## Couches

### api

Expose les routes REST.

Rôle :

- recevoir les requêtes ;
- valider les DTO ;
- retourner les réponses JSON ;
- ne pas contenir de logique métier lourde.

### application

Orchestre les cas d'utilisation.

Exemples :

- créer un voyage ;
- ajouter une personne ;
- créer une dépense ;
- calculer un résumé ;
- écrire un audit log.

### domain

Contient les règles métier pures.

C'est la couche la plus importante. Elle ne dépend pas de Spring, de JPA ou d'une base de données.

Contenu actuel :

- `DomainPerson`
- `DomainExpense`
- `PresencePeriod`
- `Balance`
- `Settlement`
- `BalanceCalculator`

### infrastructure

Contient les détails techniques :

- entités JPA ;
- repositories Spring Data ;
- base SQL ;
- modèle de persistance.

## Fonctionnalités préparées

### Plusieurs voyages séparés

Entité : `TripEntity`.

Toutes les personnes et dépenses sont rattachées à un `trip_id`.

### Comptes utilisateurs

Entité : `UserEntity`.

Les utilisateurs sont séparés des personnes d'un voyage. C'est important : une personne dans un voyage peut représenter un ami, même s'il n'a pas encore de compte.

### Membres du voyage

Entité : `TripMemberEntity`.

Rôles prévus :

- `OWNER`
- `ADMIN`
- `PARTICIPANT`
- `READ_ONLY`

### Plusieurs périodes de présence

Entité : `PresencePeriodEntity`.

Une personne peut avoir plusieurs périodes de présence. Les périodes sont inclusives. Elles doivent respecter trois contraintes métier :

- chaque période doit avoir une date de début et une date de fin ;
- chaque période doit être comprise dans les dates du voyage ;
- deux périodes d'une même personne ne doivent pas se chevaucher, y compris sur une borne commune.

Exemple :

```text
Présent du 01/08 au 04/08
Absent du 05/08 au 07/08
Présent du 08/08 au 11/08
```

### Dépenses

Entité : `ExpenseEntity`.

La date d'une dépense doit être comprise dans les dates du voyage. Cette règle s'applique aussi aux dépenses globales, même si leur répartition ignore les dates de présence individuelles.

Modes prévus :

- dépense normale ;
- dépense globale ;
- mode avancé avec participant·es manuel·les.

### Multi-devises

Chaque dépense a :

- une devise de saisie ;
- un taux de conversion vers la devise du voyage.

Le taux est stocké au moment de la dépense pour éviter que l'historique change si les taux évoluent.

### Historique détaillé

Entité : `AuditLogEntity`.

Actions préparées :

- création de voyage ;
- création/modification/désactivation d'une personne ;
- création/modification/suppression d'une dépense.

### Authentification

Le modèle utilisateur et les endpoints existent.

État bêta : les routes sensibles passent par un token de session local et les droits sont vérifiés par voyage. Avant production publique, il faudra remplacer ce mécanisme par une authentification durcie avec révocation, renouvellement et supervision.

### Mobile et cloud

Le mobile n'est pas développé dans cette refonte, mais l'architecture est compatible : l'application mobile pourra consommer l'API REST.

## Prochaines étapes techniques

1. Ajouter des tests unitaires sur `BalanceCalculator`.
2. Ajouter une vraie sécurité JWT.
3. Ajouter les droits par voyage sur chaque endpoint.
4. Ajouter l'export CSV/XLSX.
5. Ajouter l'export PDF.
6. Ajouter un module d'import ticket manuel.
7. Ajouter OCR ensuite, pas avant.
