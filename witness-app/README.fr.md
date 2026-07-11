# App witness

> Version anglaise canonique : [`README.md`](./README.md). Ce fichier en est la copie française.

L'app SwiftUI witness que `skip export` transpile en Kotlin. Sa sortie transpilée est l'entrée du POC 1
(Skip → Compose Multiplatform) et du POC 6 (dé-Android-iser SkipUI pour CMP Desktop et Kotlin/Native Linux,
sans JVM). Le récit des POC est dans le [`README.md`](../README.fr.md) à la racine.

Elle contient deux écrans, tous deux transpilés par `skip export` :

- `ContentView` : l'écran riche (`@AppStorage`, `NavigationStack`, `NavigationLink`, `List`), utilisé par
  l'analyse du POC 1 sur l'écart Skip → Compose.
- `MinimalContentView` : un compteur et un bouton minimaux, rendus par le POC 6 (offscreen sur CMP Desktop,
  et dans une fenêtre GLFW en Kotlin/Native Linux sans JVM).

Le projet Gradle transpilé (`./export` à la racine) est gitignoré ; on le régénère avec `scripts/setup.sh`,
qui lance `skip export` ici et applique `scripts/export.patch`.

C'est un projet bi-plateforme [Skip](https://skip.dev) : un module Swift Package Manager autonome et un
projet Xcode qui se traduit en app Android Kotlin/Gradle via le plugin skipstone. Pour construire ou lancer
le witness lui-même (simulateur iOS / émulateur Android), ouvrir `Project.xcworkspace` dans Xcode ; voir la
doc de Skip.
