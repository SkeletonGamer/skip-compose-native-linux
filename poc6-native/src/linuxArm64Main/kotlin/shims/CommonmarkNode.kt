package org.commonmark.node

// Cales compile-only pour Kotlin/Native Linux : arbre de noeuds commonmark tel que
// parcouru par MarkdownNode.kt (firstChild / next, plus literal / destination sur
// certains types).

// Benin : arbre vide (le walk s'arrete immediatement).
open class Node {
    open val firstChild: Node? get() = null
    open val next: Node? get() = null
}

open class Document : Node()

open class Paragraph : Node()

open class Text : Node() {
    open val literal: String get() = ""
}

open class Emphasis : Node()

open class StrongEmphasis : Node()

open class Code : Node() {
    open val literal: String get() = ""
}

open class Link : Node() {
    open val destination: String get() = ""
}

open class HardLineBreak : Node()

open class SoftLineBreak : Node()
