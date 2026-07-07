# Correction V6.4 — Dépense mutualisée voyage et contraintes

## Constat

La règle précédente assimilait le type technique `GLOBAL` à une dépense répartie sur tout le voyage sans tenir compte des blocs viande, alcool et contraintes personnalisées.

Ce comportement était incorrect pour le cas métier suivant : une personne fait les courses pour tout le séjour, avec de l'alcool ou des produits liés à une contrainte. La dépense doit être mutualisée sur tout le voyage, mais les personnes qui ne consomment pas le bloc concerné ne doivent pas le payer.

## Nouvelle règle métier

Les libellés utilisateur sont maintenant :

- `NORMAL` : **Dépense datée** ;
- `GLOBAL` : **Dépense mutualisée voyage**.

Une dépense datée :

- tient compte de la date ;
- répartit la part générale entre les personnes présentes à cette date ;
- répartit les parts viande, alcool et contraintes uniquement entre les personnes présentes concernées.

Une dépense mutualisée voyage :

- ignore les dates de présence ;
- concerne les personnes actives du voyage ;
- conserve la pondération RAV / moyenne ;
- applique quand même les exclusions viande, alcool et contraintes personnalisées.

Exemple :

```text
Courses pour tout le séjour : 120 €
Part générale : 40 €
Part viande : 30 €
Part alcool : 20 €
Part Sans porc : 30 €
```

La part alcool est répartie uniquement entre les personnes qui consomment de l'alcool. La part Sans porc est répartie uniquement entre les personnes qui n'ont pas coché la contrainte Sans porc. Les dates de présence ne sont pas utilisées, car la dépense est mutualisée voyage.

## Modifications backend

Fichier principal :

```text
backend-api/src/main/java/fr/achabitation/domain/BalanceCalculator.java
```

Modifications :

- suppression du raccourci qui répartissait directement le total des dépenses `GLOBAL` ;
- factorisation logique entre dépense datée et dépense mutualisée voyage ;
- `GLOBAL` utilise maintenant les personnes actives éligibles comme base de calcul ;
- `NORMAL` continue d'utiliser les personnes présentes à la date ;
- les blocs viande, alcool et contraintes personnalisées sont appliqués dans les deux cas ;
- la validation bloque maintenant aussi une dépense globale si un bloc positif ne concerne personne.

Tests ajoutés :

```text
BalanceCalculatorTest.globalExpenseIgnoresPresenceButAppliesMeatAlcoholAndCustomConstraints
BalanceCalculatorTest.validationBlocksGlobalAlcoholAmountWhenNoEligibleAlcoholParticipantExistsEvenWithoutPresenceDate
```

## Modifications front-web

Fichiers :

```text
frontend-web/index.html
frontend-web/src/expenses.js
```

Modifications :

- renommage UI : `Normale` devient `Datée` ;
- renommage UI : `Globale` devient `Mutualisée voyage` ;
- les champs viande, alcool et contraintes ne sont plus désactivés pour une dépense `GLOBAL` ;
- le payload web envoie maintenant les blocs viande, alcool et contraintes aussi pour `GLOBAL` ;
- le calcul d'affichage des personnes concernées applique les contraintes aussi pour `GLOBAL` ;
- le badge de type affiche un libellé métier plutôt que `NORMAL` / `GLOBAL`.

## Modifications Android

Fichier :

```text
mobile-android/app/src/main/java/fr/achabitation/mobile/ExpensesScreen.kt
```

Modifications :

- renommage UI : `Normale` devient `Datée` ;
- renommage UI : `Globale` devient `Mutualisée voyage` ;
- les champs part viande, part alcool et contraintes restent visibles et actifs pour une dépense mutualisée voyage ;
- le payload Android envoie maintenant ces montants aussi pour `GLOBAL` ;
- les textes explicatifs ont été alignés avec la nouvelle règle métier.

## Documentation mise à jour

Fichiers :

```text
docs/SPECIFICATION_FONCTIONNELLE_TECHNIQUE.md
docs/DOSSIER_TECHNIQUE_JAVA.md
docs/API_EXEMPLES.md
docs/ARCHITECTURE_REFONTE.md
docs/MVP_BETA_CHECKLIST.md
frontend-web/README.md
mobile-android/README.md
```

## Validé ici

```text
frontend-web/run-tests.sh : OK
node --check frontend-web/src/expenses.js : OK
```

## Non validé ici

```text
./mvnw clean test
./gradlew testDebugUnitTest assembleDebug
test manuel Android émulateur
```

La validation Maven/Gradle reste à relancer sur une machine avec dépendances disponibles.
