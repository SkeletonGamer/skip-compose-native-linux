// POC 6 Jalon 4: the JVM-only reflection bridges `KClass.java` and `Class.kotlin` that SkipFoundation
// uses (FileManager's `X::class.java` for Files.readAttributes, ProcessInfo's `Class.forName(..).kotlin`).
// Declared in package skip.foundation so they are in scope there without editing every transpiled file.
// Stubs: reflection does not work on K/N, matching how android.jar stubs failed at runtime on the JVM side.
package skip.foundation

import kotlin.reflect.KClass
import java.lang.Class as JavaClass

// Fonctionnel : porte le nom qualifie de la KClass (Bundle/ProcessInfo lisent `.java.name`).
// (Alias JavaClass car l'extension elle-meme s'appelle `java` et masquerait le package.)
val <T : Any> KClass<T>.java: JavaClass<T> get() =
    JavaClass(this.qualifiedName ?: this.simpleName ?: "", this.simpleName ?: "")
val JavaClass<*>.kotlin: KClass<*> get() = TODO("K/N Class.kotlin stub")
