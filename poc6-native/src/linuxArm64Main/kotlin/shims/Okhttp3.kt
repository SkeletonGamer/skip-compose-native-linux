package okhttp3

import okio.ByteString

// Cales compile-only pour Kotlin/Native Linux : reproduisent la surface d'okhttp3
// reellement utilisee par le code transpile (URLSession.kt / URLSessionTask.kt /
// URLComponents.kt). Aucune implementation reseau, uniquement de quoi compiler.

class OkHttpClient {
    fun newCall(request: Request): Call = TODO("K/N stub")
    fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket = TODO("K/N stub")
    fun newBuilder(): Builder = TODO("K/N stub")

    class Builder {
        // Chainage : chaque setter renvoie le Builder.
        fun callTimeout(timeout: Long, unit: java.util.concurrent.TimeUnit): Builder = TODO("K/N stub")
        fun readTimeout(timeout: Long, unit: java.util.concurrent.TimeUnit): Builder = TODO("K/N stub")
        fun connectTimeout(timeout: Long, unit: java.util.concurrent.TimeUnit): Builder = TODO("K/N stub")
        fun writeTimeout(timeout: Long, unit: java.util.concurrent.TimeUnit): Builder = TODO("K/N stub")
        fun cache(cache: Cache): Builder = TODO("K/N stub")
        fun build(): OkHttpClient = TODO("K/N stub")
    }
}

class Request {
    class Builder {
        // Chainage : chaque setter renvoie le Builder.
        fun url(url: String): Builder = TODO("K/N stub")
        fun method(method: String, body: RequestBody?): Builder = TODO("K/N stub")
        fun headers(headers: Headers): Builder = TODO("K/N stub")
        fun header(name: String, value: String): Builder = TODO("K/N stub")
        fun addHeader(name: String, value: String): Builder = TODO("K/N stub")
        fun cacheControl(cacheControl: CacheControl): Builder = TODO("K/N stub")
        fun post(body: RequestBody): Builder = TODO("K/N stub")
        fun build(): Request = TODO("K/N stub")
    }
}

class Response {
    val body: ResponseBody? get() = TODO("K/N stub")
    val code: Int get() = TODO("K/N stub")
    val headers: Headers get() = TODO("K/N stub")
    val protocol: Protocol get() = TODO("K/N stub")
    fun close(): Unit = TODO("K/N stub")
}

// Version du protocole HTTP ; le code n'en lit que la representation textuelle.
class Protocol

abstract class ResponseBody {
    fun bytes(): ByteArray = TODO("K/N stub")
    fun byteStream(): java.io.InputStream = TODO("K/N stub")
}

abstract class RequestBody {
    companion object {
        // Extensions companion, comme dans okhttp reel
        // (import okhttp3.RequestBody.Companion.toRequestBody / asRequestBody).
        fun ByteArray.toRequestBody(): RequestBody = TODO("K/N stub")
        fun java.io.File.asRequestBody(): RequestBody = TODO("K/N stub")
    }
}

class Headers {
    fun toMap(): Map<String, String> = TODO("K/N stub")

    companion object {
        // import okhttp3.Headers.Companion.toHeaders
        fun Map<String, String>.toHeaders(): Headers = TODO("K/N stub")
    }
}

class HttpUrl {
    val querySize: Int get() = TODO("K/N stub")
    fun queryParameterName(index: Int): String = TODO("K/N stub")
    fun queryParameterValue(index: Int): String? = TODO("K/N stub")

    companion object {
        // import okhttp3.HttpUrl.Companion.toHttpUrl
        fun String.toHttpUrl(): HttpUrl = TODO("K/N stub")
    }
}

interface Call {
    fun enqueue(responseCallback: Callback)
    fun cancel()
}

interface Callback {
    fun onResponse(call: Call, response: Response)
    fun onFailure(call: Call, e: java.io.IOException)
}

// Cache HTTP sur disque : signature d'okhttp (repertoire + taille max en octets).
class Cache(directory: java.io.File, maxSize: Long)

class CacheControl {
    companion object {
        val FORCE_CACHE: CacheControl get() = TODO("K/N stub")
        val FORCE_NETWORK: CacheControl get() = TODO("K/N stub")
    }
}

interface WebSocket {
    fun send(text: String): Boolean
    fun send(bytes: ByteString): Boolean
    fun close(code: Int, reason: String?): Boolean
}

// Classe ouverte aux methodes par defaut (comme okhttp) : le code en derive un Listener.
abstract class WebSocketListener {
    open fun onOpen(webSocket: WebSocket, response: Response) {}
    open fun onClosed(webSocket: WebSocket, code: Int, reason: String) {}
    open fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
    open fun onMessage(webSocket: WebSocket, text: String) {}
    open fun onMessage(webSocket: WebSocket, bytes: ByteString) {}
}
