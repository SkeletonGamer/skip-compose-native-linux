// POC 6 Jalon 4 : petites cales frères pour SkipUI (webkit MimeTypeMap). Un package par fichier ;
// ce fichier ne porte QUE android.webkit (les autres packages ont leurs propres fichiers).
package android.webkit

class MimeTypeMap {
    fun getMimeTypeFromExtension(extension: String): String? = TODO("K/N webkit stub")
    fun getExtensionFromMimeType(mimeType: String): String? = TODO("K/N webkit stub")
    companion object {
        fun getSingleton(): MimeTypeMap = MimeTypeMap()
        fun getFileExtensionFromUrl(url: String): String = TODO("K/N webkit stub")
    }
}
