# poc4-native : un `ui-glfw` minimal, UI Compose interactive en Kotlin/Native Linux (sans JVM)

> Canonique : [`README.md`](./README.md) (anglais). Journal complet :
> [`../FINDINGS-POC4.fr.md`](../FINDINGS-POC4.fr.md).

Prouve que la verticale Compose complète tourne en natif sur Linux **sans JVM** : une vraie composition
`@Composable` (compiler plugin + Recomposer) → un `Applier`/arbre de nœuds custom → draw **skiko**
(rects + texte fontconfig) → une fenêtre **GLFW** → **input souris → recompose**. Un `ui-glfw` *minimal*
écrit main (pas le `compose.ui` de JetBrains ; pas de material3/foundation).

## Host / cible

Kotlin/Native n'a **pas de host linux-aarch64** → **cross-compiler sur macOS arm64 → `linuxArm64`**,
puis **exécuter l'ELF dans un conteneur Linux arm64** (réutilise l'image `poc3-native`).

## Build & run

```bash
# réutiliser l'image poc3 pour le conteneur runtime + l'extraction des libs natives :
(cd ../poc3-native && docker build -f docker/Dockerfile -t poc3-native .)

# libs natives nécessaires au cross-link (GLFW/GL/EGL + fontconfig/freetype), extraites dans native/glfw :
#   (poc4 réutilise l'extraction poc3 ; lancer ../poc3-native/docker/extract-native.sh puis ajouter fontconfig/freetype)

gradle linkDebugExecutableLinuxArm64        # cross-compile sur macOS
# lancer sous Xvfb avec : DISPLAY, XDG_RUNTIME_DIR, LIBGL_ALWAYS_SOFTWARE=1, GALLIUM_DRIVER=llvmpipe
# piloter un clic avec xdotool ; capturer via GL readPixels (voir FINDINGS-POC4 pour le run exact).
```

Jeu de link natif : `-lglfw -lGL -lEGL -lfontconfig -lfreetype --allow-shlib-undefined`.

## Résultat

UI interactive (`docs/poc4-ui-*-arm64.png`) : un header + un bouton dont le compteur passe 0→1 à un
vrai clic GLFW. Binaire release **24 Mo** auto-suffisant (sans JVM) vs l'image JVM de 137 Mo du POC 2.

## Suite (chemin A)

De vrais widgets material3/foundation exigent de compiler les modules ui de Compose pour Linux K/N + un
vrai backend `ui-glfw` depuis les sources (le `nativeMain` de `compose.ui` est agnostique de plateforme ;
seul `ui-uikit` est iOS). Des semaines.
