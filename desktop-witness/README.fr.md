# desktop-witness : SwiftUI transpilé par Skip sur Compose Multiplatform desktop

> Version canonique : [`README.md`](./README.md) (anglais). Ceci est la copie française.

Preuve end-to-end du PoC : le `ContentView.kt` **transpilé verbatim par Skip** (un compteur SwiftUI :
`VStack { Text; Button }` + `@State`) rendu et **interactif** sur **Compose Multiplatform desktop**
(JVM, Skia), sur Linux/macOS/Windows.

## Ce qui est réel vs adapté

- `src/main/kotlin/witness/module/ContentView.kt`, **la vraie sortie Skip**, copiée verbatim depuis
  `skip export` (`docs/transpiled-ContentView.kt`). Non éditée.
- `src/main/kotlin/skip/ui/SkipUiMini.kt`, un **adaptateur desktop minimal** (~200 lignes)
  reproduisant seulement la tranche de l'API SkipUI qu'utilise le witness (`View`, `ComposeBuilder`,
  `ComposeContext`, `ComposeResult`, `Renderable`, `State`, `LocalizedStringKey`, `VStack`, `Text`,
  `Button`, `.padding()`), implémentée sur JetBrains Compose. **Ce n'est pas le vrai SkipUI** (une
  interface Android-Compose de 3561 lignes) ; ça mesure la petitesse de cette tranche pour une app
  plancher.
- `skip/{lib,foundation,model}/Stub.kt` : packages vides pour que les star-imports du transpileur
  résolvent.
- `witness/Main.kt` : app fenêtrée Compose Desktop. `witness/RenderPng.kt` : rendu offscreen pour
  vérification headless.

## Lancer

```bash
gradle run        # ouvre la fenêtre
gradle renderPng  # écrit ../docs/desktop-witness*.png (sans écran)
```

Résultat : `docs/desktop-witness.png` (Count: 0) et `docs/desktop-witness-after-click.png` (Count: 1
après un clic simulé) : le compteur `@State` se met à jour et Compose recompose.

### Lancer sur Linux (Docker)

```bash
docker/render-linux.sh   # build l'image Linux, rend headless, écrit ../docs/linux-witness*.png
```

Rendu logiciel Skia headless dans un conteneur Linux. Note : skiko exige `libGL.so.1` même en mode
logiciel (l'image installe `libgl1`) ; voir `docker/Dockerfile` et `../FINDINGS.fr.md` étape 6.

## Sens pour le PoC

Le code UI transpilé par Skip **est portable** vers CMP desktop. Le coût, c'est le **runtime SkipUI** :
soit porter le vrai SkipUI (grand, couplé Android), soit (comme ici) fournir la petite tranche
qu'une app donnée exige. Voir `../FINDINGS.fr.md` pour l'analyse complète.
