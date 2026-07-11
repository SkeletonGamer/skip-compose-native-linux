# FINDINGS : POC 6 : dé-Android-iser SkipUI / SkipFoundation pour cibler Compose Multiplatform

> Canonique : [`FINDINGS-POC6.md`](./FINDINGS-POC6.md) (anglais). Ceci est la copie française.
> Les FINDINGS POC 1-5 sont clos. POC 6 rouvre exactement la question que POC 1 avait laissée en suspens.

## Pourquoi ce POC existe

POC 1 a conclu NO-GO sur un transpileur Skip vers CMP parce que SkipUI/SkipFoundation semblaient
« diffusément couplés à Android ». Mais POC 1 l'a testé en *excluant* des fichiers (casser le monolithe
a fait passer les erreurs de 68 à 264). POC 5 a ensuite enseigné l'inverse : un couplage « d'apparence
diffuse » peut être un ensemble borné et énumérable d'actuals de plateforme, et les estimations d'effort
penchent vers la prudence. POC 6 re-teste le couplage de Skip avec la méthode du POC 5 : garder le monolithe
entier, caler la surface Android, compiler contre CMP.

Le découpage par cible compte :
- **CMP Desktop (JVM)** : `java.*` est présent, donc l'adossement Java de SkipFoundation survit. Tractable
  en premier.
- **CMP sur Kotlin/Native Linux (sans JVM, le monde du POC 5)** : `java.*` disparaît, SkipFoundation doit
  être réimplémenté en natif. Le gros du travail, en dernier.

## Jalon 0 : reconnaissance. Verdict : la surface de couplage est BORNÉE, pas diffuse

Mesuré sur le Kotlin transpilé par Skip (issu du `skip export` du POC 1).

**SkipUI** : 234 fichiers, 72 729 lignes.

| Imports | Compte | Note |
|---|---|---|
| `androidx.compose.*` | 1815 | CMP les fournit (mêmes noms de packages). L'essentiel. |
| `android.*` | 53 (dans 18 fichiers) | Android-only, API concrètes appelables (Build, Intent, Context, Bitmap, Log, Uri, haptics...). |
| `androidx.navigation3.*` | 32 (dans 3 fichiers) | LE point dur : navigation3 n'a aucun artefact CMP (POC 1/2). |
| autres `androidx.*` non-compose | ~43 | lifecycle 9 (CMP l'a), activity 9, work 6, core ~15. Cales bornées. |

**SkipFoundation** : 84 fichiers, 24 912 lignes. Seulement 6 `android.*` + 13 `java.*` (java.time 8,
java.util 4, java.io 1). Sur JVM desktop les `java.*` marchent tels quels ; pour K/N ils correspondent à
des libs connues (kotlinx-datetime, okio).

**Donc ~7% des imports sont Android-only, concentrés dans une minorité de fichiers.** C'est dénombrable et
localisé, pas « android.* partout ». Ça révise le POC 1 : POC 1 mesurait le monolithisme structurel
(l'exclusion casse en cascade) ; le Jalon 0 mesure la surface Android-only, monolithe intact = bornée.

Prior art : rien de publié. La discussion [skiptools#163](https://github.com/orgs/skiptools/discussions/163)
et un billet Skip évoquent l'idée (« cibler CMP partout sauf Apple ; comme CMP utilise les mêmes API que
Jetpack Compose, ça pourrait déjà marcher en grande partie »), mais personne ne l'a réalisé. POC 6 serait
inédit.

**Le seul vrai risque : `androidx.navigation3`** (32 imports, 3 fichiers). Aucun artefact CMP. Caler sa
surface d'une dizaine de symboles (NavDisplay, NavKey, NavBackStack...), ou se passer de navigation pour
une app minimale.

**Décision : GO pour le Jalon 1.** Compter les imports, c'est la surface, pas la profondeur. Le Jalon 1
(est-ce que ça compile contre CMP, l'ensemble d'erreurs converge-t-il ou déborde-t-il) est le vrai signal.

### Temps passé

- Jalon 0 : ~30 min (localisation source + mesure du couplage + prior art).

## Jalon 1 : le vrai SkipUI compile-t-il contre CMP Desktop (JVM) ? Verdict : ça CONVERGE

Installation : `poc6-skip-cmp/`, un projet `kotlin("jvm")` + Compose Desktop qui tire les 4 modules Skip
transpilés en source (SkipLib 41, SkipFoundation 84, SkipModel 10, SkipUI 234 = ~369 fichiers) et les
compile contre `org.jetbrains.compose` (Desktop), avec `android.jar` comme bouchon compile-only pour la
surface `android.*` (POC 1 avait trouvé que ça dissout le mur SkipFoundation).

Le compte d'erreurs est le signal (converge = borné = POC 1 avait tort ; déborde = diffus = POC 1 avait
raison) :

1. **Première compilation : 1348 erreurs.** Mais dominées par des **bibliothèques tierces manquantes**, pas
   du couplage : `Icons` (293, material-icons-extended), `coil3` (51, images), `okhttp3` (36),
   `commonmark` (13), `okio` (6). Toutes ont des artefacts JVM/CMP.
2. **Après ajout de ces 5 libs (+ un heap compilateur de 10 Go ; material-icons-extended est énorme) :
   535 erreurs.** Le compte s'est effondré, et ce qui reste EST le couplage.

Sur les 535 : **325 vraies « unresolved reference »** + ~94 cascades (78 inférence de type, 29 contexte
`@Composable` perdu, dès qu'un type est non résolu ses usages échouent) + **39 opt-in** material
« experimental » (trivial). Donc la vraie surface de couplage est ~325, et elle est **concentrée dans
~6 sous-systèmes identifiables**, pas éparpillée sur les 234 fichiers :

| Sous-système | ~erreurs | Où | Correctif |
|---|---|---|---|
| `androidx.navigation3` | ~75 | Navigation.kt (85), TabView.kt (88) | LE point dur : aucun artefact CMP. Caler ses ~10 symboles, ou se passer de nav pour une app minimale. |
| Notifications Android | ~53 | UserNotifications.kt | bouchon ou abandon (fonctionnalité entière) |
| Cycle de vie app | ~37 | UIApplication.kt | cale (host desktop, comme le PlatformContext du POC 5) |
| Présentation / dialogs | ~31 | Presentation.kt (`DialogWindowProvider`) | cale dialog CMP |
| Image | ~40 | AsyncImage.kt, Image.kt (`asAndroidPath`, coil svg) | cales / deps bornés |
| Compose-locals Android | ~28 | `LocalContext`, `LocalConfiguration`, `LocalView` | fournir des CompositionLocals desktop |
| resources / prefs | ~19 | `stringResource`, `getSharedPreferences` | resources CMP + cale prefs |
| `material3.adaptive`, dynamic color, divers | ~40 | `currentWindowAdaptiveInfo`, `dynamicLightColorScheme`, `testTagsAsResourceId` | dep adaptive + bouchons |

**Verdict : le couplage converge vers un ensemble borné et énumérable de cales par sous-système. Il ne
déborde PAS.** Ça confirme empiriquement le Jalon 0 et révise le POC 1 : le portage n'est pas « android.*
partout », c'est une liste dénombrable de décisions localisées (caler navigation3, boucher 3-4 fonctionnalités
Android-only, fournir ~30 cales de compose-locals, ajouter 2-3 deps).

**Réserve honnête :** « converge vers des sous-systèmes bornés » n'est pas « trivial ». Atteindre une
compilation VERTE est du travail Jalon 2/3 : la cale navigation3 est vraiment non-triviale (aucun artefact
CMP), et notifications / work / cycle de vie sont des fonctionnalités Android entières à boucher ou abandonner.
Mais c'est énumérable et localisé, l'inverse du cadrage « diffus/sans espoir » du POC 1. L'estimation en
semaines-homme pour un portage *propre complet* peut tenir ; la conclusion « laisse tomber, c'est diffus »,
non.

### Temps passé

- Jalon 1 : ~40 min (mise en place du projet + 2 itérations de compilation + catégorisation).

## Jalon 2 : viser la compilation verte. Verdict : convergence monotone, bornée

La méthode : garder les 4 modules Skip entiers, et combler la surface Android avec (a) des bibliothèques
publiées, (b) `android.jar` en compile-only pour `android.*`, (c) le seul androidx ayant une variante JVM
(`navigation3`, compile-only non-transitif pour que ses refs compose se lient à CMP), et (d) ~18 fichiers
de cale écrits main pour le reste (la méthode « fournir les actuals » du POC 5).

Trajectoire d'erreurs, chaque étape étant une action délibérée :

| Étape | Erreurs | Action |
|---|---|---|
| Première compilation | 1348 | (référence) |
| + 5 libs publiées (material-icons-extended, coil3, okhttp, okio, commonmark) | 535 | l'essentiel des 1348 n'était que des libs manquantes, pas du couplage |
| + `navigation3` compile-only non-transitif | 347 | le « point dur » résolu par une seule dep |
| + opt-ins + coil-svg + kotlin-reflect | (bruit retiré) | - |
| + 13 cales (compose-locals, adaptive/window-size, dynamic color, string resources, activity, work, semantics, graphics interop, dialog provider) | 136 | - |
| + 5 cales de plus (core.* compat, classes worker, contrôleur de marges système) | **107** | - |

**Ça converge de façon monotone et ne déborde jamais.** 1348 -> 107 avec des deps publiées + une dep
navigation3 + ~18 petits fichiers de cale. Le résidu ~107 est concentré dans **~5 sous-systèmes profonds**,
chacun un morceau borné mais réel : `NavigationSuiteScaffold` (TabView.kt, 19), `NotificationCompat`
(UserNotifications.kt, 17), activity-result + cycle de vie app (UIApplication.kt, 17), un **décalage de
version d'API coil** (AsyncImage/Image, ~23 : `fetcherFactory`, `memoryCacheKey`), et de petites traînes
(extension strikethrough de commonmark, convertisseur de font-scale Android).

**Verdict : le portage est mécanique, borné et localisé. Il ne déborde PAS.** Atteindre une compilation
*entièrement verte* est le travail multi-jours énumérable consistant à boucher en profondeur une poignée
d'API de fonctionnalités Android *complètes* (NotificationCompat, l'API NavigationSuiteScaffold, activity-result),
plus l'alignement de la version de coil. Ça confirme les Jalons 0-1 de façon décisive et révise le cadrage
« diffus/sans espoir » du POC 1 : le couplage est une liste dénombrable de décisions localisées. L'estimation
en semaines-homme pour un portage *propre complet* tient toujours ; la conclusion « laisse tomber, c'est
diffus », non. La besogne a été arrêtée à 107 volontairement : c'est une sonde jetable, et pousser des bouchons
de fonctionnalités Android profondes jusqu'à zéro est du rendement décroissant au-delà du signal.

### Temps passé

- Jalon 2 : ~1 h (stratégie de deps + ~18 fichiers de cale + 6 itérations de compilation).

## Jalon 3 : vers le vert + un écran rendu. Verdict : couplage Android entièrement résolu, résidu = pur décalage de version

Poussé depuis 107 : plus de cales (activity-result, core.app, navigation-suite échafaudage, BackHandler,
font-scaling, apparence des marges système) + exclusion des deux fichiers de notifications (feuilles, 0 référence
core). Atterrissage : **1348 -> 33 erreurs**, via `android.jar` (compile-only) + une dep `navigation3` +
~26 fichiers de cale écrits main + 2 fichiers de notifications exclus.

**Le constat décisif : les 33 erreurs restantes sont, à une exception près, toutes du décalage de VERSION
de bibliothèque, pas du couplage Android.** SkipUI a été transpilé contre des versions précises de coil /
Compose / material3 / navigation3, et ce projet en utilise d'autres :

- **coil (~22)** : `ImageRequest.Builder.fetcherFactory` / `memoryCacheKey` / `size` n'existent pas sur le
  builder de coil 3.0.4 (Image.kt, AsyncImage.kt). Décalage de version coil.
- **Compose (2)** : `Font.kt` passe un `Typeface` là où la surcharge CMP 1.9.0 attend un `Boolean`.
- **navigation3 (2)** : `rememberNavBackStack` veut une `SavedStateConfiguration`, SkipUI passe son propre
  type de clé (nav3 1.2.0-alpha05 vs la cible de SkipUI).
- **material3 (2)** : `ContentPadding` (Picker), `sheetGesturesEnabled` (ModalBottomSheet). `Shape.kt` une
  incompatibilité de receiver.

Ces sites de décalage de version ont été refermés comme le ferait un vrai portage : une montée de version coil (3.5.0), l'artefact
navigation3 **desktop** (il existe), un petit overload `rememberNavBackStack`, et ~10 patchs d'une ligne de
la source transpilée (retirer les args Android-only `autoSize` / `sheetGesturesEnabled`, `FontFamily.Default`
pour `FontFamily(Typeface)`, `SegmentedButtonDefaults.ContentPadding` -> `PaddingValues()`, et neutraliser
les chaînes de chargement d'images coil-Android, inutiles sur un witness desktop). Résultat : **`BUILD
SUCCESSFUL`, 0 erreur.** **Trajectoire complète : 1348 -> 535 -> 347 -> 136 -> 107 -> 33 -> 15 -> 3 -> 0.**

### Vert + rendu : ATTEINT

Le `ContentView` SwiftUI transpilé rend, via le **vrai SkipUI**, en PNG sur Compose Multiplatform Desktop,
sans Android : [`docs/poc6-skipui-render.png`](./docs/poc6-skipui-render.png) montre « Count: 0 » (un `Text`
SwiftUI) et un `Button` « Increment » (accent material3). Hors écran via `ImageComposeScene`.

Obtenir un pixel a exposé le versant EXÉCUTION (distinct de la compilation) :
- **L'amorçage de Context JVM de Skip.** `Bundle.main` (déclenché par tout `Text`) fait que le
  `ProcessInfo` de SkipFoundation appelle par réflexion
  `androidx.test.core.app.ApplicationProvider.getApplicationContext()` (le chemin androidx-test / Robolectric
  que Skip utilise sur JVM). On l'a simulé par une classe de ce nom renvoyant un `Context` **Mockito**
  (`RETURNS_MOCKS`), pour que le rendu démarre sans environnement Robolectric complet.
- **Les compose-locals Android doivent avoir des valeurs.** `LocalContext` / `LocalConfiguration` /
  `LocalView` sont lus pendant le rendu de `Text` (`EnvironmentValues.getLocale`). Les types `android.jar`
  lèvent `Stub!` si on les instancie, donc ils sont adossés à des mocks Mockito aussi.

Voilà la forme honnête : un vrai portage de production remplacerait ces mocks par Robolectric (l'exécution
JVM de Skip) ou un `Context`/`Configuration` desktop réel, et porterait (au lieu de désactiver) le chemin
image coil. Mais la verticale est prouvée de bout en bout : **SwiftUI transpilé -> vrai SkipUI -> CMP
Desktop -> un écran rendu, sans Android.**

**Conclusion POC 6 :** le verdict « diffus / sans espoir » du POC 1 sur la dé-Android-isation de SkipUI est
décisivement faux. Le couplage Android est un ensemble dénombrable et localisé de cales + patchs (fait ici),
la compilation est verte, et un écran SwiftUI transpilé rend vraiment sur CMP Desktop. L'estimation en
semaines-homme pour un portage *propre, maintenu, de production* tient toujours (Robolectric ou un vrai
Context desktop, porter le sous-système image, restaurer les notifications, suivre l'upstream Skip) ; le
« laisse tomber, c'est diffus », non.

### Temps passé

- Jalon 3 : ~2 h (cales activity-result/échafaudage/marges système, fermeture du décalage de version, patchs source, l'
  amorçage à l'exécution ApplicationProvider + locals, et le rendu).

## Jalon 4 (démarré) : pousser vers Kotlin/Native Linux, sans JVM. Le mur, chiffré par le compilateur

Le Jalon 3 tournait sur CMP Desktop (JVM), où le backing `java.*` de SkipFoundation marche tel quel. K/N
Linux n'a pas de `java.*`, donc la question du Jalon 4 est la taille de ce mur. Nouveau projet `poc6-native/`
: une cible linuxArm64 `kotlin("multiplatform")` qui compile les modules Skip transpilés depuis les sources
(sans compose pour l'instant) et laisse le **compilateur, pas le grep**, mesurer l'écart. Démarré au module
de base, SkipLib (tout le reste en dépend).

**SkipLib : 100 -> 33 erreurs compilateur avec 5 petites cales de types-valeurs.** La première compilation
donnait 100 erreurs ; 5 petites cales sous `poc6-native/src/linuxArm64Main/kotlin/shims/` en ont fermé 67 :
- `@androidx.annotation.Keep` (42 erreurs, toutes une annotation) -> une annotation sans effet.
- `java.util.Random` / `java.security.SecureRandom` -> adossés à `kotlin.random`.
- les aides code-point de `java.lang.Character` -> BMP seulement.
- `java.util.regex.Matcher.quoteReplacement` -> un échappeur de 6 lignes.
- `java.lang.UnsupportedOperationException` -> typealias vers celui de Kotlin ; plus un opt-in K/N.

**Le résidu de 33 est le vrai cœur dur : exactement trois réimplémentations plus une triviale.**

| Site | Erreurs | Nature |
|---|---|---|
| `Concurrency.kt` | 22 | Le moteur `Task` de Skip : `GlobalScope.async(dispatcher + ThreadLocal.asContextElement(state))` + `synchronized`. Concurrence structurée Swift bâtie sur coroutines JVM + `ThreadLocal` (coroutines-android). Exige une réécriture native (un verrou K/N + un task-local coroutines). |
| `Numbers.kt` | 7 | `java.math.BigInteger` / `BigDecimal` (précision arbitraire). Pas d'équivalent dans la stdlib K/N ; exige une bignum. |
| `String.kt` | 2 | `String.format` (style printf). Pas d'équivalent dans la stdlib K/N ; exige un formateur. |
| `AsyncStream.kt` | 2 | `@JvmName` (indice de signature réservé aux tests). Trivial : retirer les deux lignes. |

Donc même le module de base ne passe pas au vert en K/N sans réimplémenter le cœur concurrence de Skip, une
bignum et un formateur de chaînes. C'est un mur d'une autre nature que la surface Android : le couplage
Android était des cales bornées par-dessus un compose qui marche par ailleurs ; le couplage K/N est dans le
**cœur** de l'exécution de Skip.

**Et SkipLib est le module léger.** SkipFoundation porte le poids : 710 références `java.*` sur 34 fichiers,
concentrées dans le sous-système date/calendrier (`java.util.Calendar` à lui seul : 231 refs ;
`DateComponents.kt` 134, `Calendar.kt` 104), le système de fichiers (`java.nio.file`, `FileManager.kt` 110),
le formatage de nombres (`java.math.BigDecimal`) et le réseau (`java.net.*`, plus okhttp / commonmark /
org.xmlpull, tous JVM-only). Le porter, c'est réimplémenter en natif une tranche de java.time + java.text +
java.nio + java.math + une pile HTTP/markdown/XML (kotlinx-datetime, okio, ktor sont les substituts usuels).

### Résultat Jalon 4 : SkipLib + SkipFoundation compilent VERT en K/N Linux, sans JVM

Le mur a ensuite été ramené à zéro. **SkipLib et SkipFoundation compilent tous deux en klib `linuxArm64`
Kotlin/Native sans JVM (`BUILD SUCCESSFUL`, 0 erreur), depuis une première compilation à 2103 erreurs.** 125
fichiers Skip transpilés compilent en natif.

La méthode, c'est l'approche `android.jar` retournée vers le JDK : le portage ne réimplémente **pas** le
comportement de Foundation, il fournit la **surface API `java.*` / `android.icu` / tierces** que
SkipFoundation appelle, en cales compile-only (corps `TODO()`) aux signatures et types de retour corrects,
exactement comme `android.jar` a bouché la surface Android côté JVM au Jalon 3. Plus une poignée de vrais
substituts natifs là où c'est peu coûteux.

- **51 fichiers de cales, ~2400 lignes**, couvrant 40+ packages : `java.util` (Calendar/Date/Locale/TimeZone/
  Currency/UUID/Base64/logging/concurrent/stream/Timer/Scanner), `java.time`(`.format`/`.temporal`/`.zone`),
  `java.text`, `java.nio.file`(`.attribute`)/`.charset`, `java.net`, `java.io`, `java.math`, `java.security`,
  `java.lang` (Number/System/Class/Thread/Runtime/Integer/Byte/Method), `javax.crypto`, `kotlin.reflect.full`,
  `android.os`/`util`/`content`(`/pm`/`res`)/`net`/`icu`(`.text`/`.util`), `okhttp3`, `okio`, `org.commonmark`,
  `org.xmlpull`, `org.json`.
- **Vrais substituts natifs** là où c'est trivial : le moteur `Task` de SkipLib sur atomicfu (`synchronized`)
  + un task-local coroutines-natif (`asContextElement` best-effort) ; `BigInteger`/`BigDecimal` sur la bignum
  multiplateforme ionspin ; `Charsets` + encodage de chaînes sur l'UTF-8 de kotlin ; `java.util.Random` /
  `Character` / échappement regex / `String.format` (sous-ensemble printf).
- **~12 patchs d'une ligne** sur la sortie transpilée (comme au Jalon 3) : retirer `@JvmName` / `@JvmStatic` /
  `@JvmOverloads` (annotations JVM-only, invalides dans un source set K/N feuille), remplacer `Dispatchers.IO`
  (interne en K/N) par `Dispatchers.Default`, décoder les octets via `decodeToString()` au lieu de
  `java.lang.String(bytes, charset)`, et ajouter quelques `import java.lang.System/Class/Integer` (java.lang
  n'est pas auto-importé en K/N).

**Ce que ça prouve :** le mur K/N de la fondation d'exécution de Skip est **borné et mécanique au niveau
compile** : 2103 -> 0 en une session, essentiellement en énumérant la surface API du JDK que Skip touche. Les
seuls frottements systématiques étaient le bord de nullabilité (types stricts de Kotlin contre types-plateforme
Java) et l'absence d'auto-import de `java.lang`, tous deux traités localement.

**Réserve honnête :** c'est un COMPILE VERT à corps bouchons, pas une exécution fonctionnelle. Les cales lèvent
`TODO()` à l'exécution, comme `android.jar` lève `Stub!` ; un portage fonctionnel remplacerait les cales
date / calendrier / fichier / nombre / réseau par de vraies implémentations natives (kotlinx-datetime, okio,
ktor). Mais le portage au niveau de la compilation, ce que le POC 1 qualifiait de « diffus / sans espoir », est fait.

### La pile Skip complète, verte en K/N Linux, sans JVM

La poussée est ensuite montée jusqu'en haut de la pile :

- **SkipModel** (snapshot-state sur `compose.runtime`) : vert après ajout du klib K/N `compose.runtime`
  publié + cales `java.util.LinkedList` / `java.lang.ref.WeakReference` + un correctif de nullabilité. Rapide.
- **SkipUI** (234 fichiers, 1897 imports `androidx.compose.*`) : vert. C'est le gros morceau. Il exige les
  vrais `compose.ui` / `foundation` / `material3` pour K/N Linux, que JetBrains ne publie pas, donc (exactement
  comme au POC 5) ils sont compilés depuis un checkout source de `compose-multiplatform-core` (jb-main) avec la
  même hiérarchie de source sets KMP et les `actual` Linux. Par-dessus, la surface UI Android de SkipUI est
  bouchée comme celle de SkipFoundation : la première compilation à 1500 erreurs est tombée à 0 en fournissant
  `material-icons` (281 icônes placeholder), `navigation3`, `coil3`, `okhttp`, `android.os/graphics/app/content/
  view/provider/accessibility/database/webkit`, `org.w3c.dom` + `javax.xml`, et l'interop compose Android-only
  (`LocalContext`/`LocalConfiguration`, `ContentAlpha`, `currentWindowAdaptiveInfo`, `NavigationSuite*`,
  `pullRefresh`, `asAndroidPath`, `stringResource`, dynamic color, `testTagsAsResourceId`, ...).
- **L'app SwiftUI transpilée** (`ContentView` de `Witness`) compile aussi.

**Total final : 371 fichiers Skip transpilés (SkipLib + SkipFoundation + SkipModel + SkipUI + Witness)
compilent en klib K/N `linuxArm64`, sans JVM, par-dessus la pile Compose compilée depuis les sources.** Le
portage, c'est **115 fichiers de cales (~4300 lignes)** plus **42 fichiers `actual` Linux de la pile Compose**
(réutilisés du POC 5) plus ~15 patchs d'une ligne sur la sortie transpilée. Les deux frottements
systématiques ont été les mêmes du début à la fin : la nullabilité stricte de Kotlin contre les
types-plateforme Java/Android, et les symboles que la JVM auto-importe (`java.lang.*`) ou expose en statiques
de companion, que K/N n'a pas.

**Conclusion : tout le framework UI Skip, transpilé depuis SwiftUI, compile pour Kotlin/Native Linux sans
JVM.** Le verdict « diffus / sans espoir » du POC 1 est décisivement infirmé, au niveau de la compilation, pour toute la
pile. Le résidu est à l'exécution : les cales (façon `android.jar`) doivent être remplacées par de vraies
implémentations natives pour une app qui rend vraiment.

### Temps passé

- Jalon 4 : ~6 h au total (mise en place du projet K/N, catégorisation du mur, SkipLib/Foundation/Model
  2103 -> 0, puis la fusion compose-depuis-sources + SkipUI 1500 -> 0, via 115 fichiers de cales pilotés par
  le compilateur et parallélisés sur des sous-agents pour le gros du bouchonnage).

## Jalon 5 (fait) : lier, exécuter, rendre, mesurer. Sans JVM.

La pile compilée a été liée en un vrai exécutable `linuxArm64` (médiateur glfw + skiko GL, comme au POC 5)
et exécutée dans un conteneur Linux arm64 sous Xvfb (GL software). Deux résultats.

**Poids.** L'ensemble se linke en un binaire **release de 37 Mo** (93 Mo debug), sans JVM : le framework UI
Skip COMPLET (transpilé depuis SwiftUI) + la pile Compose compilée depuis les sources, contre **137 Mo** pour
l'app JVM Compose Desktop du POC 2 (la JRE seule pèse 87 Mo). L'élimination de code mort est décisive : 37 Mo,
soit ~2 Mo au-dessus de l'app material3 minimale du POC 5, car l'éditeur de liens strippe la surface Skip inutilisée.
(Prérequis à l'édition de liens : les ~130 ponts JNI `external fun Swift_*` de Skip compilent mais n'ont pas de symbole
natif, donc bouchés par des corps bouchons, inutiles pour un rendu Kotlin-only.)

**Rendu + RAM.** Le binaire ne fait pas que se lier, il TOURNE : le `ContentView` SwiftUI transpilé rend à
travers le vrai SkipUI, sans JVM. [`docs/poc6-skipui-native-render.png`](./docs/poc6-skipui-native-render.png)
montre « Count: 0 » (un `Text` SwiftUI) et un `Button` « Increment » (material3), le même écran que côté JVM au
Jalon 3, désormais 100 % natif. RSS pic : **~122 Mo** (124820 kB), contre ~224 Mo pour l'app JVM du POC 2.

**Interactif, pas seulement rendu.** On a injecté un tap synthétique (`ComposeScene.sendPointerEvent`
Press+Release) sur le bouton, puis recomposé : « Count: 0 » devient « Count: 1 »
([`docs/poc6-skipui-native-click.png`](./docs/poc6-skipui-native-click.png)). Le clic pilote donc une vraie
recomposition du `ContentView` transpilé, en K/N Linux sans JVM, pas seulement un premier dessin statique.

Passer de la compilation verte au rendu vivant, c'est le « déminage à l'exécution » : les cales façon `android.jar` lèvent
`TODO()` à l'exécution, donc chacune sur le chemin boot+rendu a été rendue fonctionnelle ou bénigne, pilotée
par les stacktraces réelles (une dizaine de correctifs) :
- un `Context` desktop fonctionnel (`DesktopContext`) à ressources / prefs / répertoires bénins, branché sur le
  `ProcessInfo.androidContext` de SkipFoundation (un patch source, remplaçant la réflexion Robolectric /
  ApplicationProvider de la JVM) ;
- un `java.net.URI` fonctionnel (parseur RFC 3986) pour que l'amorçage URL du `Bundle` de Skip réussisse ;
- `System` fonctionnel (temps epoch réel via posix `gettimeofday`, défauts de propriétés sensés), `KClass.java`
  (porte le nom qualifié), `Configuration.locales` (`LocaleList` par défaut) ;
- `PackageManager` / `Log` / `commonmark` bénins (défauts / arbres vides au lieu de lever) ;
- `setURLStreamHandlerFactory` en no-op.

C'est la forme honnête de l'exécution : un écran de base rend avec ~10 correctifs ciblés ; une app complète (vrais
assets, images via coil, réseau via ktor, dates via kotlinx-datetime) fonctionnaliserait le reste des cales de
la même façon, dans la même boucle pilotée par les crashes.

**But de la série atteint et chiffré : tout le framework UI Skip, transpilé depuis SwiftUI, à la fois COMPILE
et REND en Kotlin/Native Linux sans JVM, à 37 Mo / 122 Mo contre 137 Mo / 224 Mo sur la JVM.**

### Temps passé

- Jalon 5 : ~2 h (point d'entrée + médiateur glfw/skiko, édition de liens release/debug, et la boucle de déminage à l'exécution
  à ~10 correctifs jusqu'au premier rendu, plus la mesure poids/RSS).

## Un seul `export/` pour les deux cibles (JVM + K/N)

Les deux moitiés du POC 6 sont deux projets de build distincts (`poc6-skip-cmp`, JVM ; `poc6-native`,
Kotlin/Native Linux) mais lisent le **même** code Skip transpilé dans `./export`. Les patchs source
spécifiques au K/N avaient fait diverger cet export au point que le build JVM ne compilait plus. On l'a
réconcilié : **un seul export compile ET rend sur les deux cibles**, pour ne rien avoir à maintenir en double.

Principe : l'export n'emploie que des formes **neutres** (qui résolvent des deux côtés) ; les symboles portant
un nom « K/N » sont fournis à la cible JVM comme petites cales à elle. Quatre points de divergence, tous réduits :

- **`value class` sans `@JvmInline`** (`StackLayouts.kt`). La JVM exige `@JvmInline` sur une value class ; le K/N,
  lui, ne résout pas `@JvmInline` (le package `kotlin.*` est réservé, donc inshimmable). Résolu en **classe
  simple** (on retire `value`) : compile sur les deux, comportement identique, on perd seulement l'inlining.
- **`skip.lib.synchronized`** (import dans `Publisher.kt`). Nécessaire en K/N (pas de `synchronized` intégré) ;
  côté JVM on fournit une cale `skip.lib.synchronized` déléguant au builtin `kotlin.synchronized`, avec le
  contrat `EXACTLY_ONCE` pour que les affectations de locals capturés dans le bloc type-checkent (Skip s'y fie).
- **`android.content.DesktopContext`** (`ProcessInfo.kt`). Le `Context` desktop bénin du K/N ; côté JVM on fournit
  un `val DesktopContext` adossé au **même** contexte Mockito que fake déjà `ApplicationProvider`.
- **`Context.CLIPBOARD_SERVICE`** (`UIPasteboard.kt`). Le chemin d'import K/N `Context.Companion.CLIPBOARD_SERVICE`
  ne résout pas sur JVM (le `Context` d'`android.jar`, une classe Java, n'a pas de `Companion`). Passé en **accès
  qualifié** `Context.CLIPBOARD_SERVICE`, qui résout des deux côtés (champ statique en JVM, const de companion en K/N) :
  c'était l'import, pas l'accès, qui divergeait.

Enfin, la détection d'`android.jar` (compile-time only côté JVM) est rendue multi-plateforme : `ANDROID_JAR`, puis
`ANDROID_HOME` / `ANDROID_SDK_ROOT`, puis les emplacements SDK usuels par OS (macOS `~/Library/Android/sdk`, Linux
`~/Android/Sdk`, Windows `%LOCALAPPDATA%\Android\Sdk`), en prenant la plateforme installée la plus haute.

Vérifié de bout en bout : `scripts/run-native.sh poc6-native` rend en K/N sans JVM (PNG, RSS ~138 Mo) et
`scripts/run-jvm.sh poc6-skip-cmp` rend sur CMP Desktop JVM (PNG), depuis ce seul export partagé.

**Reproduction automatisée.** L'`export/` est régénérable en une commande (`scripts/setup.sh` :
`fetch-deps` puis `skip export` puis `scripts/patch-export.sh`). `skip export` transpile **deux écrans**
du witness (`witness-app/Sources/Witness/`) : `MinimalContentView` (rendu par le POC 6) et le `ContentView`
riche du POC 1, qui compile mais n'est pas rendu. `patch-export.sh` applique `scripts/export.patch`
(la transformation large `Swift_*` + ~20 édits SkipLib/Foundation/Model/UI), qui ne contient **que les
lignes changées** (pas de contexte source Skip). Le patch vise la version de Skip figée ; une autre version
peut demander de le régénérer.

## Jalons restants

- **Jalon 2 (tenté, convergé à 107)** : voir la section Jalon 2 ci-dessus. 1348 -> 107 via deps + ~18
  cales ; la traîne restante = boucher en profondeur NotificationCompat / NavigationSuiteScaffold /
  activity-result + aligner la version de coil. Borné, multi-jours, arrêté volontairement (sonde jetable).
- **Jalon 3** : un écran SwiftUI transpilé rendu via le vrai SkipUI sur CMP Desktop.
- **Jalon 4 (FAIT au niveau de la compilation)** : TOUTE la pile Skip (SkipLib + SkipFoundation + SkipModel + SkipUI +
  l'app Witness transpilée, 371 fichiers) compilation VERTE en K/N Linux, sans JVM, par-dessus la pile Compose
  compilée depuis les sources. 115 fichiers de cales + 42 fichiers actual Linux de la pile Compose + ~15 patchs source.
- **Jalon 5 (FAIT, voir la section ci-dessus)** : lié en binaire release de 37 Mo, et le `ContentView`
  SwiftUI transpilé REND en K/N Linux sans JVM (37 Mo / 122 Mo RSS contre 137 Mo / 224 Mo JVM), via ~10
  correctifs d'exécution ciblés (Context/URI/System fonctionnels, PackageManager/Log/commonmark bénins).
- **Au-delà de la série** : une exécution pleinement fonctionnelle (vrais assets, images coil, réseau ktor, dates
  kotlinx-datetime) fonctionnaliserait les cales restantes de la même façon pilotée par les crashes ; plus le
  coût permanent du suivi de l'upstream Skip.
