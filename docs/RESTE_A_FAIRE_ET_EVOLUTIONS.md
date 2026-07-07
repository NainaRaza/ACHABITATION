# Retour équipe — traitement P0/P1 production

Base traitée : `achabitation-refonte-stabilisation-v4-p1.zip`  
Hypothèse demandée : les builds, tests, CI et E2E non testables ici sont considérés **non faisables, a priori OK**.

## Synthèse

Le retour de l'équipe a été traité comme une bascule de priorité : le sujet n'est plus seulement la stabilisation MVP, mais la préparation d'un lancement production public. Les corrections applicables sans infrastructure externe ont été intégrées. Les points dépendant d'un hébergeur, d'un SMTP, de Redis, d'un reverse proxy HTTPS, d'un monitoring ou d'un vrai environnement de build sont conservés comme **non faisable, a priori OK**.

## P0 après traitement

| Priorité | Bloc | Tâche | Statut après traitement |
|---:|---|---|---|
| P0.1 | RGPD | Export/suppression/conservation complets | Fait renforcé, politique conservation à finaliser juridiquement |
| P0.2 | Sécurité/RAV | Confidentialité et inférences RAV | Fait partiel renforcé : résumé réduit pour non-admins, email lié masqué hors propre compte |
| P0.3 | Auth | Reset mot de passe | Fait core backend + web minimal ; envoi email SMTP non faisable ici, a priori OK |
| P0.4 | Auth | Multi-session minimale | Fait backend : table `user_session`, sessions multiples, révocation courante/toutes/spécifique |
| P0.5 | Sécurité | Rate limiting distribué | Non faisable ici, a priori OK : nécessite Redis/gateway/reverse proxy/service managé |
| P0.6 | Infra | Secrets/config production stricts | Fait : garde-fou prod + variables obligatoires + `.env.example` renforcé |
| P0.7 | Infra | HTTPS/CORS/reverse proxy | CORS/headers côté backend faits ; HTTPS/reverse proxy non faisable ici, a priori OK |
| P0.8 | Infra | Backup/restore testé | Scripts/docs présents ; test restauration réel non faisable ici, a priori OK |
| P0.9 | Ops | Monitoring/alerting/logs | Liveness/readiness + requestId présents ; alerting externe non faisable ici, a priori OK |
| P0.10 | Android | Release Android sécurisée | Fait partiel conservé ; signature/AAB réel non faisable ici, a priori OK |
| P0.11 | Produit | Périmètre lancement formalisé | Fait documentaire : recommandation lancement privé/invitation, pas inscription publique ouverte |

## P1 après traitement

| Priorité | Bloc | Tâche | Statut après traitement |
|---:|---|---|---|
| P1.1 | Backend | Tests production complémentaires | Reste à valider en CI réelle ; non faisable ici, a priori OK |
| P1.2 | Front-web | Tests Playwright supplémentaires | Base existante ; exécution navigateur non faisable ici, a priori OK |
| P1.3 | Android | Tests Android UI/ViewModel complémentaires | Tests ViewModel déjà ajoutés en V4 ; UI instrumentée non faisable ici, a priori OK |
| P1.4 | Web/Android | Harmoniser gestion compte | Fait documentaire ; web complet, Android renvoie à la documentation web pour actions compte avancées |
| P1.5 | Auth | Vérification email | Reste à faire avant inscription publique ouverte ; dépend SMTP, non faisable ici |
| P1.6 | Web/Infra | Headers sécurité | Fait côté backend ; à compléter sur hébergement front statique réel |
| P1.7 | Backend/RGPD | Politique audit logs | Fait documentaire minimal ; durée exacte à fixer |
| P1.8 | Backend | Matrice permissions métier | Fait : `docs/MATRICE_PERMISSIONS.md` |
| P1.9 | Ops | Procédure incident | Fait : `docs/PROCEDURE_INCIDENT.md` |
| P1.10 | CI/CD | Releases versionnées + rollback | Fait documentaire : `docs/RELEASE_ROLLBACK.md` |

## Changements backend appliqués

### Authentification et sessions

- ajout de `UserSessionEntity` ;
- ajout de `UserSessionRepository` ;
- création d'une session à chaque register/login/reset/change password ;
- stockage de hash de token par session ;
- expiration par session ;
- révocation session courante ;
- révocation toutes sessions ;
- révocation session spécifique ;
- fallback transitoire vers les anciennes colonnes `session_token_hash` pour compatibilité migration.

Endpoints ajoutés :

```text
GET    /api/v1/auth/sessions
DELETE /api/v1/auth/sessions/{sessionId}
DELETE /api/v1/auth/sessions
```

### Reset mot de passe

- ajout de `PasswordResetTokenEntity` ;
- ajout de `PasswordResetTokenRepository` ;
- token reset hashé en base ;
- expiration courte : 30 minutes ;
- usage unique ;
- invalidation des sessions après reset ;
- endpoint de demande générique sans énumération de compte ;
- endpoint de confirmation reset.

Endpoints ajoutés :

```text
POST /api/v1/auth/password/reset-request
POST /api/v1/auth/password/reset
```

Limite assumée : l'envoi email n'est pas branché. Le backend ne renvoie pas le jeton brut pour ne pas introduire une faille évidente. Un adaptateur SMTP ou service email reste nécessaire avant production publique.

### RGPD / export / suppression

Export enrichi :

- profil ;
- voyages liés ;
- personnes liées ;
- invitations créées ;
- audit logs où l'utilisateur est acteur ;
- dépenses payées par une personne liée au compte.

Suppression/anonymisation :

- email remplacé par une adresse technique anonymisée ;
- nom affiché neutralisé ;
- hash mot de passe remplacé ;
- sessions révoquées ;
- personnes liées détachées du compte ;
- données financières et contraintes des personnes liées neutralisées ;
- conservation des éléments collectifs nécessaires à la cohérence des voyages.

### Confidentialité RAV

- l'email d'une personne liée n'est plus exposé à un autre participant ;
- les données financières restent masquées si `livingRestPublic=false` ;
- le résumé complet est réservé aux propriétaires/admins ;
- un participant non-admin ne reçoit que sa propre balance et les règlements qui le concernent.

Ce n'est pas une suppression mathématique totale du risque d'inférence, mais c'est un durcissement significatif pour un usage public.

### Configuration production

- CORS configurable via `CORS_ALLOWED_ORIGINS` ;
- `APP_PUBLIC_URL` obligatoire en prod ;
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` obligatoires en prod ;
- démarrage refusé en prod si wildcard CORS ;
- démarrage refusé en prod si `APP_PUBLIC_URL` n'est pas HTTPS ;
- Swagger/OpenAPI déjà désactivés en prod ;
- H2 console désactivée en prod.

### Observabilité

- endpoint liveness ajouté : `/api/v1/health/liveness` ;
- endpoint readiness DB conservé : `/api/v1/health/readiness` ;
- request id déjà présent ;
- alerting externe à brancher hors code.

## Changements front-web appliqués

- ajout d'un bloc “Mot de passe oublié” ;
- appel de demande reset ;
- appel de confirmation reset par token ;
- connexion automatique après reset réussi ;
- message explicite indiquant que l'envoi email doit être branché en production.

## Changements infra/docs appliqués

Nouveaux documents :

```text
docs/POLITIQUE_CONFIDENTIALITE_DRAFT.md
docs/CGU_MENTIONS_LEGALES_DRAFT.md
docs/MATRICE_PERMISSIONS.md
docs/PROCEDURE_INCIDENT.md
docs/RELEASE_ROLLBACK.md
docs/PRODUCTION_P0_P1_RETOUR_EQUIPE_TRAITE.md
```

Fichiers renforcés :

```text
.env.example
infra/docker-compose.yml
backend-api/src/main/resources/application-prod.yml
```

## Non faisable ici, a priori OK

Les points suivants ne peuvent pas être réellement faits ou prouvés dans cet environnement :

```text
Build Maven complet.
Build Gradle Android complet.
Exécution GitHub Actions réelle.
Exécution Playwright avec navigateurs installés.
Envoi email SMTP réel.
Rate limiting distribué Redis/gateway.
Reverse proxy HTTPS Nginx/Traefik.
Certificat TLS + HSTS réel.
Monitoring externe et alerting.
Test réel de restauration PostgreSQL.
Signature Android release / AAB Play Store.
```

Ils sont donc considérés **non faisable, a priori OK**, à valider sur l'environnement cible.

## Verdict après traitement

```text
MVP avancé : oui.
Bêta privée contrôlée : oui, sous réserve builds/tests réels.
Production limitée sur invitation : plus crédible après ce lot.
Production publique ouverte : encore non recommandée tant que SMTP, email verification, Redis/rate-limit distribué, HTTPS, monitoring et restauration testée ne sont pas réellement en place.
```
