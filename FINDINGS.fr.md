# FINDINGS : POC Skip → Compose Multiplatform (desktop Linux)

> Copie de travail française. La version canonique (destinée à GitHub, en anglais) est
> [`FINDINGS.md`](./FINDINGS.md).

> **Mise à jour (POC 6, plus tard) :** ce verdict NO-GO a été rouvert. Le POC 6 a dé-Android-isé le
> SkipUI transpilé et l'a fait à la fois compiler ET rendre, d'abord sur Compose Multiplatform Desktop
> (JVM), puis en Kotlin/Native Linux sans JVM (37 Mo / 122 Mo contre 137 Mo / 224 Mo sur la JVM). Le
> cadrage « diffus / ne pas rouvrir » ne tient donc pas au niveau compilation-et-rendu. Ce qui tient
> encore, c'est la nuance déjà présente ici (le couplage est « concentré, pas diffus ») et l'estimation
> en semaines-homme pour un portage complet, maintenu et de production. Voir
> [`FINDINGS-POC6.fr.md`](./FINDINGS-POC6.fr.md).

Journal du POC. Versions réelles, commandes réellement lancées, erreurs brutes, correctifs,
temps passé, et en fin de course la décision go/no-go argumentée.

## Question à trancher

Combien de travail faut-il pour qu'une app SwiftUI transpilée par Skip tourne en Compose
Multiplatform sur desktop Linux, au lieu de Jetpack Compose sur Android ?

## Synthèse exécutive

**Décision : NO-GO sur un transpileur Skip→Compose Multiplatform pour l'appareil cible. Compose-first.**
Le chemin est *techniquement possible* (prouvé, pas argumenté) mais pour une vraie app son coût
équivaut à posséder un moteur d'exécution SwiftUI→Compose, soit le même travail qu'écrire du Compose
directement, en supprimant l'indirection mais en ajoutant une dépendance à l'upstream Skip.

**Ce qui est prouvé.** Un écran SwiftUI transpilé par Skip **verbatim** tourne, interactif, sur
**Compose Multiplatform desktop, sur macOS et Linux** (JVM/Skia). Le witness est passé d'un compteur à
un petit écran réaliste : persistance `@AppStorage`, `HStack`, un conditionnel, une `List`, et un
`NavigationStack` avec un écran de détail empilable, le tout rendu depuis la sortie du transpileur, intouchée.
Captures : macOS [`desktop-witness.png`](./docs/desktop-witness.png) /
[`…-after-click.png`](./docs/desktop-witness-after-click.png), Linux (Docker, sans affichage)
[`linux-witness.png`](./docs/linux-witness.png) /
[`…-after-click.png`](./docs/linux-witness-after-click.png),
persistance [`…-persisted.png`](./docs/desktop-witness-persisted.png). Exécutable :
[`desktop-witness/`](./desktop-witness/).

**Pourquoi ce n'est pas « ça build tout seul ».** `skip export` donne un projet Gradle **autonome mais
Android-only** (6× `com.android.library`). Sur JVM nue, `SkipLib`/`SkipFoundation`/`SkipModel`
compilent dès que le bouchon `android.jar` est au classpath (le premier « mur » était du *classpath*, pas
une réécriture). Mais **SkipUI complet** est un monolithe : une **interface `View` de 3561 lignes**
reliée à chaque fonctionnalité, tirant `androidx.navigation3` (qui n'a **aucun artefact CMP**), `LocalContext`,
material3-adaptive, activity… On ne peut pas en extraire une tranche triviale. Deux routes existent,
toutes deux convergent vers Compose-first :

1. **Porter le vrai moteur d'exécution Skip vers CMP** : bute sur son pire mur à `navigation3` + la couche
   Android de SkipFoundation. **Mois-homme** plus suivi de l'upstream Skip.
2. **Adaptateur par app** (ce que ce PoC a construit, ~300 lignes au total) : chaque fonctionnalité se révèle
   franchissable en maquette sur du Compose pur *car l'adaptateur contourne les deps Android de Skip*. Mais
   étendu à une vraie app, cet adaptateur **est** un moteur d'exécution SwiftUI-sur-Compose écrit à la main :
   réimplémenter le boulot de Skip, à une fraction de sa fidélité.

**La courbe de coût, mesurée (LOC adaptateur vs le fichier réel SkipUI/SkipFoundation) :**

| Fonctionnalité SwiftUI | Nature de l'écart | Réel | Maquette adaptateur | Gardé |
|---|---|---|---|---|
| `HStack`, `Text`, `Button` | primitive de layout | - | ~12 lignes ch. | complet |
| `@AppStorage` | **adossé Android** (UserDefaults → SharedPreferences) | 894 lignes | 28 lignes | Int seul, persiste |
| `List` | **architecture SkipUI** (LazyItemFactory) | 1334 lignes | 9 lignes | rangées statiques |
| `NavigationStack` | **navigation3** (pas d'artefact CMP) | 2326 lignes | 42 lignes | push/pop, sans transitions |

Chaque maquette fait 1-3 % du code réel ; le comportement qui rend la fonctionnalité réelle vit dans les 97-99 %
qu'on laisse tomber. Les primitives sont bon marché ; chaque fonctionnalité adossée à Foundation ou à la
navigation est sa propre réimplémentation.

**Recommandation.** Pour l'appareil cible, **écrire du Compose Multiplatform directement.** Reconsidérer le
transpileur seulement si Skip upstream livre une cible CMP/desktop officielle (discussion #163 :
intérêt, pas de livraison), ce qui sortirait le portage du moteur d'exécution de notre périmètre.

**Résultat du délai imparti :** boucle complète (installation → e2e macOS+Linux → trois expériences de falaise)
en **~4h**, très en deçà du délai imparti de 3 jours. Trancher go/no-go était bon marché ; la partie chère
(un portage de moteur d'exécution maintenu) on ne l'a, à raison, pas commencée. Détail et preuves brutes dans les
étapes numérotées ci-dessous.

---

## Étape 1 : Installation (2026-07-11)

### Toolchain du poste (macOS Apple Silicon)

| Composant | Version | État | Note |
|---|---|---|---|
| macOS | 26.5.2 (ARM) | ✅ | |
| Xcode | 26.6 (build 17F113) | ✅ | requis pour la transpilation SwiftUI |
| Swift | 6.3.3 | ✅ | via Xcode |
| Skip | 1.9.4 | ✅ | installé via Homebrew |
| Gradle | 9.6.1 | ✅ | tiré en dépendance par la formule `skip` |
| Java (JDK) | 26.0.1 (vu par skip) / Temurin 21 (dans le PATH shell) | ✅ | > 17 requis dans les deux cas |
| Android SDK | 37.0.0 | ✅ | Android Studio déjà installé sur le poste |
| Android Debug Bridge | 1.0.41 | ✅ | |
| Homebrew | 6.0.9 | ✅ | |
| Swiftly | - | ✗ | `error executing swiftly` : **non bloquant** (cf. ci-dessous) |
| GitHub CLI (`gh`) | 2.96.0 | ✅ | |

### Commandes réellement utilisées

```bash
# Skip n'était pas installé. Homebrew 6 exige de faire confiance au tap avant install.
brew tap skiptools/skip
brew trust skiptools/skip
brew install skip          # tire aussi gradle 9.6.1, gradle-completion, swiftly

skip doctor                # diagnostic complet de la toolchain
```

Remarques :
- `skip --version` n'existe pas (`Error: Unknown option '--version'`). La version se lit via
  `skip doctor`.
- `gradle` n'était pas installé en standalone au départ ; la formule `skip` l'a tiré, plus de
  souci.

### Le seul rouge : swiftly

`skip doctor` remonte une seule erreur : `Swiftly version: error executing swiftly`. Swiftly est
le gestionnaire de toolchains Swift. **Non bloquant pour ce POC** : Swift 6.3.3 est correctement
détecté via Xcode, et la transpilation Skip s'appuie sur la toolchain Xcode, pas sur une toolchain
gérée par swiftly. À revérifier seulement si une commande Skip échoue explicitement dessus.

### Temps passé

- Étape 1 : ~15 min (essentiellement l'install de Skip + dépendances).

---

## Étape 2 : App témoin minimale (2026-07-11)

### Syntaxe `skip init` (vérifiée, pas présumée)

`skip --help` expose `create` (interactif) et `init` (scriptable). `init` distingue deux modèles
d'app :

- `--transpiled-app` (**Skip Lite**) : transpile Swift/SwiftUI en **Kotlin/Jetpack Compose**. Émet
  du code source Compose, c'est le modèle à inspecter et recibler vers CMP.
- `--native-app` (**Skip Fuse**) : compile le Swift en natif pour Android via un pont ; n'émet *pas*
  de code source Compose.

Le PoC porte explicitement sur du SwiftUI *transpilé* et sur « où est le Compose émis », donc
**`--transpiled-app`** est le bon modèle.

Commande réellement lancée :

```bash
skip init --transpiled-app --appid=dev.skeletongamer.witness \
  --no-fastlane --no-module-tests --show-tree --no-git-repo witness-app Witness
```

Piège : le nom de dossier et le nom de module doivent différer (insensible à la casse). `witness` +
`Witness` a été rejeté ; utilisé `witness-app` (dossier) + `Witness` (module). Init en ~42 s.

### Structure générée

Un seul package Swift ciblant les deux plateformes :

- `Sources/Witness/` : Swift/SwiftUI partagé (`ContentView.swift`, `ViewModel.swift`, `WitnessApp.swift`)
- `Darwin/` : projet Xcode, `Info.plist`, catalogues d'assets, `Main.swift` (point d'entrée iOS)
- `Android/` : projet Gradle (`settings.gradle.kts`, `gradle.properties`, icônes de launcher)
- `Package.swift` / `Package.resolved` : manifeste SPM tirant SkipUI, SkipFoundation, SkipModel…
- `Skip.env`, `Sources/Witness/Skip/skip.yml` : config Skip

### Réduction au plancher

Le template par défaut est une app-exemple complète (TabView, List, `NavigationStack`, persistance
JSON vers `applicationSupportDirectory` via `FileManager`, view model `@Observable`, `OSLog`). C'est
justement la surface SkipFoundation qui brouillerait la mesure de l'écart CMP. Le brief veut le
*plancher*, donc :

- `ContentView.swift` réécrit en un seul écran : compteur `@State`, `VStack { Text; Button }`.
- `ViewModel.swift` supprimé (persistance / `FileManager` / `Codable`, plus référencé).
- `WitnessApp.swift` conservé tel quel : les points d'entrée plateforme référencent `WitnessRootView`,
  qui charge `ContentView`.

Résultat : l'app témoin n'exerce que des primitives UI SwiftUI (pas de navigation, pas de persistance,
pas d'I/O Foundation).

### Temps passé

- Étape 2 : ~15 min.

## Étape 3 : Chemin nominal (Android) (2026-07-11)

### Résultat : fonctionne ✅

L'app témoin se construit, s'installe et tourne sur un émulateur Android. Capture dans
[`docs/android-witness.png`](./docs/android-witness.png) : un libellé `Count: 0` centré et un
bouton `Increment`, soit l'UI minimale réduite.

### Commandes réellement lancées

```bash
# Démarrer un émulateur (aucun ne tournait ; les AVD existaient déjà)
emulator -avd Pixel_9_API34 -no-snapshot-save &
adb wait-for-device                       # puis attendre sys.boot_completed == 1

# Build + transpilation + install + lancement, Android uniquement
skip app launch --android
```

Vérification :

```bash
adb shell pm list packages | grep witness
# package:dev.skeletongamer.witness
adb shell dumpsys activity activities | grep ResumedActivity
# topResumedActivity=... dev.skeletongamer.witness/witness.module.MainActivity
```

Première compilation ~5 min (téléchargements Gradle + transpilation Skip). Code de sortie 0.

### Pièges

- `skip app launch --android` a quand même journalisé des tentatives de compilation **iOS** vers des
  appareils physiques (`The device is passcode protected`, `Failed to start remote service …`). Bruit
  non fatal, le code de sortie est resté 0 et la compilation puis le lancement Android ont réussi.
- **Artefacts transpilés périmés** : trois copies du `ContentView.kt` transpilé existent sous
  `.build/` (`plugins/outputs`, `index-build`, `Darwin/DerivedData/…`). Les deux premières étaient
  périmées (ancien template) ; la plus fraîche, la vraie, est sous `Darwin/DerivedData/…`. Toujours
  lire la plus récente par date de modification.

### Découverte clé : ce que Skip émet réellement

Le Kotlin transpilé (sauvegardé tel quel dans
[`docs/transpiled-ContentView.kt`](./docs/transpiled-ContentView.kt)) **ne cible pas** des widgets
Jetpack Compose bruts. Il cible le **moteur d'exécution Skip** :

```kotlin
import skip.lib.*
import skip.ui.*
import skip.foundation.*
import skip.model.*
// les widgets sont des types SkipUI, pas androidx :
VStack(spacing = 16.0) { ... Text(...); Button(...) }
```

- `VStack`, `Text`, `Button`, `View`, `ComposeBuilder`, `LocalizedStringKey` sont des types
  **SkipUI** (`skip.ui.*`), pas `androidx.compose.material3.*`.
- Les **seuls** imports androidx directs sont `androidx.compose.runtime.*` (`remember`,
  `mutableStateOf`, `rememberSaveable`, `Saver`), la couche état/exécution, que Compose
  Multiplatform fournit également.

**Conséquence pour CMP.** Le reciblage ne consiste *pas* à retraduire les widgets vers du CMP
brut. Le code émis dépend des bibliothèques d'exécution Skip, donc le faire tourner sur CMP desktop revient à
faire compiler et fonctionner **SkipUI / SkipFoundation / SkipModel** sur la cible JVM/desktop.
C'est exactement là que se situe le couplage Android présumé, et c'est l'objet des étapes 4-5.

Dépendances d'exécution de Skip résolues (depuis `Package.resolved`) : `skip-lib`, `skip-foundation`,
`skip-model`, `skip-ui`, `skip-unit`.

### Temps passé

- Étape 3 : ~15 min (dont ~5 min de première compilation).

## Étape 4 : Export Gradle (`skip export`) (2026-07-11)

### Commande réellement lancée

```bash
skip export --debug --no-ios --show-tree -d ../export
```

A produit trois artefacts dans `export/` (ignoré par git, volumineux) :

- `Witness-debug.apk` (84 Mo), `Witness-debug.aab` (24 Mo) : bundles Android
- `Witness-project.zip` (2,7 Mo) : **les sources du projet Gradle autonome** (la partie utile)

### Le projet exporté est un vrai projet Gradle autonome

Décompressé, `Witness-project/Witness/` contient **tout le moteur d'exécution Skip en modules Gradle** plus
l'app :

```
settings.gradle.kts        # version catalog autonome, mavenCentral() + google()
Witness/                   # le module app (ContentView.kt transpilé, etc.)
SkipUI/  SkipFoundation/  SkipModel/  SkipLib/  SkipUnit/
```

Contrairement au `Android/settings.gradle.kts` du repo (qui appelle en shell `skip plugin --prebuild`
et référence `BUILT_PRODUCTS_DIR` d'Xcode), le `settings.gradle.kts` exporté est **découplé d'Xcode
et du CLI Skip** : un simple version catalog résolvant tout depuis Maven Central / Google. L'objectif
de la discussion #163 est donc atteint : un projet Gradle qui build sans Xcode. La seule question est
*pour quelle cible* il peut builder.

### Blocage structurel n°1 : chaque module est une Android Library

Les 6 modules déclarent `plugins { alias(libs.plugins.android.library) }` et un bloc `android {
compileSdk = 36; minSdk = 28; namespace = … }`. Ils compilent en **AAR Android contre le SDK
Android**, pas en artefacts Kotlin/JVM ou KMP. Compose Desktop exige des modules KMP/JVM utilisant
**JetBrains Compose** (`org.jetbrains.compose` / les artefacts androidx multiplateformes). Convertir
`com.android.library` → KMP/JVM pour chaque module d'exécution est le premier changement, inévitable et
transverse.

### Couplage Android, quantifié (niveau source, `.kt` hors tests)

> **Correction (voir étape 5).** Les comptes ci-dessous venaient d'abord de `^import android.` /
> `^import androidx.` seulement. Le transpileur Skip émet la plupart des références plateforme en
> **noms pleinement qualifiés inline** (ex. `@androidx.annotation.Keep`,
> `android.icu.text.DecimalFormat`), que le grep d'imports rate. Le tableau utilise désormais les
> **fichiers touchant le framework `android.*`** (inline inclus). Chiffres corrigés, plus élevés :
> SkipFoundation **11** fichiers (pas 4), SkipUI **29** (pas 20).

| Module | fichiers .kt | fichiers touchant `android.*` framework | refs `androidx.compose.*` | androidx non-compose | Verdict |
|---|---|---|---|---|---|
| **SkipLib** | 41 | 0 | 0 | 0 (seul `annotation.Keep`) | portable (1 dep triviale) |
| **SkipFoundation** | 84 | **11** | ~1 | ~2 | portage substantiel (Context/ICU/prefs) |
| **SkipModel** | 10 | 2 | 7 | 0 | trivial (`Looper`) |
| **SkipUI** | 280 | **29** | **2428** | **79** | le mur |

### Lecture de SkipUI (le mur) : concentré, pas diffus

Sur les 2095 imports androidx de SkipUI :

- **2019 (96,4 %) sont `androidx.compose.*`** : l'API Compose, que **Compose Multiplatform reflète
  sous les mêmes noms de packages**. Portable au niveau source (remplacer les artefacts Compose
  Android de Google par ceux, multiplateformes, de JetBrains).
- **~76 (3,6 %) sont de l'androidx Android-only**, concentrés dans quelques sous-systèmes, pas
  éparpillés partout :
  - `androidx.navigation3` (32) : navigation Android derrière `NavigationStack`. Pas d'équivalent CMP
    direct ; à réimplémenter ou à remplacer par un bouchon.
  - `androidx.core.view/content/app/graphics/util` (~18) : wrappers du framework Android.
  - `androidx.activity` + `ComponentActivity` + `activity.compose` + `activity.result` (~9) : point
    d'entrée Activity Android, plomberie permissions/résultats.
  - `androidx.lifecycle.*` (~8) : a désormais des variantes KMP ; partiellement portable.
  - `androidx.work` / WorkManager (~6) : travail en arrière-plan Android-only.
  - `androidx.window.core` (2), `androidx.annotation` (portable).
- Plus 55 imports `android.*` framework sur **20/280 fichiers** : `Context`, `Intent`, `Bitmap`,
  `VibrationEffect` (haptique), `Settings`, `Build`, `Log`, `Looper` (vérifications du thread principal).

`material-icons-extended` figure dans le version catalog mais **n'est pas un blocage** : il est
déprécié en amont et remplacé en pratique par des Material Symbols en vector drawable, donc on peut
simplement le retirer.

`android.os.Looper` (détection du main-thread) revient dans SkipUI/SkipModel/SkipFoundation, un
candidat `expect/actual` d'école.

### Classement selon les catégories d'erreur du PoC

- **(a) Config Gradle** : convertir les 6 modules `com.android.library` en KMP/JVM + JetBrains
  Compose. Gros mais mécanique, transverse.
- **(b) Dépendance Android à remplacer** : `navigation3`, `activity`/`activity-compose`, `appcompat`,
  `work`, `lifecycle-process`, `coil` (Android). Certaines ont un équivalent CMP, d'autres un bouchon.
- **(c) API SkipUI non portable** : les ~20 fichiers touchant `android.*` framework (Context, Intent,
  Bitmap, haptique, Settings, Build, Log, Looper). En partie traitables via `expect/actual` (Log, Looper,
  haptique), en partie plus dur (entrée Activity/Context/Intent).
- **(d) SkipFoundation non portable** : **11 fichiers** adossés à des API Android : i18n ICU
  (`android.icu.text.*`, formatteurs nombres/dates), `Context`/`SharedPreferences`/`PackageManager`/
  `AssetManager` (UserDefaults, Bundle, ProcessInfo), `Looper`, `android.net.Uri`, `android.util.Xml`.
  **C'est l'écart principal suspecté, et l'étape 5 confirme qu'il est réel, pas mineur** (voir plus bas).

### Tendance préliminaire (remplacée par les compilations réelles de l'étape 5)

L'écart est **concentré et structuré** (pas éparpillé partout), mais **substantiel** :

- SkipLib est portable ; l'UI de SkipUI est à ~96 % de l'API Compose que CMP reflète.
- **Mais** SkipFoundation n'est *pas* quasi-gratuit : 11 fichiers adossés à `Context`,
  `SharedPreferences`, `PackageManager` Android et à l'i18n ICU. Et tout ce qui est au-dessus en dépend.
- Le vrai travail : (1) re-plomber les 6 modules Gradle d'AAR Android vers KMP/JVM + JetBrains
  Compose, (2) réimplémenter la couche Android de SkipFoundation sur desktop (prefs, filesystem,
  ICU/`java.text`), et (3) `expect/actual`/bouchons desktop pour les surfaces Android de SkipUI
  (navigation3, entrée Activity, ~29 fichiers framework).

Sauvegardés pour référence :
[`docs/export-inspect/SkipUI.build.gradle.kts`](./docs/export-inspect/SkipUI.build.gradle.kts),
[`docs/export-inspect/settings.gradle.kts`](./docs/export-inspect/settings.gradle.kts).

### Temps passé

- Étape 4 : ~25 min (dont ~5 min de compilation d'export).

## Étape 5 : Cible CMP desktop JVM (macOS) (2026-07-11)

Méthode : plutôt que d'ajouter une cible desktop aux modules Android-library (bloqué au niveau du
plugin), prendre les **sources Kotlin exportées** et les compiler, module par module, sur une cible
`kotlin("jvm")` pure sans SDK Android. Cela isole « le Kotlin lui-même tourne-t-il hors Android » de
la plomberie Gradle. Projets jetables dans `cmp-attempt/` (ignoré par git).

### SkipLib → compile en JVM pur ✅ (1 correctif trivial)

```
cmp-attempt/skiplib-jvm  →  gradle compileKotlin  →  BUILD SUCCESSFUL
```

Premier essai en échec sur `Unresolved reference 'androidx'` : 42× `@androidx.annotation.Keep` émis
en annotations pleinement qualifiées inline. Correctif : ajouter `androidx.annotation:annotation:1.9.1`,
un artefact **KMP/JVM pur** (`@Keep` est un no-op hors Android). Ensuite, les 41 fichiers de SkipLib
compilent avec seulement `kotlin-reflect` + `kotlinx-coroutines-core`. **SkipLib est portable.**
Catégorie (a) échange de plugin + catégorie (b) une dep triviale.

### SkipFoundation → ne compile PAS ❌ (réimplémentation réelle nécessaire)

```
cmp-attempt/skipfoundation-jvm  →  gradle compileKotlin  →  BUILD FAILED, 416 erreurs
```

Les deps ajoutées (okhttp, commonmark, kxml2 pour `org.xmlpull`) se résolvent bien. Les échecs sont
des **API du framework Android sans équivalent sur le classpath JVM**, répartis sur les 11 fichiers
identifiés :

- **i18n ICU** : `android.icu.text.DecimalFormat`, `DecimalFormatSymbols`, `MessageFormat`,
  `RelativeDateTimeFormatter` (+ `RelativeUnit`/`AbsoluteUnit`/`Direction`), `Currency`, `ULocale`.
  Derrière `NumberFormatter`, `Formatter`, `RelativeDateTimeFormatter`. L'équivalent desktop existe
  (`java.text` / ICU4J `com.ibm.icu`) mais sous d'autres packages/API → réécriture, pas un échange.
- **Contexte applicatif / stockage** : `Context.getPackageName/getPackageManager/getPackageInfo/
  getFilesDir/getContentResolver/resources`, `SharedPreferences` (`edit`, `getAll`), `AssetManager`,
  `ApplicationInfo`. Derrière `UserDefaults`, `Bundle`, `ProcessInfo`. Nécessite un adossement desktop
  (`java.util.prefs`, filesystem, ressources du classloader).
- **os/util/net** : `android.os.Build.*` (infos appareil), `Looper` (main thread), `android.util.Log`,
  `android.util.Xml`, `android.net.Uri`, `android.os.Process`.

Rien de diffus *à l'intérieur* de ces fichiers, mais ce n'est **pas optionnel** : SkipFoundation est
une dépendance dure de SkipModel, SkipUI et de l'app, donc rien au-dessus ne compile sur desktop tant
que cette couche n'est pas portée. C'est exactement l'hypothèse « SkipFoundation est le suspect
principal » du brief, **confirmée**.

### Mise à jour : le « mur de compilation » était un problème de classpath (`android.jar`)

Ajouter le **bouchon** `android.jar` du SDK (`platforms/android-36/android.jar`) au classpath de
*compilation* (`compileOnly`) fait **compiler SkipFoundation avec 0 erreur**, et SkipModel aussi. Les
416 erreurs n'étaient donc pas « à réimplémenter » : c'était « symboles `android.*` absents du
classpath ». Ça colle au modèle de Skip : il teste son moteur d'exécution sur JVM via **Robolectric**, donc
`android.*` est censé être fourni hors appareil. `android.jar` est un bouchon (méthodes qui lèvent une
exception à l'exécution), donc cela permet la *compilation*, pas l'exécution sans appareil, mais pour les
chemins de code qu'une app desktop n'emprunte jamais, cela suffit.

### SkipUI → le vrai mur, quantifié

Compiler **tout SkipUI** sur JVM contre **JetBrains Compose desktop** (fournit `androidx.compose.*`
hors Android) + `android.jar` :

- Départ : 1100 erreurs. Ajout de Coil 3 (multiplateforme) + material-icons-extended JetBrains → **457**.
- Résiduel : `androidx.navigation3` (Android-only, pas d'équivalent CMP), `LocalContext`/
  `LocalConfiguration`/`LocalView`, `androidx.activity`, `androidx.core.*`, `material3.adaptive`,
  `dynamicColorScheme`, `stringResource`, `asAndroidPath`, `androidx.work`.
- **Pas modulaire par fonctionnalité.** Exclure les fichiers de fonctionnalités (TabView, Navigation, Image…) a
  *augmenté* les erreurs de `View.kt` de 68 à 264 : le cœur `View` (une **interface de 3561 lignes**)
  référence les types des fonctionnalités (`LocalNavigator`, `TabBarPreferenceKey`, `isViewPresented`…). On
  ne peut pas extraire une tranche triviale de SkipUI : c'est monolithique.

Porter le **vrai** SkipUI sur desktop signifie donc poser des cales ou porter toute sa surface Android-Compose
d'un coup : semaines-homme, plus le suivi de l'upstream Skip (estimation de ce portage, qui n'a pas été
tenté ; c'est une tâche différente et plus lourde que le chemin du POC 5, qui chevauche le Compose existant
de JetBrains).

### Temps passé

- Étape 5 : ~70 min (compilations JVM + `android.jar` + énumération SkipUI).

## Étape 6 : Tourner sur Compose Desktop (JVM/Skia) ✅

Plutôt que porter tout SkipUI, fournir la **fine tranche que le witness utilise réellement**. Un
adaptateur `skip.ui` desktop de ~200 lignes sur JetBrains Compose (`View`, `ComposeBuilder`,
`ComposeContext`, `ComposeResult`, `Renderable`, `State`, `LocalizedStringKey`, `VStack`, `HStack`,
`Text`, `Button`, `.padding()` ; ensuite `AppStorage` et `List` ont été ajoutés, voir les sections
falaise) compile le `ContentView.kt` transpilé par Skip **verbatim** et le rend.

Résultat : end-to-end fonctionnel (app dans [`desktop-witness/`](./desktop-witness/)) :

- [`docs/desktop-witness.png`](./docs/desktop-witness.png), Count: 0, bouton Material3.
- [`docs/desktop-witness-after-click.png`](./docs/desktop-witness-after-click.png), après un clic
  simulé, **Count: 1** : le compteur `@State` transpilé se met à jour et Compose recompose.

Rendu hors écran via `ImageComposeScene` (sans affichage ; `gradle renderPng`), et exécutable en vraie
fenêtre (`gradle run`). C'est **du CMP desktop sur JVM via Skia**, d'abord vérifié sur macOS.

### Exécution Linux (Docker) : confirmé ✅ (le « juge de paix » du brief)

La même app a produit son rendu **dans un conteneur Linux (arm64)**, sans affichage, avec le backend logiciel de Skia :
sortie identique : [`docs/linux-witness.png`](./docs/linux-witness.png) (Count: 0) et
[`docs/linux-witness-after-click.png`](./docs/linux-witness-after-click.png) (Count: 1 après le clic
simulé). Scripts de compilation réutilisables dans [`desktop-witness/docker/`](./desktop-witness/docker/)
(`Dockerfile` + `render-linux.sh`).

Deux pièges spécifiques Linux, tous deux mécaniques :

- **`libGL.so.1`** : la lib native de skiko `libskiko-linux-arm64.so` fait un `dlopen` de `libGL.so.1`
  **même en mode rendu logiciel**. Une image `eclipse-temurin:21-jdk` nue ne l'a pas, donc la JVM
  meurt sur `UnsatisfiedLinkError: … libGL.so.1: cannot open shared object file`. Correctif :
  `apt-get install libgl1` (plus `libx11-6 libxext6 libxrender1` pour AWT, `fontconfig libfreetype6
  fonts-dejavu-core` pour que le texte s'affiche). L'env `SKIKO_RENDER_API=SOFTWARE` sélectionne le
  backend CPU (pas de GPU/serveur X dans le conteneur).
- **Toolchain JDK** : `kotlin { jvmToolchain(17) }` a échoué dans le conteneur (seul le JDK 21 présent,
  pas de repo de download de toolchain) : `Cannot find a Java installation … matching
  languageVersion=17`. Réglé en `jvmToolchain(21)` (présent sur l'hôte et l'image).

Bilan : le witness rend à l'identique sur macOS et Linux. La stack Skia/JVM (`skiko`) est portable d'un OS à l'autre ;
le seul delta Linux est une poignée de paquets apt, désormais capturés dans le Dockerfile.

### Croissance de l'adaptateur : la courbe de coût, un point mesuré

Le witness a ensuite été monté d'un cran, toujours primitif : `VStack { Text; HStack { Button("-");
Button("+") }; if count > 0 { Text("Positive") } }`. Re-transpilé par Skip (`swift build`) et exécuté
sur macOS + Linux (Count: 0 → clic « + » → Count: 1 avec « Positive » qui apparaît, mêmes PNG, les
deux OS).

Coût de ces trois widgets SwiftUI ajoutés, côté adaptateur : **+1 symbole** (`HStack`, ~12 lignes ; un
`Row`). Le second `Button` réutilise le `Button` existant ; le conditionnel `if` est du **Kotlin pur**
dans la sortie transpilée, donc coût nul pour l'adaptateur. Donc pour des **widgets primitifs de
layout/contrôle**, l'adaptateur croît à peu près linéairement et à faible coût (≈ une petite fonction
par nouvelle primitive).

Le bémol porte sur la forme de la courbe, pas sur ce point : ça reste bon marché tant que l'app s'en
tient aux primitives qui correspondent 1:1 à JetBrains Compose. Le premier `List`, `NavigationStack`,
`Image`, `TextField`-avec-formatteur ou `@AppStorage` tire les fichiers monolithiques couplés Android
de SkipUI (navigation3, Coil, `LocalContext`, l'ICU/prefs de SkipFoundation), ce n'est plus un ajout
de symbole mais le portage de tout le moteur d'exécution mesuré en étape 5. **La largeur en primitives est bon
marché ; la première fonctionnalité adossée à Android est la falaise.**

### La falaise, touchée exprès : `@AppStorage`

Compteur changé en `@AppStorage("count") var count = 0` (persistance SwiftUI) et re-transpilé. Le
transpileur a remplacé `skip.ui.State<Int>` par **`skip.ui.AppStorage<Int>`**, et l'adaptateur
minimal a cessé de compiler : `Unresolved reference 'AppStorage'`. C'est la falaise à la compilation : un
property wrapper de persistance n'est *pas* une primitive gratuite.

Ce qu'il y a derrière chez le vrai Skip : `skip.ui.AppStorage` (**283 lignes**) délègue à
`skip.foundation.UserDefaults` (**611 lignes**), adossé à **`android.content.SharedPreferences` +
`Context`**. Pour franchir la falaise sur desktop, il faut fournir cette persistance soi-même.

Franchie : un `AppStorage<T>` desktop ré-adossé sur `java.util.prefs.Preferences` (l'équivalent
natif desktop de SharedPreferences). Résultat : **la persistance marche de bout en bout à travers les
redémarrages de processus** : l'exécution 1 démarre à Count 0, un clic `+` écrit 1 ; l'exécution 2 (une JVM
neuve) charge **Count 1** dès sa première frame sans aucun clic,
[`docs/desktop-witness-persisted.png`](./docs/desktop-witness-persisted.png). Re-rendu aussi sur Linux.

Coût, mesuré honnêtement :

- L'`AppStorage` desktop fait **~28 lignes**, mais c'est une **maquette** : il ne gère que les 5 types
  primitifs (`Int/Long/Boolean/Double/String`) dont le witness a besoin, contre les **894 lignes** de
  Skip (AppStorage 283 + UserDefaults 611) couvrant `Data`, tableaux, dictionnaires, `Date`, `URL`,
  `Codable` personnalisé via serializer/deserializer, et les change listeners.
- Il a fallu **choisir un backend de persistance desktop** et réimplémenter l'aiguillage par type, soit
  le *début* du portage de `UserDefaults` de SkipFoundation, pas une fonction de layout. Un `@AppStorage`
  d'une vraie app sur un enum/`Codable` (comme le template par défaut de Skip :
  `AppStorage<ContentTab>(… serializer/deserializer …)`) exigerait aussi le chemin de sérialisation.

Donc la falaise est **réelle mais franchissable sous-système par sous-système** : chaque fonctionnalité
adossée à Foundation (`@AppStorage`, formatteurs, `Bundle`, `FileManager`, dates) est son propre
ré-adossement desktop. Bon marché une fois, mais ça s'accumule, et c'est exactement le coût du moteur
d'exécution complet de l'étape 5, désormais montré fonctionnalité par fonctionnalité.

### Une seconde falaise, différente : `List` (côté UI, pas Foundation)

Ajout d'une `List { Text("Alpha"); Text("Bravo"); Text("Charlie") }` et re-transpilation. Falaise de
forme différente : **interne à SkipUI**, pas Android/Foundation.

- **Symptôme à la compilation.** Le transpileur émet `skip.ui.List { … }` ; sans `List` dans l'adaptateur,
  Kotlin retombe sur `kotlin.collections.List(size, init)` de la stdlib et échoue (`No value passed for
  parameter 'size'`, mismatch de lambda). Un conteneur UI n'est pas non plus une primitive gratuite.
- **Ce qu'il y a derrière.** Le vrai `skip/ui/List.kt` fait **1334 lignes**. Sa surface Android est
  légère (seulement Compose `material`/`animation`, tous deux en CMP), le poids est le modèle propre à
  SkipUI : un protocole `LazyItemFactory`, `EnvironmentValues` (×22), le diffing `ForEach`, les rangées,
  séparateurs, sélection, actions de swipe.
- **Franchie** avec un `List` de **9 lignes** ramené à un `LazyColumn`. Les trois rangées rendent sur
  macOS et Linux (`Count`, `-`/`+`, `Positive`, puis `Alpha`/`Bravo`/`Charlie`).

Même leçon, second axe : 9 lignes contre 1334 (~0,7 %). La maquette rend des rangées *statiques* et laisse
tomber tout ce qui fait qu'une `List` est une liste : virtualisation lazy, `ForEach`-sur-données,
rangées `NavigationLink`, swipe-to-delete (tout ce qu'utilise le template par défaut de Skip). Les deux
falaises sont donc distinctes : `@AppStorage` est un sous-système **adossé à Android** (ré-adosser sur
un store desktop) ; `List` est un sous-système **d'architecture SkipUI** (réimplémenter le modèle
lazy/environment). Les deux se franchissent en maquette ; le vrai comportement vit dans les 97-99 % qu'on
laisse tomber.

### La falaise la plus dure : `NavigationStack` (et une prédiction corrigée)

Ajout de `NavigationStack { … NavigationLink("Details"){ Text("Detail for \(count)") } … }
.navigationTitle("Witness")`. On l'attendait comme **la première falaise non franchissable en maquette**,
car le vrai `skip/ui/Navigation.kt` (**2326 lignes**) est bâti sur **`androidx.navigation3`** (
`NavKey`, `NavBackStack`, `NavDisplay`, `entryProvider`, `Scene`, transition specs), une **bibliothèque
Android-only sans aucun artefact Compose Multiplatform** (contrairement aux deps de List, toutes en
CMP). Sur la route *porter-le-vrai-SkipUI*, c'est le pire mur : il faudrait réimplémenter navigation3
lui-même ou réécrire 2326 lignes sur une autre bibliothèque de nav.

**La prédiction était fausse pour la route adaptateur.** Comme l'adaptateur ne touche jamais
navigation3, un back-stack maison de **42 lignes** (un `SnapshotStateList<View>` dans un
`CompositionLocal`, push au clic de `NavigationLink`, pop au Back) donne une **vraie navigation
push/pop** sur du Compose pur. Cliquer « Details » pousse la destination ; l'écran de détail avec un
bouton « < Back » apparaît, sur macOS et Linux. 42 lignes contre 2326 (~1,8 %), en laissant tomber
les transitions de navigation3, le predictive-back, les deep links et la restauration d'état.

### Synthèse : les deux routes, et pourquoi elles pointent toutes deux vers Compose-first

La série de falaises (`@AppStorage`, `List`, `NavigationStack`) clarifie le vrai choix :

- **Porter le vrai moteur d'exécution Skip vers CMP.** Bute sur son pire mur exactement à `navigation3` (pas
  d'artefact CMP), plus la couche Android de SkipFoundation. Mois-homme + suivi de l'upstream.
- **Adaptateur par app (ce que ce PoC a construit).** *Chaque* fonctionnalité se révèle franchissable en maquette
  sur du Compose pur, car l'adaptateur contourne les deps Android de Skip et ne réimplémente que le
  comportement utilisé par l'app. Mais c'est le piège : étendu à une vraie app, l'adaptateur **est** un
  moteur d'exécution SwiftUI-sur-Compose écrit à la main, soit exactement le travail de Skip, en moins fidèle. On
  converge vers « réimplémenter la couche UI de Skip soi-même », c'est-à-dire le NO-GO du brief,
  atteint incrémentalement.

Dans les deux cas, pour une vraie app on finit par **posséder un moteur d'exécution SwiftUI→Compose** (porté ou
réécrit). Pour l'appareil cible, écrire du Compose directement, c'est le même travail sans l'indirection du
transpileur. **Compose-first tient, désormais démontré sous trois angles au lieu d'être argumenté.**

## Étape 7 : Décision go/no-go

### Question, tranchée

*Combien de travail pour faire tourner une app SwiftUI transpilée par Skip sur CMP desktop ?* → Ça
**marche aujourd'hui pour un écran plancher** ; le coût croît avec la part de SwiftUI/SkipUI qu'utilise
l'app.

### Preuves mesurées (récap)

- `skip export` = un vrai projet Gradle autonome, mais **Android-only** (6× `com.android.library`).
- **SkipLib / SkipFoundation / SkipModel compilent sur JVM nue** avec `androidx.annotation` +
  `android.jar` au classpath. Le « mur de compilation » était du classpath, pas de la réécriture.
- **SkipUI complet** ne compile pas hors Android sans caler toute sa surface Android (navigation3,
  activity, adaptive, `LocalContext`…) ; c'est une **interface `View` monolithique de 3561 lignes**,
  non découpable par fonctionnalité.
- **L'app transpilée** tourne e2e sur CMP desktop via un **adaptateur de ~200 lignes**, prouvé par un
  compteur vivant et interactif.

### Face aux critères de décision

- **GO** (*app triviale rendue sur CMP desktop avec ajustements localisés et compréhensibles*) →
  **atteint pour le plancher.** Le witness s'affiche et est interactif ; l'adaptateur est petit et
  compréhensible.
- **NO-GO** (*`android.*` diffus et non isolable → faire tourner l'UI = réimplémenter Skip*) →
  **atteint pour une vraie app.** Pour utiliser la largeur de SkipUI (listes, navigation, images,
  formatteurs, persistance) il faut porter le vrai moteur d'exécution SkipUI (monolithique, couplé Android),
  de fait maintenir un fork de Skip.

Les deux critères se déclenchent, à des tailles d'app différentes. C'est ça le constat : **la
faisabilité est réelle, le coût est fonction de la surface utilisée.**

### Décision

- **Pour une UI jetable/triviale** : GO. La sortie transpilée est réellement portable ; un petit
  adaptateur suffit, comme démontré.
- **Pour l'appareil cible (une vraie app)** : **NO-GO sur le chemin transpileur Skip→CMP.** L'adaptateur par app
  grossit à chaque fonctionnalité SwiftUI, et l'alternative (porter le vrai SkipUI + la couche Android de
  SkipFoundation vers KMP/JVM et suivre l'upstream Skip) est en semaines/mois-homme plus maintenance
  permanente. **Recommandation : Compose-first pour l'appareil cible**, en notant que le chemin transpile→CMP
  n'est *pas* impossible : c'est un arbitrage coût/maintenance, désormais quantifié.

### Estimation d'effort (estimation étiquetée, non mesurée)

- App triviale sur adaptateur par app : **heures** (fait ici).
- Vraie app, portage du vrai moteur d'exécution Skip : restructuration Gradle (6 modules → KMP/JVM) quelques
  jours ; couche desktop de SkipFoundation ~1-2 semaines ; portage de **toute la surface** SkipUI
  plusieurs semaines ; puis suivi continu de l'upstream. Ordre de grandeur : **mois-homme** + coût
  permanent.

### Ce qui ferait basculer le cas « vrai app » en GO

- Skip upstream livrant officiellement une cible CMP/desktop (la discussion #163 montre de l'intérêt,
  pas de livraison), alors le portage du moteur d'exécution est *leur* maintenance.
- Ou une app volontairement limitée à la tranche SkipUI déjà couverte par un petit adaptateur.

### Méta-résultat (le délai imparti est un résultat)

Boucle complète (de zéro à un écran Skip transpilé interactif sur CMP desktop) en **~3h**, très en
deçà du délai imparti de 3 jours. Deux notes méthodo pour le journal : (1) le comptage étape 4 par imports
seuls sous-estimait (Skip émet des refs pleinement qualifiées inline) ; les compilations réelles ont
corrigé : **se fier au compilateur, pas au grep**. (2) Le premier « mur de compilation »
(SkipFoundation) s'est dissous dès `android.jar` au classpath : **distinguer un manque de classpath
d'une réécriture** avant de conclure.

## Références

- Skip : https://skip.dev
- skiptools/skip (SwiftUI natif pour iOS + Android) : https://github.com/skiptools/skip
- L'origine de ce POC, la discussion « Why not Compose Multiplatform? » (`skiptools#163`) :
  https://github.com/orgs/skiptools/discussions/163 . Le mainteneur de Skip explique que CMP n'est pas
  une cible prévue (rendu sur un canvas Skia, expérience iOS en retrait ; Skip reste focalisé sur le
  mobile natif), mais suggère d'exporter vers un projet Gradle et de tester d'autres cibles CMP
  soi-même, en signalant les obstacles de dépendances Android. Ce POC est cette expérience : il a
  trouvé le couplage Android assez diffus pour rendre un transpileur propre NO-GO.
- Compose Multiplatform : https://kotlinlang.org/compose-multiplatform/
