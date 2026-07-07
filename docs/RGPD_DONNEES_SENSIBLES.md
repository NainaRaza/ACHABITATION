# Données sensibles, RAV et confidentialité

ACHABITATION peut manipuler des données financières et personnelles : reste à vivre, revenus, charges, dates de présence, dépenses, contraintes alimentaires, exclusions d'alcool ou contraintes personnalisées. Ces données ne doivent pas être traitées comme de simples préférences d'affichage.

## Principes retenus pour la bêta privée

- Le RAV doit être présenté comme une donnée sensible ou assimilée sensible dans le contexte d'usage.
- L'utilisateur doit comprendre si son RAV exact, un RAV masqué ou un effet de pondération est visible par les autres.
- Une personne extérieure à un voyage ne doit pas accéder aux participants, dépenses, exports, audit logs ou invitations.
- Les exports CSV sont sensibles, car ils regroupent dépenses, personnes, soldes et parfois inférences financières.

## État après traitement P1

- Suppression/anonymisation de compte : implémentation minimale via `DELETE /api/v1/auth/account`. Le compte est anonymisé, le token est supprimé, les contraintes/profils financiers du compte sont vidés et les personnes liées sont détachées/anonymisées.
- Export de données personnelles : implémentation minimale via `GET /api/v1/auth/export`, avec profil, voyages associés et personnes liées.
- Changement de mot de passe connecté : implémenté via `PUT /api/v1/auth/password`, avec rotation du token.

## À compléter avant exposition publique

- Tester juridiquement et fonctionnellement la stratégie suppression physique vs anonymisation.
- Étendre l'export aux dépenses et invitations si le besoin RGPD retenu l'exige.
- Politique de conservation.
- Consentement explicite avant partage d'un RAV utilisé dans les calculs.
- Écran expliquant ce que les autres verront.
- Audit des accès aux données sensibles.

## Règle produit recommandée

Par défaut, afficher moins d'information que nécessaire : le calcul peut utiliser un coefficient sans exposer le montant exact aux autres participant·es. Toute exposition plus précise doit être volontaire, explicite et réversible.

## Renforcement après retour équipe

L'export compte inclut maintenant profil, voyages liés, personnes liées, invitations créées, audit logs de l'utilisateur et dépenses payées par une personne liée au compte.

La suppression de compte anonymise l'identité, détache les personnes liées, neutralise les données financières associées et révoque les sessions. Les dépenses et voyages partagés restent conservés pour préserver la cohérence collective, mais l'identité directe de l'utilisateur supprimé n'est plus exposée.

Le résumé complet est réservé aux rôles owner/admin. Un participant non-admin reçoit uniquement sa propre balance et les règlements qui le concernent afin de réduire les inférences sur le RAV des autres.
