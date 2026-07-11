// Cales compile-only (compile-only shims) pour Kotlin/Native Linux.
// Surface `java.nio.file.attribute.*` referencee par `FileManager.kt`.
// Corps = `TODO` : aucun comportement reel, uniquement la compilation.
package java.nio.file.attribute

// Attributs de base d'un fichier (retour de Files.readAttributes(..., BasicFileAttributes)).
interface BasicFileAttributes {
    fun size(): Long
    fun creationTime(): FileTime
    fun lastModifiedTime(): FileTime
    fun isDirectory(): Boolean
    fun isRegularFile(): Boolean
    fun isSymbolicLink(): Boolean
    fun isOther(): Boolean
    fun fileKey(): Any?
}

// Attributs POSIX (retour de Files.readAttributes(..., PosixFileAttributes)).
interface PosixFileAttributes : BasicFileAttributes {
    fun owner(): UserPrincipal
    fun permissions(): java.util.Set<PosixFilePermission>
}

// Vue d'attributs POSIX : seul le TYPE est utilise (getFileAttributeView(..., ::class.java)).
interface PosixFileAttributeView

// Principal proprietaire : seul getName() est appele.
interface UserPrincipal {
    fun getName(): String
}

// Les 9 permissions POSIX sont toutes referencees.
enum class PosixFilePermission {
    OWNER_READ,
    OWNER_WRITE,
    OWNER_EXECUTE,
    GROUP_READ,
    GROUP_WRITE,
    GROUP_EXECUTE,
    OTHERS_READ,
    OTHERS_WRITE,
    OTHERS_EXECUTE
}

// Horodatage de fichier : fabrique statique fromMillis + lecture toMillis.
class FileTime {
    fun toMillis(): Long = TODO("K/N java.nio stub")

    companion object {
        fun fromMillis(value: Long): FileTime = TODO("K/N java.nio stub")
    }
}

// Attribut de fichier generique : seulement utilise pour typer des varargs
// (Files.createDirectory/createTempFile/...). Aucun membre appele.
interface FileAttribute<T>
