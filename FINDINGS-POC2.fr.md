# FINDINGS : POC 2 : Compose-first natif + réalité mobile ARM

> Version canonique : [`FINDINGS-POC2.md`](./FINDINGS-POC2.md) (anglais). Ceci est la copie française.
> Le POC 1 (transpileur Skip→CMP, clos) est dans [`FINDINGS.fr.md`](./FINDINGS.fr.md). Ne pas
> l'écraser.

Journal du POC 2 : versions réelles, commandes réellement lancées, erreurs brutes, correctifs,
captures par surface, chiffres de poids à l'exécution, état officiel des cibles CMP avec sources, temps
passé, et la décision go/no-go par cible.

## Synthèse exécutive

**Décision : GO Compose-first pour l'UI de l'appareil cible, selon la cible.** Une app 100 % Kotlin/Compose (zéro
Skip, zéro SwiftUI) rend, reste interactive, **navigue** (navigation-compose, un artefact CMP confirmé)
et **persiste** sur un **vrai écran Linux arm64** (Xvfb) et sur macOS, voir
[`poc2-linux-home.png`](./docs/poc2-linux-home.png) →
[`…-incremented.png`](./docs/poc2-linux-incremented.png) →
[`…-detail.png`](./docs/poc2-linux-detail.png) →
[`…-persisted.png`](./docs/poc2-linux-persisted.png).

Le bémol est le **poids à l'exécution**, pas la capacité. CMP desktop **repose sur la JVM** (JetBrains, officiel) ;
il n'existe **aucune cible desktop/Linux sans JVM**. Sur Linux arm64, l'app image fait **137 Mo**, la
RAM au repos **~224 Mo**, le démarrage à froid **460 ms**. Donc, par cible :

- **Android / iOS** → GO ; CMP tourne sur les environnements d'exécution natifs (ART / Kotlin-Native), les chiffres JVM
  ne s'appliquent pas.
- **Appliance / tablette (peu contrainte)** → GO ; la JVM jlink embarquée y est confortable.
- **Mobile Linux-ARM contraint faisant tourner la version *desktop*** → **ALERTE** : 137 Mo / 224 Mo est
  lourd et il n'y a pas de chemin sans JVM pris en charge : préférer la cible native Android ou budgéter la JVM.

Avec le POC 1 (transpileur Skip→CMP : **NO-GO**), la paire règle la couche UI de l'appareil cible : **ne pas
transpiler Skip, écrire du Compose Multiplatform**, et choisir la cible CMP selon le poids de l'appareil.

## Les deux questions

- **A** : Compose-first tient-il sur une base proche de l'appareil cible ? Une app CMP plancher, 100 %
  Kotlin/Compose (zéro Skip, zéro SwiftUI), rendue + interactive + persistante, sur une image Linux
  minimale, **à l'écran réel**.
- **B** : La réalité « mobile ARM » au-delà du JVM sans affichage : taille du distributable, RAM au repos,
  démarrage à froid sur Linux arm64 ; et existe-t-il une cible CMP sans JVM embarquée ?

## Trois surfaces de test (gardées distinctes)

1. macOS arm64 écran réel, vérification rapide de l'interactivité, **pas** l'appareil cible (Metal/CoreGraphics).
2. **Linux arm64 écran réel** : le juge. Ici via **Xvfb** (un vrai serveur X / framebuffer virtuel)
   en Docker, capturé avec ImageMagick, piloté avec xdotool. Distinct du hors écran du POC 1.
3. Linux arm64 software sans affichage, déjà fait au POC 1, non répété.

---

## Étape 1 : Projet CMP Kotlin pur (2026-07-11)

### Toolchain (réutilisée du POC 1, vérifiée)

- JDK 21 (Temurin) / Gradle 9.6.1 / plugin Compose Multiplatform `org.jetbrains.compose` 1.9.0,
  Kotlin 2.3.0 + `org.jetbrains.kotlin.plugin.compose`.

### Navigation à artefact CMP confirmé (pas présumé)

Le POC 1 a établi que `androidx.navigation3` n'a **aucun** artefact CMP. Vérifié sur Maven Central que
le portage navigation multiplateforme de JetBrains **existe** et a une release **stable** :

```
org.jetbrains.androidx.navigation:navigation-compose  → stable 2.9.2 (dernière 2.10.0-alpha02)
```

Adopté **2.9.2**. Le projet (`compose-first/`) compile : `compose.desktop.currentOs` +
`compose.material3` + `navigation-compose:2.9.2` résolvent tous. `BUILD SUCCESSFUL`.

## Étape 2 : App plancher, 100 % Kotlin/Compose

`compose-first/src/main/kotlin/app/Main.kt`, une app à deux écrans, zéro Skip, zéro SwiftUI :

- **Home** : `Column { Text("Count: $count"); Row { Button("-"); Button("+") }; if (count>0) Text("Positive"); Button("Details") }`, état via `remember { mutableStateOf(...) }`.
- **Detail** : atteint via `NavHost` + `rememberNavController().navigate("detail")` ; un bouton `< Back` (`popBackStack`) et `Text("Detail for ...")`.
- **Persistance** : `CounterStore` sur `java.util.prefs.Preferences`, le compteur survit aux redémarrages de process.

## Étape 3 : macOS écran réel (surface 1) ✅

`gradle run` ouvre une **fenêtre native** (JVM/Skia, macOS arm64). Confirmé **interactivement par
l'opérateur** (captures) : Count 0 → `+` → Count 1 avec « Positive » → « Details » → écran de détail
(« Detail for 1 », « < Back »). Rendu, interactivité, vue conditionnelle et **navigation
(navigation-compose CMP)** fonctionnent. C'est la vérification rapide, **pas** l'appareil cible.

### Constat : le hors écran ne suffit pas ici

Un `ImageComposeScene` (l'astuce hors écran du POC 1) lève `CompositionLocal LocalLifecycleOwner not
present` : navigation-compose exige un `LifecycleOwner`, que fournit une vraie fenêtre
`application { Window { … } }` mais pas une scène hors écran nue. C'est une preuve concrète de la règle
du POC 2 : l'**écran réel est requis** : le hors écran ne peut littéralement pas héberger la navigation
ici.

### Temps passé

- Étapes 1-3 : ~40 min.

## Étape 4 : Linux arm64 écran réel (Xvfb), le juge ✅

L'app tourne sur un **vrai serveur X** (Xvfb, framebuffer virtuel) dans un conteneur Linux **arm64**,
un serveur d'affichage, pas la scène hors écran du POC 1. App fenêtrée sous Xvfb, framebuffer capturé
avec ImageMagick, clics pilotés avec `xdotool`. Banc d'essai réutilisable :
[`compose-first/docker/`](./compose-first/docker/) (`Dockerfile` + `run-xvfb.sh`).

Chaîne complète prouvée (captures dans `docs/`) :

- **Rendu**, [`poc2-linux-home.png`](./docs/poc2-linux-home.png) : Count 0, `-`/`+`, Details.
- **Interactivité**, [`poc2-linux-incremented.png`](./docs/poc2-linux-incremented.png) : deux clics
  `+` → Count 2 + « Positive ».
- **Navigation** (navigation-compose CMP), [`poc2-linux-detail.png`](./docs/poc2-linux-detail.png) :
  Details → « < Back » + « Detail for 2 ».
- **Persistance**, [`poc2-linux-persisted.png`](./docs/poc2-linux-persisted.png) : arrêt + relance
  (process neuf) → Count 2 survit (`java.util.prefs`).

Pièges (mécaniques) : même besoin de `libGL.so.1` qu'au POC 1 (`libgl1`), plus `xvfb x11-utils xdotool
imagemagick libx11-6 libxext6 libxrender1 libxi6 libxtst6` et les polices DejaVu ;
`SKIKO_RENDER_API=SOFTWARE`. Pas de window manager sous Xvfb nu, donc la fenêtre se mappe en (0,0)
sans barre de titre, les coordonnées de clic sont relatives au contenu. **Question A tranchée : oui,
Compose-first rend, reste interactif, navigue et persiste sur un vrai écran Linux arm64.**

### Temps passé

- Étape 4 : ~40 min (image Docker + banc d'essai Xvfb + calibrage des clics).

## Étape 5 : Poids à l'exécution sur Linux arm64 (Question B)

Mesuré **dans le conteneur Linux arm64** (`uname -m` = `aarch64`, Temurin 21), sur l'app image
jpackage produite par `createDistributable` de Compose (JRE jlink + jars app). Banc d'essai :
[`compose-first/docker/measure.sh`](./compose-first/docker/measure.sh).

| Mesure | Valeur | Note |
|---|---|---|
| Distributable (app image) | **137 Mo** | total sur disque |
| - JRE embarqué (jlink) | **87 Mo** | l'environnement d'exécution Java embarqué |
| - jars app (`lib/app`) | **49 Mo** | skiko + Compose + stdlib Kotlin + navigation |
| Démarrage à froid → 1re fenêtre | **460 ms** | launcher → fenêtre mappée, sous Xvfb |
| RAM au repos (RSS) | **224 Mo** | mémoire résidente de la JVM au repos |

Lecture : le démarrage à froid (460 ms) est bon partout. **Le poids, c'est l'empreinte JVM+Skia** :
une image de 137 Mo et ~224 Mo de RAM au repos sont confortables pour une **appliance/tablette**, mais
lourds pour un **mobile contraint**. (La RSS n'est pas tunée : heap par défaut ; un vrai produit
capperait `-Xmx` et pourrait alléger l'image jlink, mais la lib native skiko + la baseline Compose
posent un plancher.) C'est exactement la distinction appliance-vs-mobile que le brief demande de garder
séparée.

### Temps passé

- Étape 5 : ~30 min (image jpackage + banc d'essai de mesure + correctifs).

## Étape 6 : État officiel des cibles CMP (Question B)

**Compose Multiplatform desktop repose sur la JVM.** D'après le repo officiel JetBrains
(github.com/JetBrains/compose-multiplatform) :

> « Compose Multiplatform targets **the JVM** and supports high-performance hardware-accelerated UI
> rendering on all major desktop platforms, macOS, Windows, and Linux. »

Cible par cible (support officiel) :

- **Desktop** (Windows/macOS/**Linux**) : **JVM** + Skia (skiko). Pas de cible Kotlin/Native desktop.
- **Android** : Jetpack Compose sur l'environnement d'exécution Android (ART), pas une JVM embarquée séparément.
- **iOS** : **Kotlin/Native** (compile natif, rend via skiko/Metal). Pas de JVM.
- **Web** : Kotlin/**Wasm** (Beta). Pas de JVM, mais cible navigateur, pas une fenêtre Linux native.

**Existe-t-il une cible CMP sans JVM embarquée, pour une app Linux native ? Non, pas officiellement.**
Les seules cibles Compose sans JVM sont iOS (Kotlin/Native) et Web (Wasm) ; aucune n'est une app
Linux/desktop native. skiko a lui-même des cibles Kotlin/Native et le moteur d'exécution Compose tourne
démontrablement en natif sur iOS, donc une compilation Kotlin/Native Linux desktop est *concevable*, mais
JetBrains ne livre **aucune configuration prise en charge** pour ça. Donc livrer de l'UI CMP sur une
appliance Linux ARM native aujourd'hui = **embarquer une JVM** (allégée par jlink) : les 137 Mo /
~224 Mo mesurés à l'étape 5. Simple énoncé de faisabilité ; non implémenté ici.

### Temps passé

- Étape 6 : ~15 min (sources).

## Étape 7 : Décision (par cible)

### Questions, tranchées

- **A. Compose-first tient sur une base proche de l'appareil cible : OUI.** Une app plancher 100 %
  Kotlin/Compose rend, reste interactive, navigue (navigation-compose CMP) et persiste
  (`java.util.prefs`) sur un **vrai écran Linux arm64** (Xvfb), et sur macOS. Zéro Skip, zéro SwiftUI.
- **B. Réalité mobile ARM : mesurée.** CMP desktop repose sur la JVM ; sur Linux arm64 l'app image fait
  **137 Mo**, la RSS au repos **224 Mo**, le démarrage à froid **460 ms**. Aucune cible desktop/Linux
  sans JVM prise en charge.

### Décision, par cible (comme l'exige le brief)

- **L'appareil cible en app Android / iOS → GO Compose-first, sans le poids desktop-JVM.** Sur ces cibles CMP
  utilise les environnements d'exécution *natifs* (ART Android ; Kotlin/Native iOS) : les 137 Mo / 224 Mo ci-dessus sont
  les chiffres **desktop-JVM** et ne s'appliquent **pas**. C'est le chemin le plus propre.
- **L'appareil cible en appliance / tablette (Linux ARM ou x86, peu contrainte) → GO.** Le build Compose Desktop
  avec JVM jlink embarquée y est confortable : 137 Mo sur disque, ~224 Mo de RAM au repos, démarrage
  sous la seconde.
- **L'appareil cible en mobile Linux-ARM *contraint* faisant tourner la version Compose *Desktop* → ALERTE.**
  L'empreinte JVM+Skia (137 Mo / ~224 Mo au repos) est lourde pour un appareil serré, et il n'existe
  **aucune cible CMP desktop/Linux sans JVM prise en charge**. Sur un tel appareil, préférer la **cible
  native Android** (CMP sans JVM séparée) ou budgéter explicitement la JVM ; ne pas supposer que le
  build desktop est gratuit.

### Recommandation

**Compose-first est validé pour l'UI de l'appareil cible**, ce qui confirme la direction du POC 1. Choisir la
*cible* CMP selon l'appareil : mobile natif (Android/iOS) quand c'est possible ; la version desktop JVM
seulement là où l'appareil peut porter ~137 Mo / ~224 Mo. La seule chose à ne pas faire à l'aveugle,
c'est faire tourner la version **desktop JVM** sur un mobile très contraint : là, mesurer d'abord
(chiffres de l'étape 5) plutôt que supposer.

### Note de contexte : « Skip is free » (2026, blog) ne change rien

Skip 1.7 a ouvert son moteur (`skipstone`) ; toujours **iOS + Android uniquement, aucune mention CMP**.
Forker Skip pour ajouter CMP est *possible* mais ne raccourcit pas le coût du POC 1 : l'écart est le
**moteur d'exécution** couplé Android (SkipUI/SkipFoundation, `navigation3` sans artefact CMP), pas le transpileur
désormais ouvert. Ce serait une grosse contribution upstream nécessitant l'adhésion des mainteneurs :
un autre objectif que livrer l'appareil cible. Renforce Compose-first.

### Méta / délai imparti

POC 2 fait en **~2h30** (très en deçà du délai imparti d'1 jour) : projet + app, macOS écran réel, Linux
arm64 écran réel (Xvfb), poids à l'exécution, état officiel des cibles. Ensemble, **POC 1 (Skip→CMP :
NO-GO) + POC 2 (Compose-first : GO, selon la cible)** donnent l'image complète pour la couche UI de
l'appareil cible.

### Temps passé

- Étape 7 : ~20 min.
