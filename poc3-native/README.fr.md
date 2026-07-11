# poc3-native : Compose/Skia en Kotlin/Native Linux desktop (sans JVM)

> Canonique : [`README.md`](./README.md) (anglais). Ceci est la copie française.

Sonde POC 3 : jusqu'où une UI Compose **sans JVM** peut-elle aller sur Linux via **Kotlin/Native** ?
Voir [`../FINDINGS-POC3.fr.md`](../FINDINGS-POC3.fr.md) pour le journal complet et le verdict.

## Ce que ça build

Un unique exécutable Kotlin/Native `linuxArm64` (sans JVM au runtime) qui :

- rend un **rectangle Skia offscreen** (raster), Jalon 1 ;
- exerce le snapshot state de **`compose.runtime`**, Jalon 2 ;
- rend un **rectangle Skia dans une vraie fenêtre GLFW** via un contexte OpenGL, Jalon 3 (brique fenêtrage).

Le tout contre les klibs K/N Linux officiels : `org.jetbrains.skiko:skiko:0.150.0` et
`org.jetbrains.compose.runtime:runtime:1.9.0`. (`compose.ui`/`foundation`/`material3` **ne sont pas**
publiés pour K/N Linux : ce gap est le travail restant de plusieurs semaines.)

## Réalité host / cible

Le compilateur Kotlin/Native n'a **pas de host `linux-aarch64`**. On **compile donc sur macOS arm64,
en cross-compilation vers `linuxArm64`**, puis on **exécute l'ELF dans un conteneur Linux arm64**.

## Build & run

```bash
# 1. Construire l'image conteneur runtime (JDK + gradle + deps natives).
docker build -f docker/Dockerfile -t poc3-native .

# 2. Extraire les headers + libs GLFW/GL/EGL arm64 nécessaires au cross-link (git-ignorés).
docker/extract-native.sh

# 3. Cross-compiler sur le host macOS → ELF linuxArm64.
gradle linkDebugExecutableLinuxArm64          # ou linkReleaseExecutableLinuxArm64

# 4a. Lancer les chemins offscreen + runtime (aucun écran requis) :
docker run --rm \
  -v "$PWD/build/bin/linuxArm64/debugExecutable:/b:ro" -v "$PWD/docker/out:/out" \
  poc3-native /b/poc3-native.kexe

# 4b. Lancer le chemin GL fenêtré sous Xvfb (voir FINDINGS pour les env vars :
#     DISPLAY, XDG_RUNTIME_DIR, LIBGL_ALWAYS_SOFTWARE=1, GALLIUM_DRIVER=llvmpipe).
```

Sorties dans `docker/out/` : `skia-knative.png` (offscreen), `poc3-window-gl.png` (relu depuis la
surface GL de la fenêtre). Copies sauvegardées : `../docs/poc3-skia-knative-arm64.png`,
`../docs/poc3-window-gl-arm64.png`.

## Poids (Jalon 4)

Binaire release auto-suffisant de 21,5 Mo (Skia inclus) vs l'image JVM Compose Desktop de 137 Mo
(JRE 87 Mo + jars 49 Mo). La RAM est dominée par Skia, donc à peine plus basse. Détails dans FINDINGS.
