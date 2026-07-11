// POC 6 Jalon 4 : javax.xml.parsers (DocumentBuilder pour le SVG des SF Symbols dans Image.kt). Cale K/N.
package javax.xml.parsers

class DocumentBuilder {
    fun parse(input: java.io.InputStream): org.w3c.dom.Document = TODO("K/N xml stub")
    fun parse(uri: String): org.w3c.dom.Document = TODO("K/N xml stub")
}

class DocumentBuilderFactory {
    fun newDocumentBuilder(): DocumentBuilder = TODO("K/N xml stub")
    var isNamespaceAware: Boolean = false
    companion object {
        fun newInstance(): DocumentBuilderFactory = DocumentBuilderFactory()
    }
}
