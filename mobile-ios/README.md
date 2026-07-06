# Application iOS ACHABITATION

Dossier réservé à la future application iOS.

La cible prévue est une application cliente qui consommera uniquement l’API exposée par `backend-api`. Aucune logique de calcul RAV ne doit être recodée côté iOS : l’application mobile doit appeler les endpoints REST du backend.

Statut actuel : dossier préparatoire vide.

Priorités lorsque le développement iOS commencera :

- aligner les DTO avec `backend-api/src/main/java/fr/achabitation/api/dto` ;
- stocker le token dans le Keychain ;
- refuser le HTTP hors environnement de développement ;
- couvrir auth, voyages, personnes, dépenses, résumé, invitations, exports et audit ;
- ajouter des tests unitaires et UI.
