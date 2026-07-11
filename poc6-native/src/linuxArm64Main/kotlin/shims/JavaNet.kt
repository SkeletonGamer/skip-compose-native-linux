// Cales compile-only reproduisant la surface `java.net.*` utilisee par le
// code Kotlin transpile de SkipFoundation, afin de compiler en Kotlin/Native
// Linux (ou le paquet JVM `java.net` n'existe pas). Aucun comportement reel :
// les corps sont des `TODO("K/N stub")`. Seuls les membres reference par le
// code transpile sont declares.
package java.net

// URI : valeur d'URL cote SkipFoundation (champ `platformValue`). Parseur RFC 3986 fonctionnel (K/N n'a
// pas java.net) : scheme://userInfo@host:port/path?query#fragment.
open class URI {
    private val full: String
    val scheme: String?
    val host: String?
    val userInfo: String?
    val port: Int
    val path: String
    val rawPath: String?
    val query: String?
    val rawQuery: String?
    val fragment: String?
    val rawFragment: String?

    constructor(str: String) {
        full = str
        val m = URI_REGEX.find(str) ?: throw java.io.IOException("URISyntaxException: $str")
        scheme = m.groupValues[2].ifEmpty { null }
        val authority = if (m.groupValues[3].isNotEmpty()) m.groupValues[4] else null
        var ui: String? = null
        var h: String? = null
        var p = -1
        if (authority != null) {
            var rest = authority
            val at = rest.indexOf('@')
            if (at >= 0) { ui = rest.substring(0, at); rest = rest.substring(at + 1) }
            val colon = rest.lastIndexOf(':')
            if (colon >= 0 && rest.substring(colon + 1).all { it.isDigit() } && colon + 1 < rest.length) {
                p = rest.substring(colon + 1).toInt(); rest = rest.substring(0, colon)
            }
            h = rest.ifEmpty { null }
        }
        userInfo = ui; host = h; port = p
        path = m.groupValues[5]
        rawPath = path.ifEmpty { null }
        query = m.groupValues[7].ifEmpty { null }
        rawQuery = query
        fragment = m.groupValues[9].ifEmpty { null }
        rawFragment = fragment
    }

    private constructor(full: String, scheme: String?, host: String?, userInfo: String?, port: Int,
                        path: String, query: String?, fragment: String?) {
        this.full = full; this.scheme = scheme; this.host = host; this.userInfo = userInfo
        this.port = port; this.path = path; this.rawPath = path.ifEmpty { null }
        this.query = query; this.rawQuery = query; this.fragment = fragment; this.rawFragment = fragment
    }

    fun toURL(): URL = URL(full)

    // Resolution relative simplifiee : une URI absolue (avec scheme) l'emporte ; sinon on combine les chemins.
    fun resolve(uri: URI): URI {
        if (uri.scheme != null) return uri
        val base = path
        val merged = if (uri.path.startsWith("/")) uri.path
            else base.substringBeforeLast('/', "") + "/" + uri.path
        val rebuilt = (scheme?.let { "$it:" } ?: "") +
            (host?.let { "//$it" } ?: "") + merged +
            (uri.query?.let { "?$it" } ?: "") + (uri.fragment?.let { "#$it" } ?: "")
        return URI(rebuilt, scheme, host, userInfo, port, merged, uri.query, uri.fragment)
    }

    fun resolve(str: String): URI = resolve(URI(str))

    override fun toString(): String = full
    override fun equals(other: Any?): Boolean = other is URI && other.full == full
    override fun hashCode(): Int = full.hashCode()

    companion object {
        private val URI_REGEX =
            Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")
    }
}

// URL : obtenue via `URI.toURL()`, ouvre des connexions et porte le scheme
// de handler enregistre.
open class URL {
    // Construit une URL a partir d'une chaine (surface JVM, referencee pour
    // parite d'API).
    constructor(spec: String) {
        TODO("K/N stub")
    }

    val protocol: String?
        get() = TODO("K/N stub")

    val host: String?
        get() = TODO("K/N stub")

    val path: String
        get() = TODO("K/N stub")

    fun toURI(): URI = TODO("K/N stub")

    fun openConnection(): URLConnection = TODO("K/N stub")
    fun openStream(): java.io.InputStream = TODO("K/N stub")

    // Extensions kotlin.io referencees comme membres (String/Data(contentsOf:)) ; presentes pour compiler.
    fun readText(): String = TODO("K/N stub")
    fun readBytes(): ByteArray = TODO("K/N stub")

    override fun toString(): String = TODO("K/N stub")

    companion object {
        // Enregistre la fabrique globale de handlers de scheme. No-op runtime : on ne resout pas les
        // URLs `asset:` de Skip a ce stade, mais l'init du Bundle doit pouvoir continuer.
        fun setURLStreamHandlerFactory(factory: URLStreamHandlerFactory) { }
    }
}

// URLConnection : connexion ouverte mais non encore etablie. Le champ `url`
// est protege et lu par les sous-classes (ex. AssetURLConnection).
abstract class URLConnection(protected val url: URL) {
    open fun connect() {
        TODO("K/N stub")
    }

    open fun getInputStream(): java.io.InputStream = TODO("K/N stub")

    open fun setUseCaches(usecaches: Boolean) {
        TODO("K/N stub")
    }
}

// URLStreamHandler : fabrique une connexion pour une URL d'un scheme donne.
abstract class URLStreamHandler {
    abstract fun openConnection(url: URL): URLConnection
}

// URLStreamHandlerFactory : cree le handler associe a un protocole (scheme).
interface URLStreamHandlerFactory {
    fun createURLStreamHandler(protocol: String): URLStreamHandler?
}

// InetAddress : seul l'acces au nom d'hote local est reference.
open class InetAddress {
    val hostName: String
        get() = TODO("K/N stub")

    companion object {
        fun getLocalHost(): InetAddress = TODO("K/N stub")
    }
}

