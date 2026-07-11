// POC 6 Jalon 4 : kotlin.reflect.full est JVM-only. SkipFoundation (ProcessInfo, JSONDecoder, UUID)
// s'en sert pour l'objet companion et ses fonctions. Cales : la reflexion ne marche pas en K/N (comme
// les stubs android.jar cote JVM), on fournit juste la surface pour compiler.
package kotlin.reflect.full

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

val <T : Any> KClass<T>.companionObject: KClass<*>? get() = TODO("K/N reflect.full stub")
val <T : Any> KClass<T>.companionObjectInstance: Any? get() = TODO("K/N reflect.full stub")
val <T : Any> KClass<T>.functions: Collection<KFunction<*>> get() = TODO("K/N reflect.full stub")
val <T : Any> KClass<T>.declaredMemberProperties: Collection<kotlin.reflect.KProperty1<T, *>> get() = TODO("K/N reflect.full stub")
val <T : Any> KClass<T>.memberProperties: Collection<kotlin.reflect.KProperty1<T, *>> get() = TODO("K/N reflect.full stub")
val <T : Any> KClass<T>.primaryConstructor: KFunction<T>? get() = TODO("K/N reflect.full stub")

// KCallable.call est fourni par kotlin-reflect (JVM), absent en K/N. Extension stub pour compiler.
fun KFunction<*>.call(vararg args: Any?): Any? = TODO("K/N KFunction.call stub")

val <T : Any> KClass<T>.superclasses: List<KClass<*>> get() = TODO("K/N reflect.full stub")
val <T : Any> KClass<T>.allSuperclasses: Collection<KClass<*>> get() = TODO("K/N reflect.full stub")
