package org.xmlpull.v1

// Cales compile-only pour Kotlin/Native Linux : surface XmlPull utilisee par
// PropertyListSerialization.kt (lecture d'un plist XML).

interface XmlPullParser {
    fun setInput(inputStream: java.io.InputStream, inputEncoding: String?)
    fun getEventType(): Int
    fun getName(): String?
    fun getText(): String?
    fun next(): Int

    companion object {
        // Constantes d'evenement (vraies valeurs XmlPull), utilisees dans des `when`.
        const val START_DOCUMENT: Int = 0
        const val END_DOCUMENT: Int = 1
        const val START_TAG: Int = 2
        const val END_TAG: Int = 3
        const val TEXT: Int = 4
    }
}

class XmlPullParserException(message: String? = null) : Exception(message)

class XmlPullParserFactory {
    fun newPullParser(): XmlPullParser = TODO("K/N stub")
    fun setNamespaceAware(awareness: Boolean): Unit = TODO("K/N stub")

    companion object {
        fun newInstance(): XmlPullParserFactory = TODO("K/N stub")
    }
}
