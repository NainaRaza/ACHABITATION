# ACHABITATION - Gestion finance - partage au RAV

> Note : ce document décrit l’ancien MVP desktop local et sert de référence fonctionnelle historique. La version actuelle de développement est la refonte Spring Boot + interface web documentée dans `ARCHITECTURE_REFONTE.md`, `V1_AUTH_PROFIL_CONTRAINTES.md` et `MVP_BETA_CHECKLIST.md`.

## Spécification fonctionnelle et technique

Version du document : 1.0  
Application : ACHABITATION - Gestion finance - partage au RAV  
Type d'application : application Java desktop locale  
Technologie actuelle : Java Swing  
Persistance actuelle : sérialisation locale Java dans un fichier `.ser`

---

## 1. Objectif général

L'application permet de gérer les dépenses communes d'un groupe de personnes pendant des vacances, une colocation temporaire, un séjour entre ami·es ou tout autre contexte de partage de frais.

Contrairement à une application de type Tricount classique, la répartition des dépenses ne se fait pas nécessairement à parts égales. La règle centrale de l'application est la répartition proportionnelle au reste à vivre, abrégé RAV.

Le RAV est utilisé comme poids de contribution. Plus une personne dispose d'un RAV élevé, plus sa part théorique dans les dépenses communes est élevée. L'objectif est de rendre la répartition plus équitable socialement qu'une simple division égalitaire.

L'application permet également de gérer les exceptions suivantes :

- personnes végétariennes ;
- personnes ne consommant pas d'alcool ;
- périodes de présence différentes selon les personnes ;
- dépenses globales mutualisées sans prise en compte des dates ;
- dépenses avancées avec choix manuel des participant·es ;
- personnes qui ne souhaitent pas déclarer ou utiliser leur RAV et préfèrent un poids moyen.

---

## 2. Périmètre fonctionnel actuel

Le périmètre actuel couvre les fonctionnalités suivantes :

- gestion des personnes ;
- gestion du RAV simple ;
- calcul avancé du RAV ;
- option de poids moyen ;
- gestion des contraintes végétarien / sans alcool ;
- gestion des dates de présence ;
- ajout, modification et suppression de dépenses ;
- dépenses normales avec décomposition général / viande / alcool ;
- dépenses globales ;
- dépenses en mode avancé avec participant·es sélectionné·es manuellement ;
- résumé des soldes ;
- calcul des remboursements à effectuer ;
- sauvegarde locale automatique.

Le périmètre actuel ne couvre pas encore :

- gestion de plusieurs voyages séparés ;
- comptes utilisateurs ;
- synchronisation cloud ;
- application mobile ;
- export PDF ou Excel ;
- import de tickets de caisse ;
- scan OCR ;
- authentification ;
- historique détaillé des modifications ;
- gestion multi-devises ;
- gestion de plusieurs périodes de présence par personne.

---

## 3. Concepts métier

### 3.1 Personne

Une personne représente un membre du groupe participant potentiellement aux dépenses.

Une personne possède les données suivantes :

- identifiant technique unique ;
- nom ;
- RAV ;
- indicateur d'utilisation du poids moyen ;
- indicateur d'utilisation du calcul avancé du RAV ;
- données de calcul avancé du RAV ;
- statut végétarien ;
- statut sans alcool ;
- date de début de présence ;
- date de fin de présence ;
- statut actif / désactivé.

Le nom d'une personne doit être unique. L'unicité est vérifiée en normalisant la casse et les espaces. Par exemple, `Karim`, `karim` et `Karim  ` sont considérés comme le même nom.

Une personne désactivée conserve son nom réservé afin de préserver l'historique des dépenses et d'éviter les ambiguïtés dans les anciennes données.

### 3.2 Reste à vivre

Le reste à vivre est la base normale de pondération des dépenses.

En mode simple, l'utilisateur saisit directement le RAV.

En mode avancé, le RAV est calculé à partir des champs suivants :

- salaire net après impôt ;
- loyer ;
- crédits ;
- charges fixes ;
- transport ;
- assurances ;
- autres charges contraintes.

Formule :

```text
RAV = salaire net après impôt
      - loyer
      - crédits
      - charges fixes
      - transport
      - assurances
      - autres charges contraintes
```

Le résultat est arrondi à deux décimales.

### 3.3 Poids moyen

Une personne peut choisir de ne pas utiliser son propre RAV dans la répartition. Dans ce cas, elle utilise l'option `Poids moyen dans les dépenses`.

Quand cette option est activée :

- son RAV n'est pas utilisé pour les calculs ;
- son champ RAV est grisé dans l'interface ;
- les champs du calcul avancé sont grisés ;
- le tableau des personnes affiche `Non utilisé` dans la colonne RAV.

La valeur RAV existante est conservée en mémoire. Si la personne repasse en mode RAV classique, ses données redeviennent utilisables.

La règle de calcul du poids moyen est la suivante :

Pour chaque bloc de dépense, une personne en mode moyenne reçoit comme poids la moyenne des RAV des participant·es du bloc qui utilisent le mode RAV classique.

Exemple :

```text
A : RAV 2 000 €
B : poids moyen
C : RAV 1 000 €
```

B est compté avec un poids de :

```text
(2 000 + 1 000) / 2 = 1 500 €
```

Les poids utilisés sont donc :

```text
A = 2 000
B = 1 500
C = 1 000
```

Si toutes les personnes concernées par un bloc sont en mode moyenne, aucune référence RAV n'est disponible. Dans ce cas, le bloc est réparti à parts égales.

### 3.4 Dépense

Une dépense représente une somme payée par une personne et à répartir entre plusieurs personnes selon les règles de l'application.

Une dépense possède les données suivantes :

- identifiant technique unique ;
- titre ;
- date ;
- payeur ;
- montant total ;
- montant viande ;
- montant alcool ;
- type de dépense ;
- indicateur de mode avancé ;
- liste manuelle de participant·es si le mode avancé est activé.

Les types de dépenses existants sont :

- dépense normale ;
- dépense globale.

---

## 4. Règles fonctionnelles de répartition

### 4.1 Règle centrale

Toutes les dépenses sont réparties selon le poids effectif des personnes concernées.

Le poids effectif est :

- le RAV de la personne si elle utilise le mode RAV classique ;
- le poids moyen calculé pour le bloc si elle utilise le mode moyenne ;
- `1` pour chaque personne si toutes les personnes du bloc sont en mode moyenne.

Formule générale :

```text
part personne = montant du bloc × poids effectif personne / somme des poids effectifs du bloc
```

Tous les calculs monétaires sont effectués avec `BigDecimal` et non avec `double`.

### 4.2 Dépense normale

Une dépense normale est découpée en trois blocs :

- part générale ;
- part viande ;
- part alcool.

La part générale est calculée comme suit :

```text
part générale = montant total - montant viande - montant alcool
```

Si le résultat est négatif, il est ramené à zéro par sécurité.

Les participant·es sont déterminé·es ainsi :

```text
Part générale : personnes actives présentes à la date de la dépense
Part viande   : personnes actives présentes à la date et non végétariennes
Part alcool   : personnes actives présentes à la date et non sans-alcool
```

Chaque bloc est ensuite réparti proportionnellement au poids effectif des participant·es du bloc.

### 4.3 Dépense globale

Une dépense globale sert à mutualiser une dépense considérée comme collective indépendamment de la date, de la distance ou de la consommation individuelle.

Cas d'usage typique : essence mutualisée.

Une dépense globale :

- ne tient pas compte de la date de présence ;
- ne tient pas compte du statut végétarien ;
- ne tient pas compte du statut sans alcool ;
- répartit le montant total entre toutes les personnes actives éligibles.

Le montant viande et le montant alcool n'ont pas de rôle fonctionnel dans une dépense globale.

### 4.4 Mode avancé d'une dépense

Le mode avancé permet de sélectionner manuellement les personnes concernées par une dépense.

Quand le mode avancé est activé :

- la date n'est pas utilisée pour filtrer les participant·es ;
- les statuts végétarien et sans alcool ne sont pas utilisés ;
- le montant total est réparti entre les personnes sélectionnées manuellement ;
- les montants viande et alcool ne sont pas utilisés comme sous-répartitions.

Le mode avancé est prioritaire sur le type de dépense. Si une dépense est en mode avancé, la sélection manuelle prévaut.

---

## 5. Règles de validation

### 5.1 Validation des personnes

À la création ou modification d'une personne :

- le nom est obligatoire ;
- le nom doit être unique après normalisation ;
- les dates de présence doivent être cohérentes ;
- si le mode RAV classique est utilisé, le RAV doit être positif pour participer aux répartitions ;
- si le mode moyenne est utilisé, le RAV peut être ignoré fonctionnellement.

Une personne n'est pas supprimée physiquement lorsqu'elle a vocation à préserver l'historique. Elle est désactivée.

### 5.2 Validation des dépenses

Une dépense ne doit pas pouvoir être enregistrée si elle ne concerne personne.

Cas bloquants :

- dépense normale avec une part générale positive mais aucune personne présente à la date ;
- dépense normale avec une part viande positive mais aucune personne présente non végétarienne ;
- dépense normale avec une part alcool positive mais aucune personne présente non sans-alcool ;
- dépense globale sans personne active éligible ;
- dépense en mode avancé sans participant·e valide ;
- personne sélectionnée non active ou non éligible.

Si une modification de dépense produit un état invalide, la modification est rejetée et l'ancienne dépense reste inchangée.

### 5.3 Éligibilité à une répartition

Une personne est éligible à une répartition si :

- elle est active ;
- elle utilise le mode moyenne ;

ou bien :

- elle est active ;
- elle utilise le mode RAV classique ;
- son RAV est strictement positif.

---

## 6. Calcul des soldes

Pour chaque dépense, l'application calcule d'abord la part due par chaque personne.

Ensuite, pour chaque personne :

```text
total payé = somme des dépenses dont la personne est le payeur
total dû   = somme des parts calculées pour cette personne
solde      = total payé - total dû
```

Interprétation du solde :

```text
solde positif : la personne doit recevoir de l'argent
solde négatif : la personne doit rembourser de l'argent
solde nul     : la personne est équilibrée
```

---

## 7. Calcul des remboursements

Le calcul des remboursements part des soldes finaux.

L'algorithme fonctionne ainsi :

1. créer une liste des débiteur·ices, c'est-à-dire les personnes avec un solde négatif ;
2. créer une liste des créditeur·ices, c'est-à-dire les personnes avec un solde positif ;
3. trier les deux listes par montant décroissant ;
4. prendre le ou la plus grosse débitrice et le ou la plus grosse créancière ;
5. créer un remboursement du minimum entre la dette restante et le crédit restant ;
6. diminuer les deux soldes ;
7. continuer jusqu'à extinction des dettes ou des crédits.

Exemple :

```text
A : +90 €
B : -70 €
C : -20 €
```

Remboursements proposés :

```text
B rembourse 70 € à A
C rembourse 20 € à A
```

---

## 8. Gestion des arrondis

Les montants sont gérés avec `BigDecimal` et une précision à deux décimales.

Pour éviter les erreurs d'arrondi classiques, le service de calcul convertit les montants en centimes lors de la répartition, puis distribue les centimes restants aux personnes ayant les plus gros restes décimaux.

Objectif :

```text
somme des parts calculées = montant total du bloc
```

Cela évite les écarts de type `119,99 €` ou `120,01 €` sur une dépense de `120,00 €`.

---

## 9. Interface utilisateur

L'application utilise Java Swing.

La fenêtre principale contient trois onglets :

- Personnes ;
- Dépenses ;
- Résumé.

### 9.1 Onglet Personnes

Fonctions disponibles :

- ajouter une personne ;
- modifier une personne ;
- désactiver une personne ;
- visualiser les paramètres de chaque personne.

Données visibles :

- nom ;
- RAV ou mention `Non utilisé` ;
- mode simple / avancé ;
- mode RAV / moyenne ;
- végétarien ;
- sans alcool ;
- dates de présence ;
- statut actif.

Quand le mode moyenne est activé, les champs liés au RAV sont grisés.

### 9.2 Onglet Dépenses

Fonctions disponibles :

- ajouter une dépense ;
- modifier une dépense ;
- supprimer une dépense ;
- choisir le payeur ;
- choisir le type de dépense ;
- activer le mode avancé ;
- sélectionner manuellement les participant·es en mode avancé.

Données principales d'une dépense :

- titre ;
- date ;
- payeur ;
- montant total ;
- montant viande ;
- montant alcool ;
- type normal ou global ;
- mode avancé ou non.

### 9.3 Onglet Résumé

L'onglet Résumé affiche :

- total payé par chaque personne ;
- total dû par chaque personne ;
- solde final ;
- remboursements proposés.

---

## 10. Architecture technique

### 10.1 Structure générale

Le projet est organisé dans le package Java suivant :

```text
com.vacances.ravtricount
```

Structure actuelle :

```text
src/com/vacances/ravtricount/
├── AppState.java
├── Balance.java
├── BalanceService.java
├── DataStore.java
├── Expense.java
├── ExpensePanel.java
├── ExpenseTableModel.java
├── ExpenseType.java
├── Main.java
├── MainFrame.java
├── Person.java
├── PersonPanel.java
├── PersonTableModel.java
├── Settlement.java
├── SummaryPanel.java
└── UiUtils.java
```

### 10.2 Responsabilités des classes

#### `Main.java`

Point d'entrée de l'application.

Responsabilités :

- initialiser l'application ;
- charger l'état sauvegardé ;
- ouvrir la fenêtre principale.

#### `MainFrame.java`

Fenêtre principale Swing.

Responsabilités :

- définir le titre de l'application ;
- créer les onglets ;
- gérer le menu ;
- déclencher la sauvegarde ;
- rafraîchir les panneaux après modification des données.

#### `AppState.java`

Objet racine de l'état applicatif.

Responsabilités :

- contenir la liste des personnes ;
- contenir la liste des dépenses ;
- servir d'objet sérialisable pour la sauvegarde.

#### `Person.java`

Modèle métier d'une personne.

Responsabilités :

- stocker les informations personnelles utiles au calcul ;
- calculer le RAV avancé ;
- déterminer si la personne est présente à une date ;
- porter les indicateurs végétarien, sans alcool, moyenne, actif.

#### `Expense.java`

Modèle métier d'une dépense.

Responsabilités :

- stocker le payeur ;
- stocker les montants ;
- stocker la date ;
- stocker le type de dépense ;
- stocker la sélection manuelle en mode avancé.

#### `ExpenseType.java`

Énumération des types de dépenses.

Valeurs actuelles :

```text
NORMAL
GLOBAL
```

#### `BalanceService.java`

Service métier central.

Responsabilités :

- valider les dépenses ;
- calculer les parts d'une dépense ;
- calculer les poids effectifs ;
- gérer le mode moyenne ;
- calculer les soldes ;
- calculer les remboursements ;
- gérer les arrondis au centime.

Cette classe contient la logique la plus importante de l'application.

#### `Balance.java`

Objet de résultat pour le résumé individuel.

Responsabilités :

- porter la personne ;
- porter le total payé ;
- porter le total dû ;
- exposer le solde.

#### `Settlement.java`

Objet de résultat pour les remboursements.

Responsabilités :

- indiquer qui rembourse ;
- indiquer qui reçoit ;
- indiquer le montant.

#### `DataStore.java`

Service de persistance locale.

Responsabilités :

- charger l'état depuis un fichier `.ser` ;
- sauvegarder l'état dans un fichier `.ser` ;
- créer les répertoires nécessaires si besoin.

#### `PersonPanel.java`

Interface Swing de gestion des personnes.

Responsabilités :

- afficher le formulaire personne ;
- gérer les actions ajouter / modifier / désactiver ;
- appliquer les validations d'interface ;
- gérer l'activation ou désactivation visuelle des champs RAV.

#### `ExpensePanel.java`

Interface Swing de gestion des dépenses.

Responsabilités :

- afficher le formulaire dépense ;
- gérer les actions ajouter / modifier / supprimer ;
- sélectionner le payeur ;
- sélectionner les participant·es en mode avancé ;
- appeler la validation métier avant sauvegarde.

#### `SummaryPanel.java`

Interface Swing du résumé.

Responsabilités :

- afficher les soldes ;
- afficher les remboursements ;
- recalculer les résultats à partir du `BalanceService`.

#### `UiUtils.java`

Classe utilitaire pour l'interface.

Responsabilités :

- parsing de champs ;
- affichage des messages d'erreur ;
- affichage des messages d'information.

---

## 11. Persistance

La persistance actuelle repose sur la sérialisation Java standard.

Fichier de données :

```text
rav-tricount-data.ser
```

Le fichier contient un objet `AppState` sérialisé.

Avantages :

- simplicité ;
- aucune dépendance externe ;
- suffisant pour un MVP local.

Limites :

- format peu lisible humainement ;
- compatibilité fragile si les classes changent fortement ;
- pas adapté à une synchronisation multi-utilisateur ;
- pas adapté à une application web ou mobile.

Évolution recommandée à moyen terme :

- SQLite pour une application desktop robuste ;
- PostgreSQL pour une application web ;
- export/import JSON pour faciliter la portabilité.

---

## 12. Contraintes techniques

### 12.1 Version Java

Le projet est conçu pour être compilé avec un JDK moderne, idéalement JDK 17 ou JDK 21.

### 12.2 Dépendances

Le projet actuel n'utilise aucune dépendance externe.

Il repose uniquement sur :

- Java standard ;
- Swing ;
- `java.time` ;
- `java.math.BigDecimal` ;
- sérialisation Java standard.

### 12.3 Compilation

Commande de compilation :

```bash
javac -encoding UTF-8 -d out src/com/vacances/ravtricount/*.java
```

Commande d'exécution :

```bash
java -cp out com.vacances.ravtricount.Main
```

Scripts disponibles :

```text
run.bat
run.sh
```

---

## 13. Hypothèses métier actuelles

Les hypothèses suivantes sont actuellement appliquées :

- une personne ne possède qu'une seule période de présence ;
- une dépense n'a qu'un seul payeur ;
- toutes les dépenses utilisent le RAV ou le poids moyen ;
- les dépenses globales concernent toutes les personnes actives éligibles ;
- le mode avancé remplace les filtres automatiques ;
- le montant viande et le montant alcool sont des sous-montants du montant total ;
- si `viande + alcool > total`, la part générale est ramenée à zéro mais la situation devrait idéalement être bloquée dans une version ultérieure ;
- une personne désactivée ne participe plus aux nouvelles répartitions mais son existence reste conservée dans les données.

---

## 14. Points de vigilance

### 14.1 Évolution du modèle sérialisé

Comme le stockage utilise la sérialisation Java, toute modification structurelle importante des classes peut rendre les anciennes sauvegardes incompatibles.

Pour une version durable, il faudra prévoir :

- migration vers SQLite ;
- versionnement du schéma ;
- import/export JSON.

### 14.2 Validation des montants

La validation actuelle couvre les cas où aucune personne n'est concernée.

Les validations suivantes devraient être renforcées :

- empêcher les montants négatifs ;
- empêcher `montant viande + montant alcool > montant total` ;
- empêcher une dépense sans payeur ;
- empêcher une dépense avec un titre vide ;
- empêcher une dépense à montant nul sauf cas explicitement autorisé.

### 14.3 Ergonomie

L'interface actuelle est fonctionnelle mais basique.

Améliorations possibles :

- interface JavaFX ;
- meilleure mise en page des formulaires ;
- infobulles explicatives ;
- aperçu immédiat des participant·es concerné·es par une dépense ;
- détail du calcul par dépense ;
- export du résumé.

---

## 15. Backlog fonctionnel recommandé

Priorité haute :

- bloquer les montants incohérents ;
- afficher le détail du calcul d'une dépense ;
- permettre l'export du résumé en CSV ;
- ajouter une entité `Voyage` ou `Groupe` ;
- remplacer la sérialisation par SQLite.

Priorité moyenne :

- gérer plusieurs périodes de présence par personne ;
- ajouter des catégories de dépenses ;
- permettre plusieurs payeurs pour une même dépense ;
- ajouter un import/export JSON ;
- ajouter une recherche ou un filtre sur les dépenses.

Priorité basse :

- scan de ticket ;
- application mobile ;
- synchronisation cloud ;
- authentification ;
- paiement intégré.

---

## 16. Critères d'acceptation du MVP

Le MVP est considéré comme fonctionnel si les critères suivants sont respectés :

- l'utilisateur peut créer plusieurs personnes avec des noms uniques ;
- l'utilisateur peut saisir un RAV simple ;
- l'utilisateur peut calculer un RAV avancé ;
- l'utilisateur peut activer le mode moyenne ;
- l'utilisateur peut ajouter une dépense normale ;
- l'utilisateur peut ajouter une dépense globale ;
- l'utilisateur peut ajouter une dépense en mode avancé ;
- l'application bloque les dépenses ne concernant personne ;
- le résumé affiche les montants payés, dus et les soldes ;
- les remboursements proposés équilibrent les soldes ;
- les données sont sauvegardées localement ;
- l'application peut être relancée en conservant les données précédentes.

---

## 17. Exemple complet de calcul

Personnes :

```text
Alice : RAV 2 000 €, non végétarienne, alcool OK
Bruno : RAV 1 000 €, végétarien, alcool OK
Chloé : poids moyen, non végétarienne, sans alcool
```

Dépense normale :

```text
Courses : 120 €
Viande : 30 €
Alcool : 20 €
Part générale : 70 €
```

Part générale : Alice, Bruno, Chloé.

Chloé est en poids moyen. Son poids est donc la moyenne des RAV classiques du bloc :

```text
(2 000 + 1 000) / 2 = 1 500
```

Poids du bloc général :

```text
Alice = 2 000
Bruno = 1 000
Chloé = 1 500
Total = 4 500
```

Répartition de la part générale :

```text
Alice = 70 × 2000 / 4500 = 31,11 €
Bruno = 70 × 1000 / 4500 = 15,56 €
Chloé = 70 × 1500 / 4500 = 23,33 €
```

Part viande : Alice et Chloé seulement. Bruno est végétarien.

Poids moyen de Chloé calculé sur les personnes RAV classiques du bloc :

```text
Alice seule = 2 000
Chloé = 2 000
```

Répartition viande :

```text
Alice = 15,00 €
Chloé = 15,00 €
Bruno = 0,00 €
```

Part alcool : Alice et Bruno seulement. Chloé est sans alcool.

Répartition alcool :

```text
Alice = 20 × 2000 / 3000 = 13,33 €
Bruno = 20 × 1000 / 3000 = 6,67 €
Chloé = 0,00 €
```

Total dû :

```text
Alice = 31,11 + 15,00 + 13,33 = 59,44 €
Bruno = 15,56 + 0,00 + 6,67 = 22,23 €
Chloé = 23,33 + 15,00 + 0,00 = 38,33 €
Total = 120,00 €
```


## Contraintes de cohérence des dates ajoutées

Le backend applique désormais les invariants suivants :

- la date de début du voyage est obligatoire ;
- la date de fin du voyage est obligatoire ;
- la date de début du voyage doit être antérieure ou égale à la date de fin ;
- chaque période de présence doit être comprise dans les dates du voyage ;
- deux périodes de présence d'une même personne ne doivent pas se chevaucher ;
- une dépense doit être datée dans la période du voyage.

Les périodes sont inclusives : une présence du 01/08 au 05/08 inclut le 01/08 et le 05/08. Une deuxième période démarrant le 05/08 est donc considérée comme chevauchante.

## Évolution — contraintes personnalisées et protections menstruelles

Chaque personne peut désormais porter une ou plusieurs contraintes personnalisées en plus des contraintes natives `végétarien` et `sans alcool`. Une contrainte personnalisée est un libellé libre, par exemple `sans porc`, `sans lactose`, `allergie arachide`.

La règle métier est la suivante : lorsqu'une dépense normale contient un montant associé à une contrainte personnalisée, ce montant est réparti uniquement entre les personnes présentes à la date de la dépense qui n'ont pas coché cette contrainte. Les personnes ayant coché la contrainte sont exclues de cette sous-partie de la dépense.

Le montant général d'une dépense normale est désormais calculé ainsi :

```text
montant général = total - viande - alcool - somme(montants contraintes personnalisées)
```

La somme `viande + alcool + contraintes personnalisées` ne doit jamais dépasser le total de la dépense.

Le calcul avancé du reste à vivre inclut désormais un champ `protections menstruelles`, traité comme une charge contrainte supplémentaire :

```text
RAV = revenu net après impôt
      - loyer
      - crédits
      - charges fixes
      - transport
      - assurances
      - autres charges contraintes
      - protections menstruelles
```
