# skip-compose-native-linux

Faire tourner de l'UI déclarative, **Compose Multiplatform** et **SwiftUI transpilé par
[Skip](https://skip.dev)**, en **Kotlin/Native Linux, sans JVM**.

> Version anglaise canonique : [`README.md`](./README.md). Ce fichier en est la copie française.

Une série de six sondes de recherche sur la couche UI d'un appareil Linux contraint. Elles répondent de
bout en bout à une question : *Jetpack Compose / Compose Multiplatform peut-il rendre sur un appareil
Linux sans payer le coût de la JVM, et à quel prix ?* La réponse, mesurée sur Linux arm64 : **oui**, un
vrai `MaterialTheme` + `Button` + `Text` rend et réagit en **Kotlin/Native Linux sans JVM**, dans un
binaire autonome de **35 Mo** utilisant **124 Mo** de RSS, contre **137 Mo / ~224 Mo** pour l'app
Compose Desktop (JVM) équivalente.

Le POC 6 rouvre la seule question que le POC 1 avait close en NO-GO : l'UI Skip (SwiftUI transpilée)
peut-elle être dé-Android-isée vers Compose Multiplatform ? Il est **fait** sur les deux cibles : d'abord
CMP Desktop (JVM), puis **Kotlin/Native Linux sans JVM**, où toute la pile Skip transpilée (371 fichiers)
compile vert et l'écran SwiftUI transpilé rend en natif, à **37 Mo / 122 Mo de RSS** contre **137 Mo /
224 Mo** sur la JVM.

| Avant (count 0) | Après un clic (count 1) |
|---|---|
| ![material3 en K/N Linux, count 0](./docs/poc5-material3-knative-arm64.png) | ![après clic, count 1](./docs/poc5-material3-after-knative-arm64.png) |

*Le vrai `Button` material3, mis en page et testé au clic par le vrai `ComposeScene` de JetBrains, rendu par
skiko GL dans une fenêtre GLFW, en Kotlin/Native Linux arm64, sans JVM. Rendu software (Xvfb / llvmpipe).*

## Les six POC

Chaque POC a son document de findings (anglais + français), le vrai livrable.

| # | POC | Verdict | Findings |
|---|---|---|---|
| 1 | Skip (SwiftUI transpilé) vers Compose Multiplatform desktop Linux | NO-GO sur un transpileur ; partir Compose-first | [`FINDINGS.fr.md`](./FINDINGS.fr.md) |
| 2 | Compose-first natif, et la réalité mobile ARM | GO Compose-first ; mais CMP Desktop est JVM-only (137/224 Mo) | [`FINDINGS-POC2.fr.md`](./FINDINGS-POC2.fr.md) |
| 3 | La fondation K/N Linux (skiko + runtime + fenêtrage GLFW) | GO : la fondation existe, sans JVM (rectangle Skia 21,5 Mo) | [`FINDINGS-POC3.fr.md`](./FINDINGS-POC3.fr.md) |
| 4 | Un `ui-glfw` minimal écrit à la main (UI interactive, sans JVM) | Approche prouvée viable (24 Mo) ; pas le vrai compose.ui | [`FINDINGS-POC4.fr.md`](./FINDINGS-POC4.fr.md) |
| 5 | Le **vrai** compose.ui / foundation / material3 en K/N Linux | TERMINÉ : rendu + interactif, 35 Mo / 124 Mo, sans JVM | [`FINDINGS-POC5.fr.md`](./FINDINGS-POC5.fr.md) |
| 6 | Dé-Android-iser le SkipUI transpilé, sur CMP Desktop (JVM) puis Kotlin/Native Linux | FAIT : toute la pile Skip compile vert ET l'écran SwiftUI transpilé rend en K/N Linux, sans JVM (37 Mo / 122 Mo contre 137/224 JVM) | [`FINDINGS-POC6.fr.md`](./FINDINGS-POC6.fr.md) |

> Chaque probe a un runner en une commande, et `scripts/setup.sh` prépare un clone frais. Voir la section
> [Reproduire](#reproduire) plus bas, ou [`scripts/README.md`](./scripts/README.md) pour la matrice complète.

## Comment ça marche

Le repo suit deux fils, aboutissant tous deux en Kotlin/Native Linux. Compose sur K/N Linux : le POC 2 a
constaté que Compose Multiplatform Desktop est JVM-only, donc le POC 3 a prouvé que la fondation existe
(skiko + runtime + fenêtrage GLFW), le POC 4 un `ui-glfw` minimal écrit à la main, et le POC 5 le vrai
`compose.ui` / `foundation` / `material3`. Le transpileur Skip : le POC 1 l'avait clos en NO-GO, le POC 6 le
rouvre et dé-Android-ise le SkipUI transpilé. Voici comment fonctionnent les deux aboutissements ; les
tremplins (POC 1 à 4) sont dans le tableau ci-dessus, chacun avec ses findings.

### L'UI Compose en Kotlin/Native Linux (POC 5)

Deux briques, sans forker Compose :

1. **Extract-and-compile (route A2).** Les vrais `compose.ui`, `foundation`, `material3` (et leurs
   dépendances) sont compilés pour la cible Kotlin/Native `linuxArm64` **depuis un checkout source de
   [JetBrains/compose-multiplatform-core](https://github.com/JetBrains/compose-multiplatform-core)**,
   contre les klibs **publiés** des briques que JetBrains fournit déjà pour K/N Linux (skiko, compose
   runtime, lifecycle, savedstate, collection, annotation). La surface de plateforme qui réclame des
   `actual` Linux est petite et énumérable (voir les findings du POC 5) ; deux ou trois sont remplacées par
   des bouchons (presse-papiers, couche i18n du DatePicker).
2. **Un médiateur `ui-glfw` (~180 lignes).** Il branche le vrai `ComposeScene` (`CanvasLayersComposeScene`
   + `FrameRecomposer` + un `PlatformContext`) sur une fenêtre GLFW via une surface skiko GL, lui passe
   les événements pointeur et exécute une frame Compose par itération
   (`performFrame` -> `measureAndLayout` -> `draw`).

Le seul mur de plateforme rencontré à l'exécution : `compose.ui#postDelayed` (utilisé par le debounce de
`RectManager`), qui s'exécute sur `Dispatchers.Main`, absent en K/N Linux. Il est remplacé par un
ordonnanceur drainé par la boucle de frames, qui exécute les callbacks sur le thread compose.

### La question du transpileur Skip, rouverte (POC 6)

Le POC 6 rouvre la seule question que le POC 1 avait close en NO-GO : le SkipUI/SkipFoundation transpilé de
Skip est-il « diffusément couplé à Android », ou le couplage est-il borné et remplaçable par des cales ? Il
y répond sur deux cibles : d'abord **CMP Desktop (JVM)** (`poc6-skip-cmp/`, ci-dessous), puis **Kotlin/Native
Linux sans JVM** (`poc6-native/`), où toute la pile Skip compile et rend.

La méthode est celle du POC 5 (« fournir les actual de plateforme »), appliquée à Skip : garder les quatre
modules Skip transpilés entiers (SkipLib / SkipFoundation / SkipModel / SkipUI, ~369 fichiers), et combler
la surface Android avec (a) des bibliothèques publiées (coil3, okhttp, commonmark, material-icons-extended),
(b) `android.jar` en compile-only pour la surface `android.*`, (c) le seul androidx qui a une variante JVM
(`navigation3`, compile-only non-transitif pour que ses références compose se lient à CMP), et (d) ~24
fichiers de cales écrits à la main sous
[`poc6-skip-cmp/src/main/kotlin/shims/`](./poc6-skip-cmp/src/main/kotlin/shims/).

Le nombre d'erreurs converge de façon monotone vers zéro (1348 -> 535 -> 347 -> 136 -> 107 -> 33 -> 15 ->
3 -> 0, `BUILD SUCCESSFUL`) ; le résidu était du décalage de versions de bibliothèques, pas du couplage
Android. Le `ContentView` SwiftUI transpilé rend ensuite à travers le vrai SkipUI dans un PNG, sans Android :

| Rendu POC 6 (CMP Desktop, sans Android) |
|---|
| ![SwiftUI transpilé via le vrai SkipUI sur CMP Desktop](./docs/poc6-skipui-render.png) |

*Un `Text` SwiftUI (« Count: 0 ») et un `Button` material3 (« Increment »), transpilés par Skip et rendus
par le vrai SkipUI sur Compose Multiplatform Desktop, hors écran via `ImageComposeScene`. Le verdict : le
cadrage « diffus / sans espoir » du POC 1 ne tient pas ; le couplage est un ensemble dénombrable et
localisé de cales.*

#### POC 6 en Kotlin/Native Linux, sans JVM (`poc6-native/`)

La même pile Skip transpilée a ensuite été poussée jusqu'en K/N Linux, par-dessus la pile Compose compilée
depuis les sources du POC 5. L'ensemble (SkipLib + SkipFoundation + SkipModel + SkipUI + l'app Witness
transpilée, 371 fichiers) compile vert, se lie en un binaire release de **37 Mo**, et le `ContentView`
SwiftUI transpilé **rend en natif**, sans JVM :

| Rendu POC 6 (Kotlin/Native Linux, sans JVM) |
|---|
| ![SwiftUI transpilé via le vrai SkipUI en K/N Linux](./docs/poc6-skipui-native-render.png) |

*Le même écran « Count: 0 » / « Increment », désormais 100 % natif : SwiftUI transpilé -> vrai SkipUI ->
vrai ComposeScene -> skiko GL, dans une fenêtre GLFW en Kotlin/Native Linux arm64, sans JVM. **37 Mo /
122 Mo de RSS**, contre **137 Mo / 224 Mo** pour l'app Compose Desktop (JVM) équivalente.*

Comme K/N n'a pas de `java.*`, le portage fournit la surface API `java.*` / `android.*` / tierce que Skip
appelle, en cales à la compilation seule (l'approche `android.jar` retournée vers le JDK) : **115 fichiers
de cales (~4300 lignes)** sur 40+ packages, plus une tranche fonctionnelle pour l'exécution (un `Context`
desktop, un `java.net.URI` RFC 3986, le temps réel via posix, `BigInteger` sur la bignum ionspin, ...). Les
findings détaillent la trajectoire complète (2103 -> 0 pour la fondation, 1500 -> 0 pour l'UI) et les ~10
correctifs d'exécution jusqu'au premier rendu.

## Reproduire

`./export` (la sortie transpilée de Skip) et `./.cmc` (le checkout des sources Compose) ne sont pas commités ;
`scripts/setup.sh` les régénère pour un clone frais : il récupère les dépendances, lance `skip export` sur
l'app witness, puis la dé-Android-ise avec `scripts/patch-export.sh`. Ensuite, lancer n'importe quelle probe :

| Probe | Commande |
|-------|----------|
| POC 1 : sortie Skip sur CMP Desktop (JVM) | `scripts/run-jvm.sh desktop-witness` |
| POC 2 : Compose-first, sur écran réel | `scripts/run-poc2-screen.sh` |
| POC 3 : Skia + runtime + GLFW, natif | `scripts/run-native.sh poc3-native` |
| POC 4 : ui-glfw minimal, natif | `scripts/run-native.sh poc4-native` |
| POC 5 : vrai material3, natif | `scripts/run-native.sh poc5-native` |
| POC 6 : SkipUI transpilé, natif (sans JVM) | `scripts/run-native.sh poc6-native` |
| POC 6 : SkipUI transpilé sur CMP Desktop (JVM) | `scripts/run-jvm.sh poc6-skip-cmp` |

Les prérequis (un JDK, Docker, la chaîne d'outils Skip), les overrides et la matrice complète sont dans
[`scripts/README.md`](./scripts/README.md).

Deux points à signaler : les klibs K/N Linux publiés que consomment les builds natifs sont figés sur des
versions de tête (alpha/beta/rc), car c'est le seul endroit où les variantes `linuxArm64` existent pendant
le rollout (les findings notent les versions exactes) ; et la sortie Kotlin transpilée de Skip n'est pas à
nous de redistribuer, d'où le fait que `setup.sh` la régénère localement plutôt que le dépôt la livre.

## Statut et réserves

- **Le rendu est software** (llvmpipe sous Xvfb). La fluidité GPU et le profil mémoire sur matériel réel
  ne sont pas encore confirmés.
- Le coût de cette approche est de **maintenir un backend `ui-glfw` hors-JetBrains** plus une poignée
  d'`actual` Linux, tant que JetBrains n'a pas publié les artefacts K/N Linux de la couche UI. Une fois
  publiés, l'échafaudage extract-and-compile se réduit au seul médiateur.
- Ce sont des **sondes de recherche exploratoires**, pas du code durci pour la production. Lire les findings pour le vrai bilan de coût.

## Références

- Compose Multiplatform : https://kotlinlang.org/compose-multiplatform/
- compose-multiplatform-core : https://github.com/JetBrains/compose-multiplatform-core
- skiko (Skia for Kotlin) : https://github.com/JetBrains/skiko
- GLFW : https://www.glfw.org/
- Skip (transpileur SwiftUI vers Kotlin/Compose) : https://skip.dev

La prise en charge skiko Kotlin/Native Linux arm64 sur laquelle ce projet s'appuie :

- [skiko#1051](https://github.com/JetBrains/skiko/pull/1051) « Add linuxArm64 target » (mergé juin 2025) : ajoute le klib skiko linuxArm64 cross-compilé que ce projet consomme.
- EGL sur linuxArm64, suivi par [SKIKO-918](https://youtrack.jetbrains.com/issue/SKIKO-918) / [skiko#918](https://github.com/JetBrains/skiko/issues/918) « Add EGL support » (**Fixed**) : skiko prend désormais en charge EGL sur linuxArm64 via `makeGL()`. Livré par [skia-pack#68](https://github.com/JetBrains/skia-pack/pull/68) (mergé oct. 2025) et [skiko#1052](https://github.com/JetBrains/skiko/pull/1052) (mergé janv. 2026).
- [SKIKO-611](https://youtrack.jetbrains.com/issue/SKIKO-611) / [skiko#611](https://github.com/JetBrains/skiko/issues/611) « Support Kotlin/Native on Windows and Linux (x86_64) » (**ouvert** sur YouTrack) : suit les cibles desktop x86_64 ; d'après le fil, le desktop Linux/Windows natif est community-driven, non priorisé par JetBrains. La cible arm64 utilisée ici est venue séparément via skiko#1051.
- [SKIKO-863](https://youtrack.jetbrains.com/issue/SKIKO-863) / [skiko#863](https://github.com/JetBrains/skiko/issues/863) « Support for Linux DRM » (**ouvert**) : rendu direct KMS/DRM sans window manager, pertinent pour un appareil embarqué/headless. Prior art dans le fil : `composeui-lightswitch` de Jake Wharton, qui a fait tourner Compose sur DRM.

La moitié Skip de ce repo (POC 1 et POC 6) part d'une suggestion des mainteneurs de Skip eux-mêmes :

- [discussion skiptools #163](https://github.com/orgs/skiptools/discussions/163) « Why not Compose Multiplatform? » : marcprux (mainteneur de Skip) suggérait d'exporter un projet Skip avec `skip export` et de regarder combien de changements il faut pour le faire tourner sur une autre cible Compose Multiplatform. Ce repo est cette expérience.

## Licence

Apache License 2.0. Voir [`LICENSE`](./LICENSE) et [`NOTICE`](./NOTICE). Ce repo inclut des fichiers source
copiés (et quelques-uns patchés) des projets AOSP / JetBrains Compose sous Apache-2.0, sous
`poc5-native/src/linuxArm64Main/` et (le même jeu) `poc6-native/src/linuxArm64Main/`, chacun conservant son
en-tête d'origine. Le fichier `NOTICE` consigne l'attribution et les modifications. Les cales de
`poc6-skip-cmp/` et `poc6-native/` sont du travail original ; la sortie transpilée de Skip n'est pas incluse
dans ce repo (voir « Reproduire »). Tout le reste est du travail original.
