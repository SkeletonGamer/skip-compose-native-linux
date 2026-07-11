package org.commonmark.ext.gfm.strikethrough

import org.commonmark.Extension
import org.commonmark.node.Node

// Cales compile-only pour Kotlin/Native Linux : extension GFM strikethrough.
// Strikethrough est un noeud commonmark (teste par `node is Strikethrough`).

open class Strikethrough : Node()

class StrikethroughExtension {
    companion object {
        fun create(): Extension = object : Extension {}
    }
}
