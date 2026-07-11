// POC 6 Jalon 4 : surface org.json (Android/JVM) utilisee par le parseur JSON de SkipFoundation.
// Cale compile-only ; un portage reel utiliserait kotlinx-serialization.
package org.json

class JSONTokener(input: String) {
    // Type de retour non-null pour coller au type-plateforme JVM (createJSONValue attend Any).
    fun nextValue(): Any = TODO("K/N org.json stub")
}

class JSONObject {
    fun get(name: String): Any = TODO("K/N org.json stub")
    fun keys(): Iterator<String> = TODO("K/N org.json stub")
    fun length(): Int = TODO("K/N org.json stub")
    fun opt(name: String): Any? = TODO("K/N org.json stub")
    companion object {
        val NULL: Any = Any()
    }
}

class JSONArray {
    fun get(index: Int): Any = TODO("K/N org.json stub")
    fun length(): Int = TODO("K/N org.json stub")
}

class JSONException(message: String) : RuntimeException(message)
