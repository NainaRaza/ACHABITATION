# Audit accessibilité web — état V6

## Statut

Audit réel non exécuté dans cet environnement. Ce point reste à faire avec navigateur, clavier et idéalement outil automatisé type axe, Lighthouse ou Playwright accessibility checks.

## Contrôles manuels minimaux à faire

- Navigation clavier complète : connexion, inscription, création voyage, personne, dépense, résumé, invitation, compte.
- Focus visible sur boutons, onglets et champs.
- Labels explicites pour tous les champs de formulaire.
- Messages d'erreur lisibles sans console.
- Contrastes suffisants sur fond sombre.
- Responsive mobile : largeur 360 px, 390 px, tablette, desktop.
- Reset password via lien `?resetToken=...` utilisable sans copier-coller manuel.
- Vérification email via lien `?verifyEmailToken=...` utilisable sans étape cachée.

## Statut recommandé

```text
Non bloquant bêta privée.
Bloquant avant lancement public si aucune passe clavier/responsive n'a été réalisée.
```
