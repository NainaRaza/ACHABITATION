# Politique de conservation des logs — brouillon technique

Ce document est une base technique. Il doit être validé juridiquement avant lancement public.

## Catégories

| Catégorie | Contenu | Conservation recommandée |
|---|---|---:|
| Logs applicatifs techniques | erreurs, latence, request-id, statut HTTP | 30 à 90 jours |
| Logs sécurité | login, reset demandé, email vérifié, session révoquée, suppression compte | 6 à 12 mois |
| Audit logs voyage | création/modification dépenses, personnes, invitations, exports | durée du voyage + 12 mois, ou durée définie dans les CGU |
| Logs reverse proxy | IP, user-agent, chemins HTTP | 30 à 90 jours |
| Comptes supprimés/anonymisés | identifiant technique anonymisé, date suppression | durée minimale compatible avec support/abus |

## Principes

- Ne jamais logger les tokens, mots de passe, jetons de reset ou jetons de vérification email.
- Ne pas logger le RAV détaillé, les revenus, charges, montants sensibles de profil ou données financières privées.
- Utiliser `X-Request-ID` pour corréler les erreurs sans exposer le contenu métier.
- Masquer ou hasher les emails dans les logs sécurité.
- Définir une purge automatique avant lancement public.

## Reste à décider

- Durée exacte de conservation des audit logs métier.
- Base légale et information utilisateur dans la politique de confidentialité.
- Procédure de purge après suppression de compte.
- Procédure d'extraction logs en cas d'incident sécurité.
