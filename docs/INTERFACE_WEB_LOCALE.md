# Interface web locale ACHABITATION

Cette interface est une première interface graphique branchée sur la nouvelle architecture Spring Boot.

Elle est volontairement simple : aucun React, aucun Node, aucun build frontend. Les fichiers sont servis directement par Spring Boot depuis `backend/src/main/resources/static/app`.

## Lancement

Depuis le dossier backend :

```powershell
cd achabitation-refonte\backend
mvn spring-boot:run
```

Puis ouvrir :

```text
http://localhost:8080/app
```

ou directement :

```text
http://localhost:8080/app/index.html
```

## Fonctionnalités disponibles

L’interface permet déjà de faire les opérations principales sur la nouvelle architecture :

- vérifier que le backend répond ;
- créer ou connecter un utilisateur de développement ;
- créer un voyage ;
- sélectionner un voyage ;
- ajouter, modifier et désactiver une personne ;
- gérer le RAV simple ;
- gérer le RAV avancé ;
- gérer le mode poids moyen ;
- gérer végétarien / sans alcool ;
- saisir plusieurs périodes de présence par personne ;
- ajouter, modifier et supprimer une dépense ;
- gérer les dépenses normales ;
- gérer les dépenses globales ;
- gérer le mode avancé avec participant·es manuel·les ;
- gérer une devise et un taux de conversion manuel ;
- consulter le résumé ;
- consulter les remboursements ;
- consulter l’historique d’audit.

## Limites assumées

Cette interface n’est pas encore une interface de production.

Elle ne contient pas encore :

- authentification de production finale ;
- reset de mot de passe ;
- supervision ;
- design mobile finalisé ;
- scan OCR ;
- export PDF / Excel ;
- édition complète des voyages ;
- suppression / archivage de voyages.

## Principe technique

Le navigateur appelle directement les endpoints REST du backend :

```text
/api/v1/auth/register
/api/v1/auth/login
/api/v1/trips
/api/v1/trips/{tripId}/persons
/api/v1/trips/{tripId}/expenses
/api/v1/trips/{tripId}/summary
/api/v1/trips/{tripId}/audit-logs
```

Comme l’interface et l’API sont servies par le même backend `localhost:8080`, il n’y a pas de problème CORS.

## Fichiers principaux

```text
backend/src/main/resources/static/app/index.html
backend/src/main/resources/static/app/styles.css
backend/src/main/resources/static/app/app.js
backend/src/main/java/fr/achabitation/web/AppPageController.java
```

`AppPageController` redirige `/` et `/app` vers `/app/index.html`.


## Évolution interface dépenses

Le tableau des dépenses affiche désormais une colonne "Personnes concernées". Elle liste les personnes réellement incluses dans la répartition selon le type de dépense : mode avancé, dépense globale, ou règles date / végétarien / sans alcool.

## Contraintes personnalisées

Les contraintes personnalisées se déclarent dans les paramètres du voyage. La fiche personne permet ensuite seulement de cocher les contraintes déjà acceptées pour ce voyage.

Dans le formulaire de dépense, chaque contrainte connue du voyage apparaît dans la zone `Montants liés aux contraintes personnalisées`. Le montant renseigné est exclu pour les personnes ayant coché cette contrainte et réparti entre les autres personnes éligibles.

Le calcul avancé du RAV contient aussi le champ `Protections menstruelles`, déduit du reste à vivre au même titre que les autres charges contraintes.

## Refonte ergonomique V1

L'interface web locale est désormais organisée en écrans spécialisés plutôt qu'en un écran dense avec blocs fixes latéraux.

Navigation principale :

- Tableau de bord : sélection/création de voyage, indicateurs rapides et actions fréquentes.
- Participant·es : liste des personnes, ajout/modification via panneau repliable.
- Dépenses : liste des dépenses, ajout/modification via panneau repliable.
- Résumé : soldes et remboursements.
- Paramètres : informations du voyage et contraintes officielles du voyage.
- Profil : connexion locale et profil RAV utilisateur.
- Historique : journal d'audit.

Les formulaires d'ajout/modification sont masqués par défaut. Ils s'ouvrent uniquement via les boutons `+ Ajouter`, `Modifier` ou les actions rapides du tableau de bord.

Cette organisation garde l'application compatible avec une future interface mobile ou web plus avancée : les blocs métiers sont séparés et ne dépendent plus d'une disposition latérale fixe.

## Mise à jour — déplacement du profil

Le profil n'est plus un onglet principal du voyage. Il est accessible depuis la barre haute via **Profil / compte**.

La navigation principale est recentrée sur le voyage : tableau de bord, participant·es, dépenses, résumé, paramètres et historique.

Le panneau **Profil / compte** contient :

- création / connexion locale de l'utilisateur ;
- édition du profil RAV ;
- choix de la visibilité du RAV ;
- contraintes personnelles connues ;
- application explicite du profil aux voyages/personnes liés.

L'enregistrement du profil ne modifie plus automatiquement les voyages liés. Après sauvegarde, l'utilisateur doit sélectionner les voyages/personnes à mettre à jour.

## Écran Profil / compte

Depuis la barre haute, le lien **Profil / compte** ouvre un écran exclusif, au même niveau visuel qu’un onglet.

Comportement attendu :

- si aucun utilisateur n’est connecté, l’écran affiche deux blocs :
  - connexion avec email ou nom affiché + mot de passe ;
  - création d’un nouveau compte avec email, nom affiché et mot de passe ;
- si un utilisateur est connecté, l’écran affiche :
  - l’email du compte ;
  - le nom affiché ;
  - une action de modification email / nom ;
  - une action de déconnexion locale ;
  - le profil RAV et ses options.

Le mot de passe n’est jamais prérempli dans l’interface.
