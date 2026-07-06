# ACHABITATION - Gestion finance - partage au RAV

Ce dépôt contient la refonte d'architecture du projet.

L'objectif n'est plus seulement d'avoir une application Java desktop locale, mais de préparer une architecture compatible avec :

- plusieurs voyages séparés ;
- comptes utilisateurs ;
- authentification ;
- synchronisation cloud ;
- application mobile ;
- export PDF / Excel ;
- import de tickets ;
- OCR ;
- historique détaillé ;
- multi-devises ;
- plusieurs périodes de présence par personne.

## Structure

```text
achabitation-refonte/
├── backend/          API Spring Boot, base future du cloud et du mobile
├── desktop-legacy/   ancienne application Swing conservée comme référence fonctionnelle
└── docs/             spécifications et notes d'architecture
```

## Backend

Le backend est une API Java Spring Boot organisée en couches :

```text
api/             contrôleurs REST et DTO
application/     services applicatifs
config/          configuration sécurité et infrastructure
domain/          moteur métier pur, indépendant de Spring
infrastructure/  entités JPA et repositories
```

Le moteur de calcul RAV est dans `backend/src/main/java/fr/achabitation/domain`.
Il ne dépend pas de Spring. C'est volontaire : il pourra être testé isolément et réutilisé plus tard par une application mobile, un backend cloud ou une interface desktop.

## Lancer le backend

Prérequis :

- JDK 21 ;
- Maven 3.9+.

```bash
cd backend
mvn spring-boot:run
```

Par défaut :

```text
API : http://localhost:8080/api/v1
H2 console : http://localhost:8080/h2-console
```

Base de données locale de développement :

```text
./backend/data/achabitation.mv.db
```

## Statut de la sécurité

La structure utilisateur/authentification existe déjà :

- `UserEntity`
- `/api/v1/auth/register`
- `/api/v1/auth/login`
- hachage des mots de passe avec BCrypt

Le backend impose maintenant un token de session local sur les routes sensibles. Ce token permet une bêta fermée, mais devra être remplacé par une authentification durcie avant production publique : JWT/session sécurisée, reset de mot de passe, renouvellement, révocation et supervision.

## Ancienne application

L'ancienne version Swing est conservée dans `desktop-legacy/`.
Elle reste utile pour comparer le comportement fonctionnel et éviter de perdre les règles métier déjà validées.


## Interface graphique de la nouvelle architecture

Le backend Spring Boot sert maintenant une première interface web locale.

```powershell
cd backend
mvn spring-boot:run
```

Puis ouvrir :

```text
http://localhost:8080/app
```

Voir `docs/INTERFACE_WEB_LOCALE.md`.


## Documentation V1 ajoutée

- `docs/V1_AUTH_PROFIL_CONTRAINTES.md` : règles d’authentification locale, profil RAV, guests, liaison compte et contraintes de voyage.

## Correctifs V1 / pré-production

La refonte contient maintenant une première couche de durcissement :

- accès par token de session ;
- droits par voyage ;
- rôles `OWNER`, `ADMIN`, `PARTICIPANT`, `READ_ONLY` ;
- invitations avec code expirant ;
- accès aux voyages limité aux membres ;
- exports CSV ;
- profil PostgreSQL/Flyway pour la production ;
- Dockerfile backend et docker-compose PostgreSQL ;
- documentation dédiée dans `docs/PROD_READY_CHECKLIST.md`.


## Validation bêta

Pour valider une version bêta fermée :

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Dans un second terminal :

```powershell
.\scripts\smoke-test.ps1
```

ou :

```bash
./scripts/smoke-test.sh
```

Voir `docs/MVP_BETA_CHECKLIST.md`.
