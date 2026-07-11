// POC 6 Jalon 4 : java.lang.ref.WeakReference (SkipModel/Publisher). K/N a kotlin.native.ref.WeakReference ;
// on l'adapte a l'API java (get()). Note : reference faible reelle cote K/N.
package java.lang.ref

class WeakReference<T : Any>(referent: T?) {
    private val impl = if (referent != null) kotlin.native.ref.WeakReference(referent) else null
    fun get(): T? = impl?.get()
    fun clear() { impl?.clear() }
}
