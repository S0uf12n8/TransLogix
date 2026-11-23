# TransLogix – Espace Chauffeur (JavaFX / FXML)

Conversion fidèle du fichier HTML `chefaire (1) (2).html` en JavaFX natif (FXML + CSS JavaFX + contrôleurs Java).
Aucun WebView, aucune base de données, aucune API : tout reste simulé comme dans l'original.

## Lancer
- Classe principale : `ma.translogix.chauffeur.Main`
- Maven : `mvn javafx:run` (le `pom.xml` est fourni à titre d'exemple – **alignez `javafx.version` sur celle de votre projet**,
  ou ignorez ce pom et copiez simplement `src/` dans votre projet existant). Code compatible Java 11+ / JavaFX 11+.

## Structure
```
src/main/java/ma/translogix/chauffeur/
  Main.java, AppContext.java (navigation, toast, modales, notifications partagées)
  controller/  1 contrôleur par section + MainController + modales + gabarits de lignes
  model/       Notification, MissionData, Rows (lignes des tableaux)
  util/        Ui, TableUtil, FileInput
src/main/resources/
  fxml/Main.fxml                 coque : barre latérale, en-tête, toast, modale (fx:include des 12 sections)
  fxml/sections/*.fxml           les 12 sections
  fxml/components/*.fxml         gabarits : ligne de notification, bloc d'info, étape, point d'inspection
  fxml/modals/*.fxml             modale « Reçu » et modale « Incident »
  css/translogix.css             toutes les couleurs / rayons / ombres d'origine
  fonts/                         Font Awesome 6 Free (Solid + Regular), licence SIL OFL incluse
```

## Scene Builder
Les FXML s'ouvrent seuls (chaque racine référence `translogix.css`). **Pour voir les icônes dans Scene Builder,
installez `fonts/fa-solid-900.ttf` et `fa-regular-400.ttf` sur votre système** (JavaFX ne sait pas charger de police
depuis le CSS ; l'application, elle, les charge dans `Main.java`).

## Écarts connus par rapport à l'HTML (limites techniques)
- **2 icônes absentes de Font Awesome Free** (Pro uniquement) : `truck-steering-wheel` (logo de la barre latérale) et
  `arrow-down-to-bracket` (bloc « Déchargement »). Avec le CDN gratuit de l'original elles s'affichent vides ;
  elles sont ici remplacées par `truck-front` et `download`.
- **Cartes KPI du tableau de bord** : dans l'original, le cercle décoratif (`.kpi:after`) est peint *par-dessus* l'icône,
  qui est donc masquée. C'est reproduit tel quel. Pour afficher les icônes, déplacez le `Circle` en premier enfant du `StackPane`.
- `letter-spacing` n'existe pas en JavaFX CSS ; `text-transform: uppercase` non plus (les libellés sont écrits en majuscules).
- Police Inter non embarquée : repli sur la police système.
- Seule la mise en page bureau est reproduite (pas de menu mobile, pas de styles d'impression).
- Les bulles de validation HTML5 natives sont approchées par une info-bulle ; `Export Relevé` ouvre un sélecteur
  d'enregistrement au lieu d'un téléchargement automatique.
- Comportement d'origine conservé : dans « Preuve de livraison », la clé de mission testée est le texte complet du
  `<select>`, donc la mission n'est pas clôturée par cette action (comme dans le JS d'origine).
