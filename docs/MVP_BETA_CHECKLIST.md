# ACHABITATION — Checklist MVP bêta

Ce document définit le niveau minimal attendu avant de faire tester l'application par un petit groupe de personnes réelles.

## Statut visé

Le projet est considéré comme prêt pour une bêta fermée lorsque les points suivants sont vrais :

- les tests automatisés passent avec `mvn clean test` ;
- le backend démarre en local avec H2 ;
- le backend démarre avec Docker + PostgreSQL ;
- le smoke test API passe ;
- l'interface web permet un scénario complet sans appel manuel à l'API ;
- les droits d'accès principaux sont bloqués côté backend ;
- aucune donnée financière privée n'est renvoyée directement à un autre compte ;
- les exports CSV fonctionnent ;
- les erreurs métier sont lisibles.

## Commandes de validation

Depuis `achabitation-refonte/backend-api` :

```bash
mvn clean test
mvn spring-boot:run
```

Dans un second terminal, depuis `achabitation-refonte` :

```powershell
.\scripts\smoke-test.ps1
```

ou sous Linux/macOS/Git Bash :

```bash
./scripts/smoke-test.sh
```

Pour tester PostgreSQL + Docker :

```bash
docker compose -f infra/docker-compose.yml up --build
```

Puis relancer le smoke test contre le backend Docker :

```bash
./scripts/smoke-test.sh http://localhost:8080/api/v1
```

## Scénario manuel minimal dans l'interface

Ouvrir :

```text
http://localhost:5173
```

Valider le scénario suivant :

1. créer un compte owner ;
2. créer un voyage ;
3. ajouter une contrainte de voyage ;
4. créer deux guests ;
5. créer une dépense normale avec viande, alcool et contrainte ;
6. créer une dépense globale ;
7. vérifier les personnes concernées ;
8. vérifier le résumé ;
9. exporter les CSV ;
10. créer une invitation ;
11. créer un second compte ;
12. rejoindre le voyage avec l'invitation ;
13. lier ce compte à un guest sans appliquer automatiquement le profil ;
14. modifier le profil ;
15. appliquer le profil explicitement au voyage lié ;
16. vérifier que le RAV privé est masqué pour l'autre compte ;
17. vérifier l'historique.

## Ce qui reste hors bêta

Les éléments suivants ne bloquent pas la bêta fermée mais bloquent une vraie production publique :

- reset de mot de passe par email ;
- suppression complète de compte et export RGPD complet ;
- supervision et alerting ;
- sauvegardes automatisées ;
- audit sécurité externe ;
- application mobile native ;
- OCR tickets de caisse ;
- multi-devises automatique via API de change.

## Critère de go / no-go

Go bêta si :

```text
mvn clean test = SUCCESS
smoke-test = SUCCESS
scénario manuel = SUCCESS
aucune erreur 500 pendant le parcours nominal
```

No-go si :

```text
un endpoint sensible répond sans token
un utilisateur non membre lit un voyage
un RAV privé apparaît dans une réponse destinée à un autre compte
un scénario nominal déclenche une erreur 500
les exports ne se téléchargent pas
```
