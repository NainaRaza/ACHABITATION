# Matrice de permissions métier

| Action | Owner | Admin | Participant | Read only | Utilisateur externe |
|---|---:|---:|---:|---:|---:|
| Lire le voyage | Oui | Oui | Oui | Oui | Non |
| Modifier paramètres voyage | Oui | Oui | Non | Non | Non |
| Créer personne/guest | Oui | Oui | Non | Non | Non |
| Se créer comme personne liée | Oui | Oui | Oui | Non | Non |
| Modifier guest non lié | Oui | Oui | Non | Non | Non |
| Modifier sa propre personne liée | Oui | Oui | Oui, si liée au compte | Non | Non |
| Lire profil financier détaillé autre personne liée | Non par défaut | Non par défaut | Non | Non | Non |
| Lire résumé complet | Oui | Oui | Non | Non | Non |
| Lire son propre résumé | Oui | Oui | Oui | Non sauf personne liée | Non |
| Créer dépense | Oui | Oui | Oui | Non | Non |
| Export CSV voyage | Oui | Oui | Non | Non | Non |
| Audit logs voyage | Oui | Oui | Non | Non | Non |
| Créer invitation | Oui | Oui | Non | Non | Non |
| Export données personnelles | Propre compte uniquement | Propre compte uniquement | Propre compte uniquement | Propre compte uniquement | Non |
| Suppression/anonymisation compte | Propre compte uniquement | Propre compte uniquement | Propre compte uniquement | Propre compte uniquement | Non |

Cette matrice doit rester alignée avec `AuthorizationService` et les tests d'intégration backend.
