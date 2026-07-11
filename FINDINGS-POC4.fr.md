# FINDINGS. POC 4 : un backend Linux `ui-glfw` pour Compose en Kotlin/Native (sans JVM)

> Canonique : [`FINDINGS-POC4.md`](./FINDINGS-POC4.md) (anglais). Ceci est la copie française. Les
> FINDINGS POC 1-3 sont clos : ne pas les écraser. Le POC 3 a prouvé la fondation (skiko +
> compose.runtime + une brique fenêtrage GLFW tournent tous en K/N Linux arm64, sans JVM). Le POC 4
> attaque l'écart restant : une vraie UI Compose.

## Synthèse exécutive (session 1 : une UI Compose interactive, sans JVM)

**Une UI Compose interactive tourne en Kotlin/Native Linux arm64 sans JVM**, prouvé de bout en bout :

- **Jalon 0** ✅ : cadrage, le code natif de compose.ui est indépendant de la plateforme (`ui-uikit` isolé dans
  `iosMain`), et ses dépendances natives partagées (lifecycle, coroutines, internes compose) ont toutes des
  klibs K/N Linux. L'écart = seulement les modules ui compilés pour Linux + un backend plateforme
  (`ui-glfw`).
- **Jalon 1** ✅ : une vraie composition `@Composable` **compose + recompose** en K/N Linux (compiler
  plugin + Recomposer + Applier sur mesure).
- **Jalon 2** ✅ : cette composition est **rendue par skiko dans une vraie fenêtre GLFW** : boîtes +
  texte fontconfig, compteur vivant ([`docs/poc4-ui-knative-arm64.png`](./docs/poc4-ui-knative-arm64.png)).
- **Jalon 3** ✅ : un vrai **clic GLFW → recompose** (count 0→1 :
  [avant](./docs/poc4-ui-before-arm64.png) / [après](./docs/poc4-ui-after-arm64.png)).
- **Jalon 4** ✅ : toute l'UI interactive tient dans un binaire natif auto-suffisant de **24 Mo** vs
  l'image JVM de 137 Mo du POC 2 (sans JRE de 87 Mo).

**C'est un `ui-glfw` *minimal* écrit à la main (chemin B)** : il prouve que la verticale complète
(composition → skiko → fenêtre → entrée) marche en natif sans JVM, et la mesure. Ce n'est **pas** le
`compose.ui` de JetBrains : pas de vrai measure/layout, pas de widgets `foundation`/`material3`. Les
livrer (pour que de vraies apps Compose tournent) = **chemin A** : ajouter les cibles K/N Linux aux
modules ui de Compose + un vrai backend `ui-glfw`, depuis les sources : le chantier de plusieurs
semaines cadré au Jalon 0. **Verdict : l'approche est prouvée viable ; un backend de production est
borné, connu, et reste des semaines d'ingénierie.**

> En rétrospective (ajouté après le POC 5) : le POC 5 a réalisé exactement ce chemin A, et rendu du
> material3 interactif, en environ une demi-journée. Cette estimation « plusieurs semaines » était trop
> conservatrice. Elle vaut pour le chemin qui chevauche le Compose existant, pas pour l'estimation plus
> lourde du portage de Skip au POC 1.

## Objectif

Faire tourner une vraie UI Compose (composition → layout → draw skiko → fenêtre, avec entrée) en
**Kotlin/Native Linux, sans JVM**, c.-à-d. fournir l'équivalent Linux du backend `ui-uikit` de
`compose.ui` (appelons-le `ui-glfw`). R&D, par jalons ; seul le jalon courant est limité dans le temps.

## Jalon 0 : Cadrage (depuis POC 3 + recon source/deps) ✅

**La structure est favorable et l'écart est précisément localisé.**

- **Le code natif de compose.ui est indépendant de la plateforme, sauf le backend plateforme.** Dans
  `JetBrains/compose-multiplatform-core` (`jb-main`, `compose/ui/ui/build.gradle`), les source sets
  sont `skikoMain ← nonJvmMain ← nativeMain`, et le backend plateforme iOS (`:compose:ui:ui-uikit`)
  n'est tiré **que dans `iosMain`**. Donc le moteur natif partagé (ComposeScene, layout, draw) est dans
  `nativeMain` ; seuls fenêtrage/events/polices sont spécifiques à la plateforme.
- **Les dépendances externes de ce natif partagé sont déjà publiées pour K/N Linux :**

  | Dep | klib K/N linuxArm64 |
  |---|---|
  | `skiko` | ✅ (POC 3) |
  | `compose.runtime:runtime` | ✅ (POC 3) |
  | `androidx.lifecycle:lifecycle-viewmodel` / `-runtime` | ✅ |
  | `compose.annotation-internal:annotation` | ✅ |
  | `compose.collection-internal:collection` | ✅ |
  | `kotlinx-coroutines-core` | ✅ |
  | `compose.ui:ui-backhandler` | ❌ (module ui frère, compilé avec) |

  Donc rien d'externe ne bloque ; les pièces manquantes sont les modules ui de Compose compilés pour
  Linux K/N **+ un backend plateforme Linux**.

### Deux chemins d'implémentation

- **Chemin A (builder depuis les sources)** : forker `compose-multiplatform-core`, ajouter les cibles
  `linuxArm64`/`linuxX64` à `ui`, `foundation`, `material3`, `ui-backhandler`, et écrire un backend
  plateforme `linuxMain`/`ui-glfw`. Vrais widgets material3 ; upstream-able. Build monorepo lourd ;
  semaines.
- **Chemin B (minimal sur le runtime publié)** : construire un petit `ui-glfw` sur le `compose.runtime`
  + `skiko` *publiés* + GLFW : un `Applier` sur mesure + layout + draw skiko + entrée GLFW. Prouve
  l'architecture de bout en bout et mesure le backend, sans material3. Plus rapide ; la méthode des POC
  précédents (adaptateur minimal + mesurer l'écart).

Cette session poursuit **le chemin B d'abord** (une UI Compose native interactive est le signal-qui-tue),
et cadre le chemin A.

## Jalon 1 : Une vraie composition Compose compose + recompose en K/N Linux ✅

Au-delà du snapshot-state du POC 3 : une **composition @Composable** complète avec le **compiler plugin
Compose**, un `AbstractApplier` sur mesure construisant un arbre de nœuds, un `Recomposer` +
`BroadcastFrameClock`, piloté manuellement. Cross-compilé macOS → `linuxArm64`, exécuté dans le
conteneur Linux arm64 :

```
POC4 Jalon1: composed once -> count=0
POC4 Jalon1: after state change -> count=42
POC4 Jalon1: composition+recomposition on K/N Linux, no JVM => OK
```

- La composition initiale exécute le `@Composable`, l'Applier matérialise le nœud (`count=0`).
- Un changement de `mutableStateOf` + `Snapshot.sendApplyNotifications()` + `clock.sendFrame()`
  déclenche la **recomposition** ; l'arbre passe à `count=42`. Sans JVM (ELF natif).
- Un piège : `recomposer.runRecomposeAndApplyChanges()` doit être lancé avec le `MonotonicFrameClock`
  dans **son** contexte coroutine (`launch(clock) { … }`) : sinon `IllegalStateException: A
  MonotonicFrameClock is not available`.

**Le moteur de composition, le cœur de Compose (compiler plugin + Recomposer + Applier + snapshot +
frame clock), tourne en natif sur K/N Linux.** Le klib `compose.runtime` publié suffit. Ensuite :
transformer l'arbre de nœuds en pixels via skiko, dans la fenêtre GLFW (Jalon 2).

### Temps passé

- Jalon 1 : ~25 min.

## Jalon 2 : Composition → draw skiko dans une fenêtre GLFW ✅ (une vraie UI Compose à l'écran)

Un `ui-glfw` minimal de bout en bout : des `@Composable Box/Label` construisent un arbre `UiNode` via le
`UiApplier` sur mesure ; une boucle de rendu parcourt l'arbre et le dessine avec **skiko** (rects arrondis +
texte) dans la **fenêtre GL GLFW** du POC 3 ; un compteur `mutableStateOf` recompose en direct tous les
~40 frames. Sur K/N Linux arm64, **sans JVM**, preuve visuelle relue depuis la surface GL :
[`docs/poc4-ui-knative-arm64.png`](./docs/poc4-ui-knative-arm64.png) : un header « Compose on
Kotlin/Native Linux » et un bouton violet affichant un « count: N » vivant.

```
wrote /out/poc4-ui.png (6989 bytes)
POC4: rendered a live Compose UI (count reached 5) in a GLFW window on K/N Linux, no JVM
```

### Plomberie d'édition de liens ajoutée (polices)

Le `FontMgr` de skiko sur Linux utilise **fontconfig + freetype** : l'édition de liens échouait sur
`FcPatternAddString`, `FcFontSetAdd`… jusqu'à ajouter `-lfontconfig -lfreetype` (libs arm64 extraites
comme GLFW/GL/EGL). Le texte est chargé via `FontMgr.default.makeFromFile(".../DejaVuSans.ttf")`
(`Typeface.makeFromFile` n'existe pas dans l'API K/N). Bibliothèques natives à lier, désormais au complet :
`glfw, GL, EGL, fontconfig, freetype`.

### Ce que ça prouve

**Toute la verticale marche en natif sur K/N Linux sans JVM** : compiler plugin Compose + Recomposer
(composition/recomposition) → un Applier/arbre de nœuds sur mesure (le travail de la couche `ui`) → draw
**skiko** (rects + texte fontconfig/freetype) → une vraie fenêtre **GLFW**. C'est un `ui-glfw`
*minimal* écrit à la main (chemin B) : il rend l'arbre composé mais n'est pas le `compose.ui` de JetBrains
(pas de vrai layout/measure, pas de widgets foundation/material3). Binaire : 32,5 Mo debug (skiko +
compose runtime inclus), toujours sans JRE séparée.

### Temps passé

- Jalon 2 : ~40 min.

## Jalon 3 : Entrée → recompose ✅ (interactif)

Polling du bouton gauche chaque frame (`glfwGetMouseButton`), détection du front de clic, et au clic
increment du state `count` (`Snapshot.sendApplyNotifications()`) → recomposition → re-rendu. Piloté par
un **vrai clic `xdotool`** sur la fenêtre GLFW sous Xvfb :

- avant : [`docs/poc4-ui-before-arm64.png`](./docs/poc4-ui-before-arm64.png), « count: 0 »
- après un clic : [`docs/poc4-ui-after-arm64.png`](./docs/poc4-ui-after-arm64.png), « count: 1 »

Un vrai event souris GLFW a piloté le state Compose → recomposition → re-rendu, sur K/N Linux, sans
JVM. (Hit-test grossier = toute la fenêtre ; la détection de front par polling a raté certains des 3
clics rapides `xdotool` sous le timing sans affichage, mais la boucle clic→recompose est prouvée. Le
hit-testing layout-aware réel est ce que fournit le `compose.ui` de JetBrains : la pièce qu'un `ui-glfw`
complet brancherait.)

### Temps passé

- Jalon 3 : ~20 min.

## Jalon 4 : Mesure ✅

L'**UI Compose interactive complète** (compiler Compose + runtime + skiko + le ui-glfw minimal) en
binaire Kotlin/Native `linuxArm64` auto-suffisant, **sans JVM** :

| | POC 4 (K/N, UI Compose interactive) | POC 2 (JVM Compose Desktop) |
|---|---|---|
| Binaire / image | **24,0 Mo** release (32,5 Mo debug) | 137 Mo app image (JRE 87 Mo + jars 49 Mo) |
| JRE séparée | aucune | 87 Mo |

Toute la stack UI Compose (composition + skiko + fenêtrage) tient dans un binaire natif auto-suffisant
de **24 Mo** vs une image JVM de 137 Mo. Comme noté au POC 3, la RAM est dominée par Skia donc le gain
RAM est modeste ; le gain d'**empreinte** est décisif.

### Temps passé

- Jalon 4 : ~10 min.
