# Procédure d'incident production

## Objectif

Disposer d'une procédure courte pour les incidents de sécurité, données, calculs et exploitation.

## Incident compte compromis

1. Révoquer toutes les sessions du compte concerné.
2. Forcer changement ou reset du mot de passe.
3. Vérifier les audit logs du compte.
4. Prévenir l'utilisateur si des données ont pu être exposées.

## Fuite de données suspectée

1. Désactiver temporairement inscriptions et invitations si nécessaire.
2. Identifier la période, les comptes et les voyages concernés.
3. Préserver les logs techniques sans exposer tokens/RAV.
4. Prévenir les personnes concernées selon la gravité.
5. Documenter cause, correction et mesure d'évitement.

## Backup invalide

1. Stopper les rotations destructives.
2. Identifier le dernier backup restaurable.
3. Tester restauration sur base temporaire.
4. Documenter RPO/RTO réel.

## Bug de calcul RAV ou solde

1. Désactiver temporairement l'affichage ou l'export concerné si nécessaire.
2. Identifier voyages/dépenses impactés.
3. Recalculer après correctif.
4. Informer les utilisateurs concernés si des remboursements ont pu être erronés.

## Suppression compte erronée

1. Stopper la fonctionnalité si le bug est reproductible.
2. Vérifier backups disponibles.
3. Restaurer uniquement si c'est cohérent avec les droits des autres utilisateurs.
4. Documenter l'arbitrage RGPD et métier.
