# Tests automatisés ACHABITATION backend

## Commande principale

Depuis `achabitation-refonte/backend-api` :

```bash
mvn clean test
```

Sous Windows PowerShell :

```powershell
cd C:\chemin\vers\achabitation-refonte\backend-api
mvn clean test
```

## Tests disponibles

### Tests unitaires purs

- `BalanceCalculatorTest` : calcul RAV, mode moyenne, exclusions, dépenses globales/avancées, conversions, arrondis, remboursements et validations bloquantes.
- `PresencePeriodTest` : bornes de présence et protection contre les valeurs nulles.
- `EntityMapperTest` : normalisation des noms, arrondi monétaire, taux par défaut et masquage du RAV privé.
- `AuthServiceTest` : inscription, login, normalisation email, hash de mot de passe, rejet des doublons.

### Tests d’intégration API

- scénario bêta complet : compte, voyage, personnes, dépenses, résumé, exports et audit ;
- authentification obligatoire ;
- refus d’accès pour un non-membre ;
- invitations et adhésion à un voyage ;
- liaison guest ↔ compte sans écrasement automatique ;
- application explicite du profil à une personne liée ;
- confidentialité du RAV ;
- droits `OWNER/ADMIN/PARTICIPANT/READ_ONLY` sur les opérations sensibles ;
- validations serveur : dates de voyage, périodes, doublons, contraintes, dépenses incohérentes.

## Base utilisée en test

Les tests utilisent le profil Spring `test` et une base H2 en mémoire :

```yaml
jdbc:h2:mem:achabitation-test
```

Ils ne touchent pas la base locale :

```text
backend-api/data/achabitation.mv.db
```

## Smoke test API

Après lancement du backend :

```bash
mvn spring-boot:run
```

Dans un autre terminal, depuis `achabitation-refonte` :

```powershell
.\scripts\smoke-test.ps1
```

ou :

```bash
./scripts/smoke-test.sh
```

## Résultat attendu

```text
BUILD SUCCESS
Smoke test OK
```
