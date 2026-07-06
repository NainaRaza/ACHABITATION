# ACHABITATION - Gestion finance - partage au RAV

Application Java desktop ACHABITATION de gestion de dépenses de vacances avec répartition proportionnelle au reste à vivre.

## Lancement

Sous Windows :

```bat
run.bat
```

Sous Linux/macOS :

```bash
./run.sh
```

Lancement manuel :

```bash
javac -encoding UTF-8 -d out src/com/vacances/ravtricount/*.java
java -cp out com.vacances.ravtricount.Main
```

## Fonctionnalités

- Gestion des personnes : ajout, modification, désactivation.
- Blocage des doublons de nom : deux personnes ne peuvent pas avoir le même nom, même avec une casse différente ou des espaces multiples.
- Reste à vivre utilisé comme clé de répartition pour toutes les dépenses.
- Calcul simple du reste à vivre : saisie directe du montant.
- Calcul avancé du reste à vivre : salaire net après impôt moins loyer, crédits, charges fixes, transport, assurances et autres charges contraintes.
- Option de pondération moyenne par personne : la personne ne prend pas son propre reste à vivre comme poids, mais la moyenne des participant·es de la dépense.
- Paramètres par personne : végétarien, sans alcool, dates de présence.
- Dépenses normales : montant total, part viande, part alcool, date, payeur.
- Dépenses globales : mutualisation hors dates de présence, utile pour l'essence ou les frais décidés collectivement.
- Mode avancé : sélection manuelle des participant·es.
- Résumé : total payé, total dû, solde, remboursements à effectuer.
- Sauvegarde locale dans `rav-tricount-data.ser`.

## Règle du poids moyen

Pour chaque bloc de dépense, le système calcule les participant·es réellement concerné·es.

Une personne en mode `Poids moyen` reçoit comme poids la moyenne des restes à vivre des autres participant·es du même bloc qui utilisent le mode RAV classique.

Exemple :

- A utilise son RAV : 2 000 €
- B utilise le poids moyen
- C utilise son RAV : 1 000 €

Pour cette dépense, B reçoit le poids moyen suivant :

```text
(2000 + 1000) / 2 = 1500
```

La dépense est donc répartie avec les poids suivants :

```text
A = 2000
B = 1500
C = 1000
```

Si toutes les personnes concernées par un bloc sont en mode `Poids moyen`, il n'y a aucune base RAV disponible. Dans ce cas, le bloc est réparti à parts égales.

## Règles de validation bloquantes

Une personne est refusée si son nom est déjà utilisé par une autre personne. Le contrôle ignore la casse et les espaces multiples.

Une personne sans poids moyen doit avoir un reste à vivre strictement positif.

Une personne avec poids moyen peut avoir un reste à vivre à zéro, car son poids est calculé dynamiquement au moment de chaque dépense.

Une dépense est refusée si elle ne concerne personne après application des règles de répartition.

Cas bloqués :

- dépense normale à une date où aucune personne active n'est présente ;
- part viande positive alors qu'aucune personne concernée ne peut la payer ;
- part alcool positive alors qu'aucune personne concernée ne peut la payer ;
- dépense globale sans personne active éligible ;
- mode avancé sans participant·e valide.
