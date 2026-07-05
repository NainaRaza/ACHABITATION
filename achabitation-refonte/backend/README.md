# Backend ACHABITATION

API Spring Boot destinée à remplacer progressivement l'application locale.

## Lancement

```bash
mvn spring-boot:run
```

API : `http://localhost:8080/api/v1`

## Notes

- Base H2 locale en développement.
- PostgreSQL déjà ajouté comme dépendance runtime pour la cible cloud.
- Authentification par token de session local sur les routes sensibles.
- Le moteur métier est dans `fr.achabitation.domain` et reste indépendant de Spring.

## Tests

Les tests automatisés sont documentés dans `TESTS.md`.

Commande principale :

```bash
mvn test
```


## Interface web locale

Une première interface web locale est disponible sur la nouvelle architecture.

Lancer le backend :

```powershell
mvn spring-boot:run
```

Puis ouvrir :

```text
http://localhost:8080/app
```

Documentation dédiée : `../docs/INTERFACE_WEB_LOCALE.md`.

## Profil production expérimental

Le profil local reste basé sur H2. Le profil `prod` utilise PostgreSQL et Flyway :

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/achabitation"
$env:DATABASE_USER="achabitation"
$env:DATABASE_PASSWORD="achabitation"
mvn spring-boot:run
```

Pour Docker :

```bash
docker compose up --build
```


## Validation bêta

Depuis `achabitation-refonte/backend` :

```bash
mvn clean test
mvn spring-boot:run
```

Depuis `achabitation-refonte` dans un second terminal :

```powershell
.\scripts\smoke-test.ps1
```

ou :

```bash
./scripts/smoke-test.sh
```
