# Sauvegarde et restauration PostgreSQL

Ce document décrit une procédure minimale pour une bêta privée utilisant PostgreSQL via `infra/docker-compose.yml`.

## Préconditions

- Les variables `DATABASE_USER`, `DATABASE_PASSWORD` et `DATABASE_URL` doivent être définies dans un fichier `.env` local non versionné.
- Les sauvegardes doivent être stockées hors du dépôt Git.
- Toute sauvegarde contenant des données réelles doit être considérée comme sensible.

## Sauvegarde locale

Depuis la racine du dépôt :

```bash
mkdir -p backups
source .env

docker compose -f infra/docker-compose.yml exec -T postgres \
  pg_dump -U "$DATABASE_USER" -d achabitation --format=custom --no-owner --no-acl \
  > "backups/achabitation-$(date +%Y%m%d-%H%M%S).dump"
```

## Restauration locale

La restauration remplace le contenu de la base cible. À utiliser uniquement sur un environnement contrôlé.

```bash
source .env
BACKUP_FILE="backups/achabitation-YYYYMMDD-HHMMSS.dump"

docker compose -f infra/docker-compose.yml exec -T postgres \
  psql -U "$DATABASE_USER" -d achabitation -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

docker compose -f infra/docker-compose.yml exec -T postgres \
  pg_restore -U "$DATABASE_USER" -d achabitation --clean --if-exists --no-owner --no-acl \
  < "$BACKUP_FILE"
```

## Règles de sécurité minimales

- Ne jamais commiter `.env`, `*.dump`, `*.sql`, `*.backup` ou une archive de sauvegarde.
- Chiffrer les sauvegardes si elles sortent de la machine locale.
- Tester une restauration avant toute utilisation avec des données réelles.
- Définir une durée de conservation courte pour les données de bêta privée.

## Reste à renforcer avant production

- Sauvegardes automatiques planifiées.
- Chiffrement au repos.
- Journal de restauration.
- Test de restauration régulier.
- Procédure de purge des sauvegardes contenant des données supprimées par un utilisateur.


## Scripts ajoutés

Deux scripts minimaux sont fournis à la racine du projet :

```bash
DATABASE_URL=postgresql://user:password@localhost:5432/achabitation ./scripts/backup-postgres.sh
DATABASE_URL=postgresql://user:password@localhost:5432/achabitation ./scripts/restore-postgres.sh backups/achabitation-YYYYMMDDTHHMMSSZ.dump
```

Les scripts n’intègrent aucun secret. Les identifiants doivent venir de l’environnement d’exécution ou d’un gestionnaire de secrets.
