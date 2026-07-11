// POC 6 Jalon 4 : org.w3c.dom (Image.kt parse le XML SVG des SF Symbols). Cale compile-only K/N.
package org.w3c.dom

interface Node {
    val nodeName: String
    val childNodes: NodeList
    // Type-plateforme JVM : non-null (le code appelle des methodes dessus sans null-check).
    fun getAttribute(name: String): String
}

interface Element : Node

interface NodeList {
    val length: Int
    fun item(index: Int): Node?
}

interface Document : Node {
    fun getElementsByTagName(tagname: String): NodeList
    val documentElement: Element
}
