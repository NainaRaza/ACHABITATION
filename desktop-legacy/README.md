# ACHABITATION Desktop Legacy

Ce dossier contient l’ancienne application Java Swing historique. Elle est conservée comme référence fonctionnelle locale, mais elle n’est plus l’architecture cible du projet.

L’architecture active est désormais :

```text
frontend-web      → backend-api → base de données
mobile-android    → backend-api → base de données
mobile-ios futur  → backend-api → base de données
```

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

## Fonctionnalités historiques

Le client Swing couvre les bases métier initiales :

- gestion locale des personnes ;
- saisie du RAV simple ou avancé ;
- poids moyen ;
- contraintes végétarien et sans alcool ;
- dates de présence ;
- dépenses normales, globales et avancées ;
- résumé des soldes et remboursements ;
- sauvegarde locale dans `rav-tricount-data.ser`.

## Limites du client legacy

Ce client ne couvre pas les fonctionnalités V1 actuelles : comptes utilisateurs, authentification, voyages multiples côté serveur, invitations, rôles, profil utilisateur central, contraintes personnalisées déclarées au niveau du voyage, confidentialité du RAV, audit logs, exports CSV backend et clients web/Android.

Il ne doit donc pas être utilisé comme référence technique pour les nouveaux développements. Les règles métier à jour sont dans `backend-api/src/main/java/fr/achabitation/domain` et dans `docs/SPECIFICATION_FONCTIONNELLE_TECHNIQUE.md`.
