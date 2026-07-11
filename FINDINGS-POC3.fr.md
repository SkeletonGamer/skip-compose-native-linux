# FINDINGS. POC 3 : Compose Multiplatform en Kotlin/Native Linux desktop (sans JVM)

> Canonique : [`FINDINGS-POC3.md`](./FINDINGS-POC3.md) (anglais). Ceci est la copie française.
> POC 1 (`FINDINGS.fr.md`) et POC 2 (`FINDINGS-POC2.fr.md`) sont clos : ne pas les écraser.

Projet R&D, pas une affaire d'un après-midi : construire un **backend desktop Linux natif** pour Compose afin de
supprimer la JVM (retirer la JRE de 87 Mo du POC 2 + la surcharge JVM des 137 Mo / 224 Mo). Par jalons ;
chaque jalon peut tuer/redéfinir le projet à bas coût. **Seul le Jalon 1 est limité dans le temps (~3 jours).**

## Synthèse exécutive (intermédiaire : session 1, Jalons 0-4 explorés)

**La fondation existe et fonctionne ; la prémisse de cadrage était périmée.** Le plan craignait que le
mur bas niveau soit « skiko ne se compile pas en K/N Linux ». Il se compile. En une session, sur Linux arm64,
**sans JVM** :

- **Jalon 0** : `skiko` livre un klib K/N `linuxArm64` officiel (0.150.0), ajouté par la PR fusionnée
  skiko#1051. La prémisse (« skiko n'a pas de K/N Linux ») est périmée pour arm64. À noter : SKIKO-611
  lui-même (K/N Windows/Linux x86_64) reste ouvert et porté par la communauté ; la cible arm64 est distincte.
- **Jalon 1** ✅ (signal-qui-tue) : un **rectangle Skia rend en Kotlin/Native Linux arm64** (raster
  hors écran, [`docs/poc3-skia-knative-arm64.png`](./docs/poc3-skia-knative-arm64.png)), cross-compilé
  depuis le host macOS (K/N n'a **pas de host linux-aarch64**).
- **Jalon 2** ✅ : `compose.runtime` s'exécute en K/N Linux (snapshot state), mais `compose.ui`,
  `foundation`, `material3` **ne sont pas publiés** pour K/N Linux. **C'est tout l'écart.**
- **Jalon 3** : la **brique fenêtrage marche** : skiko rend via GL dans une vraie **fenêtre GLFW** en
  K/N Linux (300 frames, sans JVM). Gouffre restant = brancher `ComposeScene` dessus (événements/polices/
  densité), le backend `compose.ui` Linux non publié = le travail de plusieurs semaines.
- **Jalon 4** : binaire autonome de **21,5 Mo** vs image JVM de **137 Mo** (sans JRE 87 Mo) ;
  RAM à peine plus basse (Skia domine).

**Verdict à ce stade : GO pour continuer.** Le signal-qui-tue est franchi nettement : les briques
rendu + runtime + fenêtrage existent toutes en natif pour K/N Linux aujourd'hui. Ce qui reste n'est pas
de la recherche sur *si* ça peut marcher, mais de l'ingénierie du backend `compose.ui` Linux
(`ComposeScene`↔GLFW : entrées, polices, hidpi). C'est borné et connu, mais c'est des semaines, pas un
après-midi. Détail par jalon ci-dessous.

## Objectif

Une UI Compose rendue sur Linux via **Kotlin/Native** (sans JVM), en répliquant ce que le chemin iOS
fait déjà en natif (Skia + fenêtrage natif) mais pour Linux, la couche que JetBrains n'a pas livrée.

## Point de départ réel (état de l'art, à vérifier au Jalon 0)

- Les cibles Kotlin/Native de skiko sont iOS + macOS ; Linux/Windows sont JVM-only. SKIKO-611 suit la
  prise en charge K/N Windows/Linux : ouvert, non livré.
- La partie difficile est le fenêtrage/événements/polices (ex-AWT, JVM-only), pas Compose↔skiko (déjà natif
  sur iOS/macOS). skiko peut être alimenté par un contexte GL créé par GLFW/SDL/Winit.

---

## Jalon 0 : Reconnaissance ✅ (une surprise clé)

**skiko livre déjà une cible Kotlin/Native Linux.** La prémisse portée dans ce POC (« skiko K/N ne
prend pas en charge Linux ; SKIKO-611 ouvert ») est **périmée**. Vérifié sur Maven Central :

```
org.jetbrains.skiko:skiko-linuxarm64:0.150.0  → skiko-linuxarm64-0.150.0.klib
org.jetbrains.skiko:skiko-linuxx64:0.150.0    → (existe aussi)
```

Le `.module` (métadonnées Gradle) confirme que c'est un **klib Kotlin/Native**, pas un jar JVM :

```
"org.gradle.usage": "kotlin-api" / "kotlin-runtime"
"org.jetbrains.kotlin.native.target": "linux_arm64"
```

Donc la « brique qui n'existe pas » (un moteur Skia K/N Linux) **existe en artefact officiel**
(skiko 0.150.0 ; à côté d'`iosarm64`, `macosarm64`, etc.). `mingwx64` reste absent (Windows K/N non
publié), mais **Linux arm64 et x64 K/N le sont**. Cela réduit radicalement le risque du mur bas niveau qui cadrait
le plan par jalons : le Jalon 1 peut se tenter contre un klib officiel plutôt qu'en compilant skiko
depuis les sources.

**Réserve (à prouver au Jalon 1, pas à supposer) :** le klib qui existe ≠ il se compile/se lie/rend. Ce qu'il
expose (rendu seul, ou fenêtrage aussi ?), s'il faut un contexte GL vs un raster software, et les deps
natives/`libGL`, c'est exactement ce que le Jalon 1 doit exercer empiriquement.

### Temps passé

- Jalon 0 : ~15 min.

## Jalon 1 : Signal-qui-tue ✅. Skia rend en Kotlin/Native Linux arm64, sans JVM

**La condition d'arrêt (« skiko ne se compile pas en K/N Linux ») est infirmée : il se compile, se lie et rend.**

### Constat host-arch (une vraie contrainte, contournée)

Le **compilateur Kotlin/Native n'a pas de host `linux-aarch64`**. Hosts publiés : `linux-x86_64`,
`macos-aarch64`, `macos-x86_64`, `windows-x86_64` (vérifié sur Maven Central :
`kotlin-native-prebuilt-2.3.0-linux-aarch64.tar.gz` → HTTP 404). Le compilateur ne peut donc pas
s'exécuter dans le conteneur Linux arm64. Contourné par la machine réelle : **compiler sur le host macOS
arm64, en cross-compilation vers la cible `linuxArm64`**, puis **exécuter l'ELF dans le conteneur
Linux arm64**.

### Build + run

- `poc3-native/`, projet `kotlin("multiplatform")`, unique cible `linuxArm64`, exécutable, dépendant
  de `org.jetbrains.skiko:skiko:0.150.0` (résout le klib K/N linuxArm64).
- Kotlin `2.1.21` n'a pas résolu le toolchain K/N sous Gradle 9.6.1 ; **Kotlin 2.3.0** fonctionne.
- `gradle linkDebugExecutableLinuxArm64` sur macOS → `BUILD SUCCESSFUL`. Sortie : `poc3-native.kexe` =
  **ELF 64-bit ARM aarch64**, dynamiquement lié, `/lib/ld-linux-aarch64.so.1`. **Sans JVM** : c'est un
  binaire natif ; la JVM n'intervenait qu'à la compilation.
- ELF exécuté dans le conteneur Linux arm64 → a écrit un PNG valide d'un rectangle Skia violet
  ([`docs/poc3-skia-knative-arm64.png`](./docs/poc3-skia-knative-arm64.png)), exit 0. Raster hors écran
  (`Surface.makeRasterN32Premul` → `canvas.drawRect` → `encodeToData(PNG)`), tout en `org.jetbrains.skia.*`.

### Deps d'exécution + taille (avant-première Jalon 4)

- **Deps dynamiques : glibc seulement** (`libc/libm/libpthread/libdl/libgcc_s/libcrypt/libresolv/librt`).
  **Aucun `libGL`, ni fontconfig, ni freetype** pour le chemin raster : Skia est lié statiquement.
  Bien plus léger que le chemin JVM, qui exigeait `libGL.so.1` même en software (POC 1/2).
- **Taille binaire : 21,5 Mo release** (autonome, Skia inclus), 25,5 Mo debug, **vs 137 Mo**
  pour l'app image JVM (POC 2), et **sans la JRE séparée de 87 Mo**. Première preuve dure que virer la
  JVM réduit radicalement l'empreinte.

### Honnêteté de périmètre

Ceci prouve la **brique de rendu + le toolchain** (Skia en K/N Linux arm64, sans JVM) : le mur bas
niveau que le plan craignait. C'est du **raster hors écran**, pas encore **à l'écran** : ouvrir une
fenêtre (GLFW + contexte GL + événements) est la couche fenêtrage = Jalon 3, le vrai gouffre. Mais la
fondation est verte : « ne rien construire au-dessus » était la règle, et le « quelque chose en
dessous » existe désormais.

### Temps passé

- Jalon 1 : ~50 min (dont l'impasse host-arch + téléchargement toolchain K/N + cross-compile).

## Jalon 2 : Compose runtime en K/N ✅ (quasi gratuit, comme prévu). Et l'écart est localisé

### Reconnaissance Maven : ce que Compose publie pour K/N Linux

| Artefact (klib K/N linuxArm64) | Publié ? |
|---|---|
| `org.jetbrains.skiko:skiko` (rendu Skia) | ✅ (Jalon 1) |
| `org.jetbrains.compose.runtime:runtime` | ✅ |
| `org.jetbrains.compose.ui:ui` | ❌ 404 |
| `org.jetbrains.compose.foundation:foundation` | ❌ 404 |
| `org.jetbrains.compose.material3:material3` | ❌ 404 |

(Par contraste, `runtime`, `ui`, `foundation`, `material3` publient tous des klibs
`iosarm64`/`macosarm64`.) Donc **le rendu (skiko) et le moteur de composition (compose.runtime)
existent pour K/N Linux ; la couche widgets + fenêtrage (ui/foundation/material3) non.** C'est
exactement la frontière « fenêtrage/événements/polices, pas Compose↔skiko » que la recherche pointait,
confirmée comme une ligne de packaging dure.

### Confirmation empirique

Ajout de `org.jetbrains.compose.runtime:runtime:1.9.0` à la cible linuxArm64 et exercice du snapshot
state (le cœur réactif), sans JVM, même binaire que le rectangle Skia :

```
compose.runtime in K/N Linux: count=21 derived(doubled)=42
```

`mutableStateOf` + `derivedStateOf` + `Snapshot.withMutableSnapshot` se compilent et s'exécutent en natif.
`BUILD SUCCESSFUL`, exécuté dans le conteneur Linux arm64. Jalon 2 vert avec une dépendance d'une
ligne : « quasi gratuit », comme annoncé.

### Ce que ça signifie pour les jalons

L'écart du POC 3 est maintenant précis : **ni** le toolchain (Jalon 1), **ni** le runtime (Jalon 2),
mais **`compose.ui` pour K/N Linux**, que JetBrains ne publie pas : le backend natif desktop-Linux de
fenêtrage / événements / polices / intégration skiko-GL. C'est le **Jalon 3**, et c'est tout le coût restant.

### Temps passé

- Jalon 2 : ~15 min.

## Jalon 3 : La brique fenêtrage marche ; brancher Compose est le gouffre restant

**Signal : skiko rend via OpenGL dans une vraie fenêtre GLFW en K/N Linux arm64, sans JVM.**

Binaire `linuxArm64` qui ouvre une fenêtre **GLFW** (cinterop C), active le contexte GL courant, crée une
surface skiko **GL** (`DirectContext.makeGL()` + `BackendRenderTarget.makeGL` +
`Surface.makeFromBackendRenderTarget`), et boucle en dessinant un rectangle Skia. Sous Xvfb dans le
conteneur Linux arm64 :

```
renderInWindow: drew 300 frames of a skiko rectangle in a real GLFW window (K/N Linux, no JVM)
```

### La plomberie de cross-link réellement nécessaire (la friction est réelle)

- **cinterop GLFW** : `-DGLFW_INCLUDE_NONE` dans le `.def` (le header GLFW tire `GL/gl.h`, absent ;
  skiko apporte son propre GL).
- **Libs natives pour une cible Linux cross-compilée depuis macOS** : extraction des `libglfw.so.3`,
  `libGL.so.1`, `libEGL.so.1` arm64 (+ headers GLFW) du conteneur vers le projet ; link
  `-lglfw -lGL -lEGL`.
- **`--allow-shlib-undefined`** : les libs Ubuntu arm64 réclament des versions de symboles glibc plus
  récentes (`@GLIBC_2.34/2.27`) que le sysroot linux embarqué de K/N ; sans ça l'édition de liens échoue sur
  `pthread_setspecific@GLIBC_2.34`, `powf@GLIBC_2.27`, etc. Résolu à l'exécution.
- **Exécution** : `XDG_RUNTIME_DIR` doit être défini, sinon GLFW/EGL avortent ; `LIBGL_ALWAYS_SOFTWARE=1` +
  `GALLIUM_DRIVER=llvmpipe` pour le GL software (pas de GPU dans le conteneur). Bibliothèques d'exécution :
  `libglfw3`, `libgl1`, `libegl1`, `libglx-mesa0`, `libgl1-mesa-dri`, `libx11-6`.

### Prouvé vs non

- **Prouvé** : la *brique fenêtrage* (fenêtre GLFW + contexte GL skiko + boucle de rendu) s'exécute en
  natif en K/N Linux arm64 sans JVM. Les fondations rendu (Jalon 1) et fenêtrage existent.
- **Visuel du rendu GL fenêtré** : capturé en **relisant la surface GL de la fenêtre** vers un
  `Bitmap` raster (`surface.readPixels` → `Image.makeFromBitmap` → PNG), le rectangle violet que skiko
  a dessiné via OpenGL dans le framebuffer de la fenêtre GLFW, en K/N Linux arm64, sans JVM :
  [`docs/poc3-window-gl-arm64.png`](./docs/poc3-window-gl-arm64.png). (Un premier essai
  `makeImageSnapshot().encodeToData()` renvoyait null : une image GPU doit d'abord être rasterisée ;
  `readPixels` vers un Bitmap est le chemin qui marche. `import -window root` sous Xvfb sans affichage reste
  vide faute de compositeur présentant la surface EGL au root X, mais c'est un artefact de présentation
  X, pas de rendu.)
- **Le vrai gouffre restant** : `compose.ui` pour K/N Linux **n'est pas publié** (Jalon 2). Transformer
  cette brique en app Compose = brancher `ComposeScene` sur la fenêtre GLFW : distribution des événements
  (souris/clavier/redimensionnement/défilement), chargement de polices (fontconfig), densité/hidpi, la couche
  équivalent-AWT que JetBrains ne livre qu'en JVM (desktop) et Kotlin/Native (iOS/macOS), pas Linux
  K/N. **C'est le travail de plusieurs semaines**, là où « vivent les semaines », comme prévu.

### Cadrage du travail restant (Jalon 3-proper)

Confirmé par recherche (JetBrains/kotlinlang, DeepWiki) : Compose desktop **repose sur la JVM**, où Skiko
fournit « rendu, gestion des événements **et gestion de fenêtres** » via le chemin hébergé AWT ; il n'y a
**pas de Compose Kotlin/Native Linux desktop officiel**. Les backends `compose.ui` K/N existent pour
**iOS/macOS** (fenêtrage natif) mais **pas Linux** (les 404 du Jalon 2). Donc le travail restant est
**entièrement nouveau pour Linux** : écrire un backend `compose.ui` Linux-K/N branchant `ComposeScene` sur une
fenêtre (GLFW/Winit) : entrées, polices (fontconfig), densité/hidpi. Le *patron* existe (les backends K/N
iOS/macOS), ce qui réduit le risque, mais c'est du code neuf, pas un flag de config : l'estimation de
plusieurs semaines tient.

**Confirmation au niveau source** (`JetBrains/compose-multiplatform-core`, `jb-main`, `compose/ui/ui/build.gradle`) :
les source sets du module ui sont `commonMain / skikoMain / nativeMain / iosMain / desktopMain / …`.
Le **source set `nativeMain` dépend de `:compose:ui:ui-uikit`** et le natif est configuré via
`configureDarwinFlags()`, c.-à-d. **la prise en charge Kotlin/Native de compose.ui est Darwin/UIKit (iOS)
uniquement ; pas de `linuxMain` ni de cible native Linux.** Bonne nouvelle : le source set
**`skikoMain`** (rendu partagé adossé à skiko) serait réutilisé ; la partie inédite est un **backend
plateforme Linux** (l'équivalent de `ui-uikit`, p. ex. `ui-glfw`) plus l'ajout des cibles
`linuxArm64`/`linuxX64` au module ui. Voilà la forme concrète du travail de plusieurs semaines :
borné, et désormais localisé précisément.

### Temps passé

- Jalon 3 (brique fenêtrage) : ~45 min.

## Jalon 4 : Poids. K/N natif vs JVM (137 Mo / 224 Mo)

Mesuré sur Linux arm64 :

| Mesure | K/N natif (POC 3) | JVM Compose Desktop (POC 2) |
|---|---|---|
| Distributable / binaire | **21,5 Mo** (release, autonome, Skia inclus) | 137 Mo app image (JRE 87 Mo + jars 49 Mo) |
| JRE séparée | **aucune** | 87 Mo |
| Deps d'exécution (chemin raster) | **glibc seulement** | exige `libGL.so.1` même en software |
| RSS | **177 Mo** de pic en rendu (dont Mesa llvmpipe software GL, binaire debug) | 224 Mo au repos |

**Lecture, honnête :** le gain net est l'**empreinte** : un binaire autonome de **21,5 Mo** vs
une image de 137 Mo, et **pas de JRE embarquée de 87 Mo**. **La RAM n'est pas un grand gain** : Skia +
framebuffers GL dominent, donc la RSS au pic de rendu (177 Mo, GL software) n'est que modestement sous
les 224 Mo au repos de la JVM, et ce n'est pas comparable strictement (pic-rendu-llvmpipe vs
JVM-repos ; binaire debug). Sur GPU réel et binaire release, l'image RAM différerait. **Conclusion :
virer la JVM réduit radicalement le disque et la distribution et supprime le préchauffage de la JVM, mais ne réduit pas la RAM :
Skia est le plancher.**

### Temps passé

- Jalon 4 : ~15 min.
