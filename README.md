# Galaxy Status Bar Studio

Éditeur Android de packs personnels pour le Galaxy SM-S911U1, build CP2A.260605.016.S911U1UEU8GZIF (One UI 9.0). Le module LSPosed classique cible `com.android.systemui`. Par défaut, son pack actif est vide.

## Préparer un pack

1. Ouvrir le Studio, puis **Créer un pack**. Chaque brouillon est enregistré localement.
2. Choisir un état dans le catalogue issu du firmware. Filtrer par exemple sur `wifi_signal`, `data_5g`, `airplane`, `bluetooth` ou `battery_`.
3. Importer une silhouette transparente en PNG, WebP ou SVG autonome. Le Studio la convertit en PNG de 128 pixels sur le grand côté. Prévoir une marge dans le dessin si nécessaire. Les SVG avec ressources externes et les animations ne sont pas pris en charge. Le cadre de destination reprend les dimensions intrinsèques du drawable Samsung.
4. Examiner les deux aperçus. La variante nuit est facultative. Pour les drawables, sa sélection suit le mode nuit Android. Le fond d'une application peut changer la teinte demandée par SystemUI indépendamment de ce mode. La batterie utilise la luminosité de la teinte Samsung. Une silhouette alpha lisible sur les deux fonds donne un résultat plus prévisible.
5. Cocher les états souhaités. **Exporter** permet de sauvegarder un pack JSON dans un emplacement choisi. **Importer un pack** crée un nouveau brouillon. Les packs restent éditables sans reconstruire l’APK.
6. **Utiliser ce pack** publie une copie des états cochés dans un fichier privé. Un fournisseur Android donne accès en lecture au pack publié à SystemUI. Le module lit le fichier en arrière-plan au démarrage de SystemUI. Un état déjà affiché peut nécessiter un rafraîchissement de son icône. Limites de publication : 384 états, 2 Mio encodés. Les brouillons sont indépendants. Le prochain chargement de SystemUI applique la copie publiée.

## Premier essai sur le Galaxy — avec accord du propriétaire

Exporter les brouillons importants. Conserver une connexion ADB déjà autorisée. Vérifier que le module est désinstallable depuis le PC avant d’autoriser un rechargement de SystemUI. Activer le module et sélectionner uniquement SystemUI dans LSPosed lors d'une intervention autorisée. Le premier pack d’essai utilise uniquement `stat_sys_wifi_signal_4` (0x7e08154a sur ce build, vecteur Samsung 19 × 14 dp). Le motif à trois bandes est original. Utiliser **Préparer l’essai Wi-Fi à un seul état**, contrôler les aperçus, puis publier ce pack. Un Wi-Fi 6/7, un autre niveau ou une variante opérateur peut solliciter une autre ressource.

Le Studio ne demande aucun redémarrage automatiquement. Lors du prochain démarrage autorisé du processus, vérifier la présence du compteur `GalaxyBarStudio` dans Logcat et le rendu dans l’état ciblé. Un compteur indique un enregistrement, pas un affichage. Comparer le rendu avec le pack vide au chargement suivant. Si OneUFy cible aussi ce nom, l’ordre de chargement peut masquer un remplacement. L’essai peut alors être non concluant.

## Retour arrière

- Avec l’interface utilisable, **Désactiver les remplacements du Studio** publie un pack vide. Les brouillons restent disponibles. Désactiver le Studio dans LSPosed retire ses hooks au prochain démarrage du processus.
- Si SystemUI devient instable et qu’ADB reste accessible, la désinstallation du seul paquet `studio.galaxy.statusbar` depuis le PC retire le nouveau module pour les futurs processus. Commande de secours réservée à une intervention autorisée : `adb -d uninstall studio.galaxy.statusbar`. Elle efface aussi les brouillons privés de cette application. Un processus déjà chargé garde ses hooks jusqu’à sa fin. Toute relance du processus ou du téléphone demande un accord séparé.
- Cette procédure conserve l’état des autres modules. Si Plumby est actif, le rendu après désactivation sera celui qu’il fournit. Le retour aux icônes Samsung d’usine dépend aussi de ce module existant. En cas de perte simultanée d’ADB et de l’interface, la récupération dépend du mécanisme de démarrage du root temporaire, non vérifié sur ce téléphone.

## Architecture et mises à jour

`profile.json` contient l’intersection de la table de ressources compilée de SystemUI et des cibles de la classe IconMap de Plumby. Le module contrôle le modèle et le build avant tout hook et résout les noms à l’exécution. Les identifiants numériques servent au diagnostic. Après une OTA, extraire la nouvelle table avec AAPT2, réexaminer les chemins de rendu et régénérer le profil via `tools/profile.py` avant de reconstruire. La liste des états batterie est liée séparément au build analysé.

Les remplacements de ressources utilisent `IXposedHookInitPackageResources` et `XResources.DrawableLoader`. Pour la batterie, un hook optionnel sur `SamsungBatteryMeterDrawable.draw(Canvas)` sélectionne un dessin par tranche 0–9, 10–19, …, 100 et selon la recharge. Il ne s’installe que si le pack contient une tranche batterie. Une tranche absente, la protection, l’économie d’énergie ou un état de santé autre que GOOD laisse le rendu Samsung s’exécuter. Le pourcentage dessiné par Samsung est remplacé avec l’icône pour la tranche personnalisée. Les animations de branchement et la puce de recharge restent gérées par One UI.

## Compilation et vérification

GitHub Actions utilise le wrapper Gradle 8.7 et Java 17, exécute assembleDebug, Android Lint et la vérification de signature, puis publie l’APK et son SHA-256. Le SDK de compilation 34 permet d’utiliser les API publiques nécessaires. Les classes Samsung sont résolues sur le firmware cible. L’API Xposed 82 est une dépendance compileOnly, LSPosed fournit l’implémentation. La signature de débogage d’un runner éphémère peut changer entre deux compilations. Exporter les packs avant une réinstallation. Ce circuit produit un APK de test.

[Référence OneUFy](https://github.com/mrx7014/OneUFy/tree/2f49c1d8391da6ea5b6ed02df917783e4b145338) · [Préférences LSPosed](https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences) · [RRO Android](https://source.android.com/docs/core/runtime/rros)

## Pack Pixel Android 17 (version 0.3)

Dans le Studio, **Charger le pack Pixel Android 17** crée un brouillon. Les cases **Batterie**, **Vague** et **Satellite** fonctionnent indépendamment. **Utiliser ce pack** publie les réglages pour le prochain chargement autorisé de SystemUI. Le pack vide désactive aussi les trois effets. Pour revenir à un pack personnel, le sélectionner et le publier à nouveau.

Les vecteurs Wi-Fi et mobile proviennent de SettingsLib du tag android-17.0.0_r1. Les niveaux Wi-Fi Samsung 0 à 4 utilisent respectivement les dessins AOSP 0, 1, 1, 2, 3. Les séries mobiles à quatre et cinq barres gardent leur échelle. UC, UW, 5G-A et certains badges régionaux utilisent un lettrage adapté. Les originaux et licences sont référencés dans les assets NOTICE.txt et Apache-2.0.txt. Les animations des flèches opérateur et les états invisibles restent gérés par Samsung.

La batterie horizontale suit le niveau réel et la recharge, en respectant le dessin Samsung pour les états protégés ou anormaux. Les images de batterie personnelles ont priorité. Le branchement USB ou secteur déclenche une vague et une petite batterie verte. La première observation après chargement du module ne déclenche rien. L’animation respecte la désactivation des animations système.

Le satellite décoratif s’applique au slot mobile visible de chaque SIM identifié dans ModernStatusBarMobileView. En appels d’urgence, les quatre points utilisent le niveau cellulaire transmis par Android. Si la mesure manque, un anneau creux s’affiche. En absence confirmée de voix et de données, le satellite occupe tout le slot. Un service de données actif, le mode avion/radio éteinte et les vraies connexions NTN gardent leur présentation standard. Si Samsung masque entièrement le slot, le Studio ne crée pas de vue supplémentaire. La logique concerne la présentation, sans modification de la radio ou des appels d’urgence.
