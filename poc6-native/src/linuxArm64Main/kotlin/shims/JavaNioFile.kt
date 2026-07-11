// Cales compile-only (compile-only shims) pour Kotlin/Native Linux.
// Reproduit la surface de `java.nio.file.*` reellement referencee par le code
// Kotlin transpile (principalement `FileManager.kt`), afin que ce code COMPILE
// en K/N. Aucun comportement reel : chaque corps leve `TODO`.
// Equivalent natif d'un android.jar strictement compile-only.
//
// Les types `java.io.*`, `java.util.*`, `java.net.*` et `java.lang.Class` sont
// fournis par d'autres cales : ils sont references par nom qualifie, jamais
// redefinis ici.
package java.nio.file

// Interfaces marqueuses pour typer les varargs d'options (comme en Java, ou
// StandardCopyOption/StandardOpenOption/LinkOption implementent ces contrats).
interface CopyOption

interface OpenOption

// Options de copie/deplacement referencees : REPLACE_EXISTING, ATOMIC_MOVE,
// COPY_ATTRIBUTES.
enum class StandardCopyOption : CopyOption {
    REPLACE_EXISTING,
    ATOMIC_MOVE,
    COPY_ATTRIBUTES
}

// Options d'ouverture referencees : CREATE, WRITE, TRUNCATE_EXISTING.
enum class StandardOpenOption : OpenOption {
    CREATE,
    WRITE,
    TRUNCATE_EXISTING
}

// LinkOption est aussi une option de copie (passee a Files.copy).
enum class LinkOption : CopyOption {
    NOFOLLOW_LINKS
}

// Seule l'entree CONTINUE est referencee par le code transpile.
enum class FileVisitResult {
    CONTINUE
}

// Chemin de fichier. Seuls les membres reellement appeles sont declares.
interface Path {
    fun getParent(): Path
    fun getFileName(): Path
    fun getName(index: Int): Path
    fun resolve(other: Path): Path
    fun relativize(other: Path): Path
    fun normalize(): Path
    fun toRealPath(vararg options: LinkOption): Path
    fun toUri(): java.net.URI
    fun toFile(): java.io.File
}

// Fabrique de Path : get(String, ...) et get(URI).
object Paths {
    fun get(first: String, vararg more: String): Path = TODO("K/N java.nio stub")
    fun get(uri: java.net.URI): Path = TODO("K/N java.nio stub")
}

// Visiteur d'arborescence. Le code transpile instancie un
// `object : SimpleFileVisitor<Path>() { override ... }`.
interface FileVisitor<T> {
    fun preVisitDirectory(dir: T, attrs: java.nio.file.attribute.BasicFileAttributes): FileVisitResult
    fun visitFile(file: T, attrs: java.nio.file.attribute.BasicFileAttributes): FileVisitResult
    fun visitFileFailed(file: T, exc: java.io.IOException?): FileVisitResult
    fun postVisitDirectory(dir: T, exc: java.io.IOException?): FileVisitResult
}

// Implementation de base surchargeable (les methodes sont `open` pour permettre
// les `override` du code transpile).
open class SimpleFileVisitor<T> : FileVisitor<T> {
    override fun preVisitDirectory(dir: T, attrs: java.nio.file.attribute.BasicFileAttributes): FileVisitResult = TODO("K/N java.nio stub")
    override fun visitFile(file: T, attrs: java.nio.file.attribute.BasicFileAttributes): FileVisitResult = TODO("K/N java.nio stub")
    override fun visitFileFailed(file: T, exc: java.io.IOException?): FileVisitResult = TODO("K/N java.nio stub")
    override fun postVisitDirectory(dir: T, exc: java.io.IOException?): FileVisitResult = TODO("K/N java.nio stub")
}

// Operations statiques sur les fichiers. Types de retour fideles a java.nio afin
// que le code transpile compile (chainage `.size()`, `.close()`, `?:`, etc.).
object Files {
    fun exists(path: Path, vararg options: LinkOption): Boolean = TODO("K/N java.nio stub")
    fun isDirectory(path: Path, vararg options: LinkOption): Boolean = TODO("K/N java.nio stub")
    fun isSymbolicLink(path: Path): Boolean = TODO("K/N java.nio stub")
    fun isReadable(path: Path): Boolean = TODO("K/N java.nio stub")
    fun isWritable(path: Path): Boolean = TODO("K/N java.nio stub")
    fun isExecutable(path: Path): Boolean = TODO("K/N java.nio stub")

    fun createDirectory(dir: Path, vararg attrs: java.nio.file.attribute.FileAttribute<*>): Path = TODO("K/N java.nio stub")
    fun createDirectories(dir: Path, vararg attrs: java.nio.file.attribute.FileAttribute<*>): Path = TODO("K/N java.nio stub")
    fun createSymbolicLink(link: Path, target: Path, vararg attrs: java.nio.file.attribute.FileAttribute<*>): Path = TODO("K/N java.nio stub")
    fun createTempFile(dir: Path, prefix: String, suffix: String, vararg attrs: java.nio.file.attribute.FileAttribute<*>): Path = TODO("K/N java.nio stub")

    fun delete(path: Path) { TODO("K/N java.nio stub") }
    fun deleteIfExists(path: Path): Boolean = TODO("K/N java.nio stub")

    fun copy(source: Path, target: Path, vararg options: CopyOption): Path = TODO("K/N java.nio stub")
    fun move(source: Path, target: Path, vararg options: CopyOption): Path = TODO("K/N java.nio stub")

    fun readSymbolicLink(link: Path): Path = TODO("K/N java.nio stub")

    fun write(path: Path, bytes: kotlin.ByteArray, vararg options: OpenOption): Path = TODO("K/N java.nio stub")

    fun list(dir: Path): java.util.stream.Stream<Path> = TODO("K/N java.nio stub")
    fun walk(start: Path, vararg options: LinkOption): java.util.stream.Stream<Path> = TODO("K/N java.nio stub")
    fun walkFileTree(start: Path, visitor: FileVisitor<Path>): Path = TODO("K/N java.nio stub")

    fun <A> readAttributes(path: Path, type: java.lang.Class<A>, vararg options: LinkOption): A = TODO("K/N java.nio stub")
    fun <V> getFileAttributeView(path: Path, type: java.lang.Class<V>, vararg options: LinkOption): V? = TODO("K/N java.nio stub")

    fun setPosixFilePermissions(path: Path, perms: java.util.Set<java.nio.file.attribute.PosixFilePermission>): Path = TODO("K/N java.nio stub")
    fun setLastModifiedTime(path: Path, time: java.nio.file.attribute.FileTime): Path = TODO("K/N java.nio stub")
}
