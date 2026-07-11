package org.commonmark.parser

import org.commonmark.Extension
import org.commonmark.node.Node
import java.lang.Class

// Cale compile-only pour Kotlin/Native Linux : Parser commonmark (builder + parse)
// utilise par MarkdownNode.kt.

// Benin (runtime) : on ne parse pas de markdown pour le rendu de texte simple ; l'arbre est vide.
class Parser {
    fun parse(input: String): Node = org.commonmark.node.Document()

    class Builder {
        fun extensions(extensions: List<Extension>): Builder = this
        fun enabledBlockTypes(enabledBlockTypes: Set<Class<out Node>>): Builder = this
        fun build(): Parser = Parser()
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
