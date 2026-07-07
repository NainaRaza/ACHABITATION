# Politique de confidentialité — brouillon production

Statut : **brouillon de travail, à faire relire avant lancement public**.

ACHABITATION peut traiter des données personnelles classiques et des données à forte sensibilité contextuelle : email, nom affiché, voyages, participants, dépenses, contraintes personnelles, reste à vivre, revenus ou charges si le profil avancé est utilisé.

## Données collectées

- compte : email, nom affiché, mot de passe hashé ;
- session : jeton hashé, date de création, dernière utilisation, expiration, révocation ;
- profil financier : RAV simple ou champs de calcul avancé ;
- voyages : nom, dates, devise, membres, rôles ;
- personnes/guests : nom, lien éventuel avec un compte, présence, contraintes ;
- dépenses : titre, montant, payeur, participants, règles de répartition ;
- invitations : code, date de création, expiration, révocation ;
- audit : action, entité, acteur anonymisable, date.

## Finalités

Les données servent à gérer les comptes, les voyages, la répartition des dépenses, les soldes, les remboursements, les invitations et la sécurité du service.

## RAV et inférences

Le RAV peut permettre d'inférer une situation financière. Par défaut, le détail du RAV d'une personne liée n'est pas exposé à un autre participant si `livingRestPublic=false`. Le résumé est réduit pour les non-administrateurs : ils voient leur propre balance et les règlements qui les concernent, pas la vue complète du groupe.

## Export

L'utilisateur connecté peut exporter ses données depuis `GET /api/v1/auth/export`. L'export inclut le profil, les voyages liés, les personnes liées, les invitations créées, les logs d'audit où l'utilisateur est acteur et les dépenses payées par une personne liée au compte.

## Suppression de compte

La suppression anonymise l'identité du compte, retire les liens entre compte et personnes, neutralise les données financières et contraintes associées, révoque les sessions et conserve les éléments collectifs nécessaires à la cohérence des voyages partagés.

## Conservation indicative

- sessions actives : jusqu'à expiration ou révocation ;
- jetons de reset : 30 minutes, usage unique ;
- voyages et dépenses : tant que le voyage existe ;
- audit logs : durée à fixer avant production publique ;
- backups : durée à fixer selon l'hébergeur et la politique de rétention.

## Points restant à contractualiser

- base légale détaillée ;
- identité de l'éditeur et contact ;
- durée exacte de conservation des audit logs et backups ;
- procédure d'exercice des droits ;
- sous-traitants d'hébergement, SMTP, monitoring et sauvegarde.

## Ajouts techniques V6 à valider juridiquement

- Les comptes publics peuvent exiger une vérification email avant connexion.
- Les liens de vérification email expirent après 24 heures.
- Les liens de réinitialisation de mot de passe expirent après 30 minutes et sont à usage unique.
- Les événements sécurité peuvent être journalisés sous forme minimisée : demande de reset, email vérifié, session révoquée, suppression de compte. Les emails doivent être masqués ou hashés dans les logs.
- Les durées exactes de conservation des logs doivent être alignées avec `docs/LOG_RETENTION_POLICY.md` puis validées juridiquement avant lancement public.
