# FINDINGS : POC 5 : builder le VRAI `compose.ui` pour Kotlin/Native Linux (chemin A, GLFW)

> Canonique : [`FINDINGS-POC5.md`](./FINDINGS-POC5.md) (anglais). POC 1-4 clos. Le POC 4 a prouvé un
> `ui-glfw` *minimal* écrit à la main (sans material3). Le POC 5 est le chemin A : compiler le **vrai**
> `compose.ui`/`foundation`/`material3` de JetBrains pour Linux K/N + brancher un vrai backend
> `ui-glfw`, pour que de vraies apps Compose (widgets material3) s'exécutent sur K/N Linux, sans JVM.

**C'est un effort de build multi-sessions.** Par jalons ; cette session fait le premier pas borné.

## Jalon 0 : Cadrage ✅

- **Toute la pile Compose UI est non publiée pour Linux K/N** (tout en 404) : `ui-unit`, `ui-geometry`,
  `ui-graphics`, `ui-text`, `ui-util`, `ui`, `ui-backhandler`, `foundation-layout`, `foundation`,
  `material`, `material3`. Il faut donc tout builder depuis les sources.
- Les sources sont dans **`JetBrains/compose-multiplatform-core`** (`jb-main`), le **fork du monorepo
  AndroidX** (`build-fork.gradle`). Ses cibles natives ne sont activées que pour ios/macos ; ajouter
  Linux dans sa convention de build + lancer le build androidx est lourd (JDK figé, prebuilts, Go).
- **Structurellement propre** (POC 4 Jalon 0) : le `nativeMain` de `compose.ui` est agnostique de
  plateforme (`ui-uikit` isolé dans `iosMain`) ; les deps natives partagées (runtime, lifecycle,
  coroutines, internes annotation/collection de Compose) ont toutes des klibs K/N Linux.

**Deux routes de build :** (A1) forker le monorepo et ajouter les cibles Linux à son build (lourd,
upstream-able) ; (A2) **extraire les sources des modules et les compiler dans un projet KMP `linuxArm64`
simple** contre les deps publiées (la méthode du POC 1 ; évite le build androidx). Cette session teste
**A2** sur les plus petits modules de base (bas de l'arbre de dépendances : `ui-util`, `ui-geometry`, `ui-unit`).

## Jalon 1 : Les modules de base du vrai compose.ui compilent pour Linux K/N ✅

Clone partiel de `compose/ui/{ui-util,ui-geometry,ui-unit}` depuis `compose-multiplatform-core` et
compilation pour `linuxArm64` dans un projet KMP simple (`poc5-native/`), contre les deps **publiées**
(`compose.runtime`, `compose.collection-internal`, `compose.annotation-internal`).

- Premier essai (tous les source sets aplatis en un) : ~40 erreurs, toutes dues à la casse du
  mécanisme expect/actual de KMP (expect+actual dans le même source set) et `@OptionalExpectation`.
- Fix : **préserver la hiérarchie KMP**, `commonMain` reçoit les `commonMain` des modules (les
  `expect`), `linuxArm64Main` reçoit `nonJvmMain`/`nonAndroidMain` (les `actual`). → **2 erreurs**.
- Ces 2 : les expects `trace`/`traceValue` de `ui-util` n'avaient pas d'actual Native (le systrace
  Android n'a pas d'équivalent Linux). Fourni un **actual no-op de 3 lignes**. → **`BUILD SUCCESSFUL`.**

**Donc les vrais modules fondamentaux de compose.ui de JetBrains portent vers Linux K/N quasi tels
quels** : le seul code spécifique plateforme pour ces modules de base était un trace no-op trivial. Ça valide
la route A2 : la source compose.ui est portable ; le travail est (a) préserver la structure des source
sets, (b) fournir les `actual` plateforme Native. Le gros de (b) pour les modules supérieurs EST le
backend `ui-glfw` (ComposeScene ↔ fenêtre/entrées/polices).

## Jalon 2 : Étendre vers ui:ui / ComposeScene 🟡 (vrai progrès + friction cartographiée)

Étendu la route A2 dans la chaîne de deps. **`ui-graphics`, la couche graphique adossée skiko (103
fichiers : Canvas, Paint, Path, ImageBitmap, GraphicsLayer…), compile pour Linux K/N** au-dessus des
modules de base. Désormais `ui-util + ui-geometry + ui-unit + ui-graphics` buildent tous (`BUILD SUCCESSFUL`,
`poc5-native/`).

Ce qu'il a fallu (tout mécanique, aucun blocage fondamental) :

- **Une vraie hiérarchie de source sets KMP**, pas plate : `commonMain` (expects) → `skikoMain`
  intermédiaire (non-JVM/skiko partagé) → `linuxArm64Main` (actuals natifs). Aplatir casse
  expect/actual (« declared in the same module »).
- Mapper les source sets des modules dessus : `commonMain`→common ; `nonJvmMain`/`skikoMain`→skikoMain ;
  `nativeMain`/**`skikoExcludingWebMain`** (skiko desktop+natif, hors web)→natif. `darwinMain` (Apple)
  exclu.
- Ajout de la dep `org.jetbrains.compose.runtime:runtime-saveable` (l'API `Saver`).

**Deux types de friction sont apparus à `ui-text`, tous deux désormais diagnostiqués :**

1. **Hiérarchie de source sets (réglée).** La vraie hiérarchie jb-main est
   `commonMain → skikoMain → nonJvmMain → nativeMain`, j'avais au départ `skikoMain`/`nonJvmMain`
   **inversés** (donc le `CharHelpers` de `nonJvmMain` ne voyait pas `CodePoint`/`StrongDirectionType`
   définis dans `skikoMain`). Lire le graphe `dependsOn` de `ui-text/build.gradle` et le reproduire a
   fait passer `ui-text` de 699 → 11 → **4 erreurs**.
2. **Alignement de version skiko (les 4 dernières erreurs).** Avec skiko épinglé en **0.150.1** (la
   valeur du catalogue jb-main) et la hiérarchie correcte, `ui-text` compile *sauf*
   `NativeFont.native.kt` : il appelle `org.jetbrains.skia.FontStyle(weight = FontWeight(..),
   width = FontWidth.NORMAL, slant = ..)`, mais le constructeur `FontStyle` de skiko 0.150.1 public est
   `(Int, Int, FontSlant)`. Donc **jb-main HEAD cible une skiko plus récente que la 0.150.1 publiée**
   (un build dev avec l'overload `FontWeight/FontWidth`). Un détail d'alignement de version dans **un
   seul fichier de font**, pas un blocage de portabilité.

Toute la pile **graphics + text** est donc à ~4 lignes d'API skiko de compiler pour Linux K/N. `ui:ui`
(où vit `ComposeScene`, ~349 fichiers) est conditionné à un `ui-text` entièrement vert. L'atteindre,
et énumérer ses `expect` plateforme (la surface exacte du backend `ui-glfw`), est l'étape suivante,
une fois le build (dev) exact de skiko épinglé.

### `ui-text` compile : premières pièces du backend plateforme Linux écrites

Plutôt que chasser la skiko dev non publiée, j'ai **remplacé par une cale l'unique ligne `FontStyle`** (un
`NativeFont.native.kt` patché, l'original HEAD exclu dans Gradle) vers l'API publique
`FontStyle(Int, Int, FontSlant)`. Ça a fait apparaître **5 `expect` plateforme sans actual Linux** :
les équivalents darwin sont dans `darwinMain` (NSLocale/CoreText). J'ai fourni des actuals Linux
minimaux :

- `Locale` (expect class) + `createPlatformLocaleDelegate()` + `Locale.isRtl()` : i18n/locale ;
- `ActualStringDelegate()` : string ops plateforme (upper/lower/(de)capitalize) ;
- `createPlatformResolveInterceptor()` : hook de résolution de polices (no-op).

→ **`ui-text` compile pour Linux K/N.** Cinq vrais modules compose.ui buildent désormais :
`ui-util + ui-geometry + ui-unit + ui-graphics + ui-text`. Ces actuals (+ `trace`, + la cale FontStyle)
sont les **premières pièces concrètes du backend plateforme Linux** : la spec `ui-glfw`/texte, énumérée
par le compilateur, pas devinée.

### `ui:ui` (ComposeScene) atteint : surface énumérée

Ajout de `ui:ui` (244 commonMain + 92 skikoMain + …). **`ComposeScene` lui-même est dans `skikoMain`**
(`scene/ComposeScene.skiko.kt`), agnostique de plateforme, compilable ; seul le **médiateur**
(`ComposeSceneMediator.ios.kt` / `.desktop.kt`, window+entrées) est spécifique plateforme = le backend
`ui-glfw`. Compiler `ui:ui` a donné **847 erreurs, mais dominées par des *dépendances* manquantes, pas
des trous plateforme** : `androidx.navigationevent`, plus de `savedstate`/`lifecycle-viewmodel-savedstate`,
une API `retain`. Même pattern que `runtime-saveable` : ajouter les deps publiées → le compte
s'effondre → la surface d'`expect` plateforme (le médiateur = `ui-glfw`) reste. **Aucun mur fondamental
à ComposeScene.**

### Résultat de la chasse aux deps : la route A2 converge vers la route A1 à `ui:ui`

Chasser les deps de `ui:ui` révèle le constat final : certaines ne sont **pas résolvables contre les
artefacts publiés**.

- `lifecycle-viewmodel-savedstate`, `savedstate-compose` : publiés pour K/N Linux ✅.
- **`androidx.navigationevent`** (+ `navigationevent-compose`) : importé par `ui:ui`, **aucun klib
  K/N Linux publié** (404). Il *est* dans le monorepo, donc buildable via A2, mais c'est un module de
  plus.
- **`androidx.compose.runtime.retain.*`** (`RetainedValuesStore`, `retain`, `RetainObserver`, …) : une
  **API récente de compose.runtime absente de la runtime 1.9.0 publiée.** L'utiliser exige une runtime
  plus récente (non publiée pour K/N Linux), ou builder la runtime depuis les sources.

Donc le graphe de deps de `ui:ui` HEAD **dépasse les artefacts K/N Linux publiés** : il faut des API
HEAD-only (`retain`) et des modules du monorepo non publiés (`navigationevent`). La route A2 (contre
les deps publiées) **ne se termine donc pas à `ui:ui` : elle converge vers la route A1** (build du
monorepo depuis HEAD), parce que **jb-main HEAD est la seule source avec support K/N Linux et il est en
avance sur les releases.**

**Observation clé : c'est un rollout officiel en cours.** JetBrains a *déjà publié* les klibs K/N Linux
de la **fondation** (skiko, runtime, runtime-saveable, lifecycle, savedstate, collection, annotation)
mais **pas encore** `ui`/`foundation`/`material3`/`navigationevent`. Cette publication partielle est la
signature d'un **rollout en cours** : la couche UI de Compose-sur-K/N-Linux arrive de chez JetBrains.
Les options pragmatiques sont donc (1) builder depuis HEAD maintenant (route A1, tout le monorepo, deps
alignées), ou (2) attendre les artefacts officiels K/N Linux `ui`/`material3`.

### Évaluation

La vraie pile compose.ui **porte vers Linux K/N par du travail mécanique** : mapping de source sets, une
cale de version skiko, ajouts de deps publiées, et un ensemble croissant de petits actuals natifs (le
backend plateforme Linux). **Aucun blocage fondamental sur cinq modules (dont les couches graphics +
text skiko), et `ComposeScene` est dans du code skiko partagé.** Le coût restant est (a) du volume,
chasse aux deps + finir `ui:ui`/`foundation`/`material3`, et (b) le vrai backend plateforme `ui-glfw`
(le médiateur `ComposeScene` : window, entrées, polices, densité), que le compilateur nous énumère
désormais.

### PERCÉE : `ui:ui` + `ComposeScene` compilent pour linuxArm64 K/N (route A2, sans build monorepo)

Le « mur » décrit ci-dessus (l'API `retain` exige une runtime non publiée, `navigationevent` non publié)
**est tombé.** La route A2 se termine bien à `ui:ui` : `BUILD SUCCESSFUL`, `ui:ui` **et `ComposeScene`
compilent en klib Kotlin/Native linuxArm64**. Deux leviers ont levé le blocage :

1. **Monter les deps publiées vers leurs versions K/N Linux les plus récentes.** Les klibs K/N Linux
   n'existent qu'aux versions de tête du rollout, pas aux versions stables que je figeais : `compose.runtime`
   **1.12.0-beta01** (au lieu de 1.9.0 → fournit les API HEAD `HostDefaultKey`, `compositionLocalWithHostDefaultOf`,
   `CancellationHandle`, `scheduleFrameEndCallback`…), `lifecycle` **2.11.0-rc01** (le K/N Linux de lifecycle
   ne commence qu'à cette version), `savedstate` 1.3.0-alpha06, `savedstate-compose` 1.4.0. Le décalage
   n'était pas HEAD-vs-publié, c'était publié-stable-vs-publié-tête.
2. **Compiler depuis les sources 3 petits modules non publiés** (`runtime-retain`, `navigationevent`,
   `navigationevent-compose`) via la même mécanique A2 (ajout de source sets pointant le monorepo). Aucun
   d'eux ne dépend d'`android.*`.

Restait alors un compte de **795 erreurs qui s'est effondré à 4 puis à 0** en deux gestes purement de
configuration :

- **765 des 795 étaient un lint d'opt-in** (« Unstable API for use only between compose-ui modules sharing
  the same version », soit `@InternalComposeUiApi`), pas de vraies erreurs. Le vrai build de `ui:ui` fait
  un opt-in **global** à ces annotations ; en le répliquant (`languageSettings.optIn(...)` pour
  `InternalComposeUiApi`, `ExperimentalComposeUiApi`, `InternalComposeApi`, `ExperimentalComposeRuntimeApi`,
  `InternalTextApi`…), les 765 disparaissent.
- Les 4 dernières unresolved venaient d'un module manquant, `lifecycle-viewmodel-compose:2.11.0-rc01`
  (package `androidx.lifecycle.viewmodel.compose` : `LocalViewModelStoreOwner`, `ViewModelStoreOwnerHostDefaultKey`).

#### La surface du médiateur est bornée : 20 actuals de plateforme, énumérés

Une fois les deps réglées, `ui:ui` ne réclamait plus que **20 déclarations `expect` sans `actual`** pour la
cible native. C'est **exactement la frontière du médiateur** (ce que POC 3/4 ont dé-risqué), pas de l'`android.*`
diffus. Décompte du travail réel :

- **5 fichiers réutilisés verbatim depuis `macosMain`** (natif K/N, sans API Apple) : `InteropView` (=`Any`),
  `DragAndDrop` (classes bouchons), `Focusability` (`systemDefinedCanFocus = true`), `PlatformVelocityTracker`
  (`= Lsq2VelocityTracker()`), et surtout **`Key.macos.kt`** (582 lignes de table de key codes, pur Kotlin).
- **3 actuals `expect` de backhandler** (`BackHandler`, `PredictiveBackHandler`, `BackEventCompat`) fournis
  par le source set **partagé `jbMain`** d'`ui-backhandler` (aucune réécriture).
- **3 seuls fichiers touchant une API de plateforme**, remplacés par un bouchon en Linux (~60 lignes au total) :
  `PointerIcon` (objets curseur marqueurs), `PlatformClipboard` (presse-papiers en mémoire),
  `PlatformUriHandler` (no-op, TODO `xdg-open`).

Autrement dit, la partie « spécifique plateforme » de `ui:ui` pour Linux se résume à **3 petits bouchons** ;
tout le reste est soit du code skiko partagé, soit réutilisable depuis le natif Apple existant.

#### Preuve

Sur le klib linuxArm64 produit, `klib dump-metadata` liste bien, en `builtins_platform=NATIVE`,
`compiler_version=2.3.0`, cible `linuxArm64` :

```
interface androidx/compose/ui/scene/ComposeScene
abstract class androidx/compose/ui/scene/BaseComposeScene : ComposeScene
final class androidx/compose/ui/scene/CanvasLayersComposeSceneImpl : BaseComposeScene, ComposeSceneContext
class androidx/compose/ui/scene/PlatformLayersComposeScene ...
```

**Conséquence directe sur la décision.** La route A2 **n'a PAS eu besoin de builder le monorepo** (route A1),
contrairement à ce que la section précédente concluait. `ComposeScene` s'exécute sur du code skiko partagé
compilé pour K/N Linux, contre des artefacts **publiés** + 3 modules de sources + une poignée d'actuals. Le
seul morceau restant pour *afficher* une fenêtre est le **médiateur `ui-glfw`** (brancher `ComposeScene` sur
une fenêtre GLFW : window, entrées, polices, densité), dont la plomberie GLFW + skiko a déjà été prouvée aux
POC 3/4. **Signal GO net : l'écart n'est pas « android.* diffus », c'est un médiateur borné et énumérable.**

### Temps passé

- Jalon 2 (cette session) : ~40 min (reconnaissance + compilation des modules de base).
- Jalon 2 (extension, cette session) : ~50 min (`ui:ui` + `ComposeScene` verts pour K/N Linux).

## Jalon 3 : `foundation` + `material3` compilent pour linuxArm64 K/N

`gradle compileKotlinLinuxArm64` → **BUILD SUCCESSFUL, 0 erreur.** Toute la pile UI de Compose compile
désormais en un klib Kotlin/Native linuxArm64 : `ui` (Jalon 2) **plus** `foundation`, `foundation-layout`,
`animation`, `animation-core`, `material-ripple`, `graphics-shapes` et `material3`. Le `klib dump-metadata`
exporte les composables réels : **`Button`, `Text`, `Card`, `Checkbox`, `Scaffold`, `Slider`,
`OutlinedTextField`, `DatePicker`** (material3) et **`LazyColumn`, `LazyRow`, `BasicTextField`, `Canvas`,
`Image`** (foundation), 50 packages `material3`/`foundation`/`animation`.

### Recette (deltas par rapport au Jalon 2)

- **Modules ajoutés depuis les sources** (aucun publié pour K/N Linux, tous 404) : `animation-core`,
  `animation` (avec son source set `nonAndroidMain`), `foundation-layout`, `foundation`, `material-ripple`
  (avec `nonAndroidMain`), `graphics-shapes`, `material3`.
- **Deps publiées ajoutées / montées en version** (consigne : viser les dernières versions K/N Linux, alphas comprises,
  car le K/N Linux n'existe qu'en tête de rollout) : `kotlinx-datetime:0.7.1` (DatePicker),
  `collection`/`annotation` **1.12.0-alpha02** (vs 1.10.0).
- **Opt-ins globaux** étendus : `ExperimentalFoundationApi`, `InternalFoundationApi`,
  `ExperimentalLayoutApi`, `ExperimentalAnimationApi`, `ExperimentalMaterial3Api`,
  `ExperimentalMaterial3ExpressiveApi`, `InternalMaterial3Api`, `ExperimentalGraphicsShapesApi`.
- **Heap Gradle/Kotlin** monté à 8 Go (`gradle.properties`) : à cette échelle (metadata ~393k lignes) le
  daemon partait en thrashing GC à 512 Mo.

### Surface plateforme : encore bornée, majoritairement gratuite

- **`foundation` : 44 actuals `expect`**, tous présents dans `macosMain` (natif K/N). Les **24 fichiers
  `foundation/macosMain` sont sans API Apple** et réutilisés verbatim ; **3 seuls** fuitaient un bit AppKit,
  patchés en une ligne chacun : `platformScrollConfig` (deltas molette → bouchon, le médiateur fournira),
  `NativeClipboard.hasText`/`pasteboardItems` (adaptés à mon presse-papiers en mémoire du Jalon 2).
- **`animation-core` : 1 actual**, `getCurrentThread(): Any`, écrit en 3 lignes via `@ThreadLocal` (identité
  par thread, ce qu'exige le check de `rememberTransition`).
- **`material3` : 5 actuals.** `PlatformRipple`/`createPlatformRippleNode` viennent gratuitement de
  `material-ripple/nonAndroidMain`. Les 3 restants sont la **couche i18n du DatePicker** (`CalendarLocale`,
  `defaultLocale`, `PlatformDateFormat`), écrite sur **kotlinx-datetime** avec libellés anglais (~130 lignes,
  qualité POC) là où macOS s'appuie sur `NSDateFormatter`/`NSLocale`. `createCalendarModel` et le gros du
  calendrier sont déjà du code **skiko partagé** (`KotlinxDatetimeCalendarModel`).

### Conséquence sur la décision

La pile complète `ui` + `foundation` + `material3` **porte vers Linux K/N par du travail mécanique** :
mapping de source sets, deps publiées de tête, réutilisation des actuals natifs Apple existants, et une
poignée de petits actuals Linux. Le **seul** code réellement nouveau et non trivial écrit à ce stade est la
**couche i18n du DatePicker** (localisée, remplaçable par ICU plus tard). Il ne reste qu'**un** morceau pour
*afficher et animer* une vraie UI material3 sur K/N Linux sans JVM : le **médiateur `ui-glfw`** (Jalon 4),
dont la plomberie GLFW + skiko est déjà prouvée aux POC 3/4.

### Temps passé

- Jalon 3 (cette session) : ~1 h (foundation + material3 + deps + actuals date/locale).

## Jalon 4 : le médiateur `ui-glfw` rend et anime du VRAI material3 sur K/N Linux, sans JVM

**Atteint, prouvé par capture.** Le vrai `ComposeScene` de JetBrains (compilé aux Jalons 2-3) est branché
sur une fenêtre **GLFW** via une surface **skiko GL**, et rend un contenu **material3 authentique**
(`MaterialTheme` + `Button` + `Text`), interactif, sur Linux arm64 **sans JVM**.

- Avant : [`docs/poc5-material3-knative-arm64.png`](./docs/poc5-material3-knative-arm64.png) (`count: 0`).
- Après un clic (synthétique, injecté au centre du bouton) :
  [`docs/poc5-material3-after-knative-arm64.png`](./docs/poc5-material3-after-knative-arm64.png)
  (`count: 1`). Le `onClick` du vrai `Button` material3 passe par le **hit-testing + le layout réels** du
  `ComposeScene`, déclenche la recomposition, et le re-rendu skiko. Ce n'est **pas** le rendu à la main de
  POC 4 : c'est le composable `Button` de material3, avec la couleur primaire de la charte, la forme et le
  libellé blanc.

### Le médiateur (~180 lignes, `main.kt`)

Le patron est celui d'`ImageComposeScene` (skiko partagé), étendu avec fenêtre + entrées :

1. `CanvasLayersComposeScene(frameRecomposer, density, size, platformContext)` + `setContent { App() }`.
2. `PlatformContext` minimal avec un `WindowInfoImpl` (taille conteneur). **Astuce clé :** le médiateur est
   **compilé dans le même module** que les sources de Compose, donc ses `internal` (`WindowInfoImpl`,
   `CanvasLayersComposeScene`, `FrameRecomposer`, `PlatformContext.Empty`) sont directement accessibles.
3. Boucle par frame : `drivePostDelayed(t)` → `frameRecomposer.performFrame(t)` → `scene.measureAndLayout()`
   → `scene.draw(skiaCanvas.asComposeCanvas())` → `context.flush()` → `glfwSwapBuffers`.
4. Entrées : clic GLFW → `scene.sendPointerEvent(Press/Release, position, PointerButton.Primary)`.
5. Socle GLFW + skiko GL (cinterop `glfw.def`, `DirectContext.makeGL`, `Surface.makeFromBackendRenderTarget`)
   **réutilisé de POC 4**, ainsi que le vendoring des `.so` linux arm64 (glfw/GL/EGL/fontconfig/freetype).

### Le seul mur plateforme rencontré à l'exécution : `Dispatchers.Main`

Le binaire démarrait (glfw + GL + surface skiko OK) puis crashait à la création du `ComposeScene` :
`Dispatchers.Main is missing on the current platform`. Cause : `compose.ui#postDelayed` (utilisé par le
debounce de `RectManager`) s'exécute sur `Dispatchers.Main`, **absent en K/N Linux**. Corrigé en remplaçant
`postDelayed`/`removePost` par un **ordonnanceur drainé par la boucle de frames** (`LinuxPostDelayed.kt`) :
les callbacks s'exécutent sur le **thread compose**, exactement là où le debounce de `RectManager` doit
s'exécuter. Deux fichiers d'actuals skiko/nonJvm exclus et réimplémentés en Linux (l'`expect`
`PostDelayedDispatcher` disparaît avec eux). C'est précisément le genre de petit actual que possède la
couche médiateur.

### Build & run

- **Cross-compilé ET cross-linké depuis le host macOS** vers un `.kexe` linuxArm64 (K/N n'a pas de host
  linux-aarch64 ; l'édition de liens résout les `.so` vendored). Binaire **debug : 70 Mo**.
- **Exécuté dans Docker Linux arm64** (`debian:bookworm-slim` + mesa/llvmpipe + libglfw3 + fontconfig +
  freetype + polices DejaVu), sous **Xvfb** (GL logiciel). 120 frames, sortie propre, PNG capturés.
- Le rendu est **software (llvmpipe)** : la fluidité GPU sur matériel réel reste à confirmer (même réserve
  que POC 2/3/4).

### Temps passé

- Jalon 4 (cette session) : ~1 h 15 (médiateur + cinterop + correctif `Dispatchers.Main` + exécution Docker + preuve clic).

## Jalon 5 : mesure poids/RAM K/N vs JVM (le but de toute la série)

Mesuré sur le binaire **release** (K/N Linux arm64, la même app material3 du Jalon 4), face aux chiffres JVM
du POC 2 (même classe d'app, Linux arm64) :

| Métrique | POC 2 (CMP Desktop JVM) | POC 5 (K/N Linux, sans JVM) | Gain |
|---|---|---|---|
| Poids livré | **137 Mo** (app image, dont JRE 87 Mo) | **35 Mo** (binaire release autonome) | **~74 %** |
| RAM au repos (RSS pic) | **~224 Mo** | **124 Mo** | **~45 %** (~100 Mo) |

- Le poids chute de 137 à 35 Mo : la **JRE de 87 Mo disparaît** (le binaire K/N n'embarque pas de JVM), et
  le reste est du natif lié. Le debug non optimisé pèse 70 Mo ; le release, 35 Mo.
- La RAM tombe de ~224 à 124 Mo. La baisse est réelle (~100 Mo) mais pas proportionnelle au poids : **Skia
  domine la RAM** quel que soit le moteur d'exécution (constat déjà fait au POC 3). Virer la JVM enlève son surcoût,
  pas l'empreinte de Skia.
- **Réserve :** RSS mesuré en **rendu software (llvmpipe)** sous Xvfb ; sur GPU réel, le profil mémoire
  diffèrera. Chiffre indicatif, cohérent avec les réserves des POC 2/3/4.

**Conclusion de la série.** L'objectif initial (virer la JVM des 137/224 Mo du POC 2) est **atteint et
chiffré** : une vraie app material3, rendue et interactive, s'exécute en **Kotlin/Native Linux sans JVM**, pour
**35 Mo / 124 Mo**. Le coût est de **maintenir un backend `ui-glfw` hors-JetBrains** (le médiateur + une
poignée de petits actuals Linux), tant que JetBrains n'a pas publié les artefacts K/N Linux de la couche UI
(le poll Maven surveille exactement ça).

### Temps passé

- Jalon 5 (cette session) : ~20 min (build release + mesure RSS).

## Jalon 6 : la même pile en linuxX64 (x86_64), sans code spécifique à l'architecture

Les Jalons 1 à 5 visaient tous **linuxArm64**. SKIKO-611, le ticket qui traque le K/N Linux, parle de
**x86_64**. La question ouverte était donc : est-ce que tout ça se transpose à l'autre architecture, ou
bien arm64 avait-il quelque chose de particulier ?

Ça se transpose, et il n'y avait rien à porter.

**Ce qui a changé** (plomberie de build uniquement, zéro Kotlin écrit) :

- `src/linuxArm64Main/` -> `src/linuxMain/`, un source set partagé par les deux cibles Linux. C'est la
  structure qu'utilise la PR upstream #2027 (`compose/ui/ui/src/linuxMain/...`).
- Cible `linuxX64` déclarée à côté de `linuxArm64` ; les deux passent par le même helper `linuxTarget()`.
- Cinterop GLFW commonisé (`kotlin.mpp.enableCInteropCommonization=true`) : le `.def` ne contient que des
  headers, donc les bindings se résolvent depuis le source set partagé.
- `linkerOpts` est le **seul** paramètre dépendant de l'architecture : quels `.so` lier
  (`native/glfw/lib` pour arm64, `native/glfw/lib-x64` pour x86_64). C'est du linkage, pas du Compose.
- `scripts/fetch-deps.sh` extrait aussi les `.so` x86_64 ; `scripts/run-native.sh` prend un argument
  d'architecture (`arm64` par défaut, donc les appels existants sont inchangés).

**Résultat.** `compileKotlinLinuxX64` passe au vert du premier coup, 0 erreur. Les deux klibs sont
structurellement identiques : 101 packages compose chacun (48 `ui`, 39 `foundation`, 7 `material3`,
4 `animation`), 23 Mo d'IR chacun, avec `MaterialTheme`, `Button`, `Scaffold`, `LazyColumn`, `DatePicker`
et `CanvasLayersComposeScene` présents dans le klib x64. Aucun des 44 actuals Linux n'a eu besoin d'être
scindé entre arm64 et x64.

| | linuxArm64 | linuxX64 |
|---|---|---|
| Compilation | 0 erreur | 0 erreur |
| Binaire release | 35 Mo | **38 Mo** (ELF x86-64) |
| Rend du material3 | oui | oui |
| Clic -> `count` 0 à 1 | oui | oui |

Lancé avec `scripts/run-native.sh poc5-native release x64`, dans un conteneur `linux/amd64` sous Xvfb.
Captures : `docs/poc5-material3-knative-x64.png` (count: 0) et
`docs/poc5-material3-after-knative-x64.png` (count: 1).

**Réserves.** Le x86_64 tourne en **émulation qemu** sur un hôte ARM, et le rendu est **software
(llvmpipe)** : ça ne dit donc rien de la fluidité GPU sur du vrai matériel x86. Le clic est **synthétique**
(injecté), comme au Jalon 4. Même niveau de preuve que les Jalons 4/5, transposé au x64 : ni plus, ni moins.

**Rebuild depuis un `jb-main` frais.** `.cmc` a été recloné depuis `jb-main` HEAD le 2026-07-14 et la
baseline arm64 compile toujours avec 0 erreur, sans un seul patch. C'est une vérification à quelques jours,
pas sur la durée : le POC a été écrit quelques jours plus tôt. Ça montre donc que la recette se reproduit
depuis un clone propre, pas qu'elle résiste à la dérive upstream sur des mois. Cette question reste ouverte,
et c'est le poll Maven qui la surveille.

### Temps passé

- Jalon 6 (cette session) : ~1 h (restructuration des source sets + libs x86_64 + link + run).

## Route A1 (build monorepo complet) : recette + mur, et le poll Maven

**Poll Maven (2026-07-11, 17:45Z) :** `org.jetbrains.compose.ui:ui-linuxarm64`,
`foundation-linuxarm64`, `material3-linuxarm64`, `ui-backhandler-linuxarm64`,
`org.jetbrains.androidx.navigationevent:navigationevent-linuxarm64` → **tous 404 (pas encore
publiés)**. Les klibs de la **fondation** (skiko, runtime, runtime-saveable, lifecycle, savedstate,
collection, annotation) **sont** publiés pour K/N Linux : un rollout partiel, en cours.

**Poll Maven (2026-07-14), x64 inclus cette fois :** `ui`, `foundation` et `material3` sont toujours
**404 pour `linuxx64` comme pour `linuxarm64`**, en 1.12.0-beta02 (la dernière). `skiko` 0.150.1 et
`compose.runtime` 1.12.0-beta02 publient **les deux** architectures Linux. Le trou que comble ce POC est
donc inchangé après 14 mois, et il n'est pas propre à arm64 : personne ne publie la couche UI pour K/N
Linux.

**Pourquoi ça manque toujours, côté upstream.** La PR de Thomas Vos
[compose-multiplatform-core#2027](https://github.com/JetBrains/compose-multiplatform-core/pull/2027)
(« Compose UI for native Linux ») est **ouverte en draft depuis le 2025-04-16**, sans aucune revue
JetBrains. Le blocage n'est pas du code manquant, c'est une question de design restée sans réponse, qu'il
pose lui-même dans la PR : avec `expect/actual`, actualiser la couche de fenêtrage vers Wayland casse le
cas d'usage embarqué / sans window manager, et il n'y a pas de moyen propre de la rendre enfichable. Jake
Wharton fait le même constat dans le thread `#compose` du Slack Kotlin, et JetBrains (Ivan Matkov) a
confirmé vouloir passer à du polymorphisme, « mais ce n'est pas si simple ». Son travail sur skiko, lui, a
bien atterri : `skiko#1051` (cible linuxArm64) et `skiko#1052` (EGL) sont mergés et publiés, ce qui
explique que la fondation soit en place alors que la couche UI ne l'est pas.

**Recette route A1** (builder `compose-multiplatform-core` depuis HEAD, établie depuis les fichiers de
build du fork) :

1. **Checkout COMPLET** du monorepo : un clone sparse/`blob:none` ne suffit pas (le blob
   `gradle-wrapper.jar` manque, et `settings-fork.gradle` `include` ~centaines de modules).
2. Entrée fork : `./gradlew -c settings-fork.gradle` + `build-fork.gradle` + le script d'installation
   `buildSrc-fork/settingsScripts/out-setup.groovy`. Propriété `androidx.studio.type=jetbrains-fork`.
3. Env **`ANDROIDX_JDK21`** → un JDK 21 (`org.gradle.java.installations.fromEnv=ANDROIDX_JDK21`).
4. **Activer les cibles natives Linux** (`linuxX64`/`linuxArm64`) dans la convention multiplateforme
   partagée (jb-main n'active que ios/macos/desktop).
5. `kotlin.native.enableKlibsCrossCompilation=false` dans `gradle.properties` → **les klibs natifs se
   buildent sur Linux** (pas en cross depuis macOS), ou flipper le flag.
6. Puis builder `:compose:ui:ui`, `:compose:foundation:foundation`, `:compose:material3:material3`,
   `:navigationevent:*` pour `linuxArm64` (les builds androidx veulent ~16 Go+ RAM, du disque, des heures).

**Verdict sur A1 :** c'est un **chantier de build-infra de plusieurs jours**, pas une tâche de session :
la recette et les 6 prérequis sont établis ; le lancer (checkout multi-Go + mise en place du fork + build monorepo
sur macOS avec cross-compilation désactivée) n'a pas été mené à terme, car il calerait quasi
certainement et consommerait le budget sans artefact. **Et c'est probablement inutile :** JetBrains publie
activement la stack K/N Linux de bas en haut (fondation faite, UI en attente), donc le chemin pragmatique
est de **surveiller les artefacts UI et, quand ils sortent, écrire seulement le médiateur `ui-glfw`** (déjà
dé-risqué POC 3/4).

### Surveillance automatisée

Un poll Maven récurrent pour `ui-linuxarm64` / `material3-linuxarm64` / `foundation-linuxarm64` est mis
en place en routine planifiée, pour notifier dès la publication des klibs UI. À ce moment, le POC 5 se
réduit à câbler le médiateur `ui-glfw` sur la stack POC 4.

## Jalons restants (multi-sessions)

- **Jalon 2 : ATTEINT.** `ui-graphics`, `ui-text`, `ui-backhandler` puis `ui:ui` (avec `ComposeScene`)
  compilent en klib K/N linuxArm64 via A2, contre des deps publiées + 3 modules de sources
  (`runtime-retain`, `navigationevent`, `navigationevent-compose`) + 3 bouchons d'actuals Linux. Voir la
  section « PERCÉE » ci-dessus.
- **Jalon 3 : ATTEINT.** `foundation` + `material3` (+ `foundation-layout`, `animation(-core)`,
  `material-ripple`, `graphics-shapes`) compilent en klib linuxArm64 K/N. Voir la section « Jalon 3 »
  ci-dessus. Klib : `Button`/`Text`/`DatePicker`/`LazyColumn`/`Scaffold`… exportés.
- **Jalon 4 : ATTEINT.** Le médiateur `ui-glfw` branche le vrai `ComposeScene` sur une fenêtre GLFW et rend
  du material3 interactif (`Button`/`Text`, clic → `count` 0→1) sur K/N Linux sans JVM. Voir « Jalon 4 ».
- **Jalon 5 : ATTEINT.** Release **35 Mo** vs 137 Mo JVM ; RSS pic **124 Mo** vs ~224 Mo JVM. Voir « Jalon 5 ».

### Note de reproductibilité

`poc5-native/build.gradle.kts` pointe sur un clone de `compose-multiplatform-core` (branche `jb-main`,
chemin scratch, variable `cmcRoot`). Source sets tirés du monorepo : `compose/ui/{ui-util, ui-geometry,
ui-unit, ui-graphics, ui-text, ui-backhandler, ui}` (leurs `commonMain`/`skikoMain`/`nonJvmMain`/`nativeMain`/
`skikoExcludingWebMain` selon la hiérarchie), plus `compose/runtime/runtime-retain`, `navigationevent/
navigationevent`, `navigationevent/navigationevent-compose`. Actuals Linux ajoutés dans
`poc5-native/src/linuxArm64Main/` : `NativeFontPatched` (cale skiko FontStyle), `LinuxLocale`,
`LinuxStringDelegate`, `LinuxResolveInterceptor`, `TraceActual`, `PointerIcon.linux`,
`PlatformClipboard.linux`, `PlatformUriHandler.linux`, plus 5 fichiers copiés depuis `ui/src/macosMain`
(`Key`, `DragAndDrop`, `Focusability`, `PlatformVelocityTracker`, `InteropView`). Deps publiées clés :
`compose.runtime:1.12.0-beta01`, `lifecycle:2.11.0-rc01`, `savedstate:1.3.0-alpha06`,
`savedstate-compose:1.4.0`, `skiko:0.150.1`. Opt-ins globaux compose requis (voir le bloc `sourceSets.all`).
Commande : `gradle compileKotlinLinuxArm64` (produit le klib sous `build/classes/kotlin/linuxArm64/main/klib`).

### Temps passé

- POC 5 (cette session) : ~40 min (reconnaissance + compilation des modules de base).
