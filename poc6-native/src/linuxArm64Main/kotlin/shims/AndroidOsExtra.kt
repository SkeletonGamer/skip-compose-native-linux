package android.os

// Cales compile-only pour Kotlin/Native Linux : complement de la surface
// `android.os` (les cales de base Build/Bundle/Process/Looper sont dans
// AndroidOs.kt et ne sont pas redefinies ici). On couvre le retour haptique
// (`SensoryFeedback.kt`, `UIFeedbackGenerator.kt`), le `Handler`
// (`AccessibilityEnvironment.kt`), la serialisation `Parcelable`
// (`ComposeStateSaver.kt`) et le descripteur de fichier PDF (`AsyncImage.kt`).
// Les constantes lues comme valeurs recoivent leurs vraies valeurs Android.

/// Effet de vibration. Le code construit soit un effet predefini, soit une
/// composition de primitives. Les constantes d'effet et de primitive sont lues
/// comme valeurs (identifiants Android reels).
open class VibrationEffect {
    /// Composition de primitives haptiques (API 30+). Chaine `addPrimitive`
    /// puis `compose()`.
    open class Composition {
        fun addPrimitive(primitiveId: Int, scale: Float, delay: Int): Composition = TODO("K/N android stub")
        fun compose(): VibrationEffect = TODO("K/N android stub")

        companion object {
            const val PRIMITIVE_CLICK: Int = 1
            const val PRIMITIVE_THUD: Int = 2
            const val PRIMITIVE_SPIN: Int = 3
            const val PRIMITIVE_QUICK_RISE: Int = 4
            const val PRIMITIVE_SLOW_RISE: Int = 5
            const val PRIMITIVE_QUICK_FALL: Int = 6
            const val PRIMITIVE_TICK: Int = 7
            const val PRIMITIVE_LOW_TICK: Int = 8
        }
    }

    companion object {
        const val EFFECT_CLICK: Int = 0
        const val EFFECT_TICK: Int = 2
        const val EFFECT_HEAVY_CLICK: Int = 5

        fun startComposition(): Composition = TODO("K/N android stub")
        fun createPredefined(effectId: Int): VibrationEffect = TODO("K/N android stub")
        fun createOneShot(milliseconds: Long, amplitude: Int): VibrationEffect = TODO("K/N android stub")
    }
}

/// Service de vibration. Le code declenche un `VibrationEffect`.
open class Vibrator {
    fun vibrate(vibe: VibrationEffect): Unit = TODO("K/N android stub")
    fun hasVibrator(): Boolean = TODO("K/N android stub")
}

/// Gestionnaire de vibreurs (API 31+). Fournit le vibreur par defaut.
open class VibratorManager {
    fun getDefaultVibrator(): Vibrator = TODO("K/N android stub")
}

/// File de messages liee a un `Looper`. Seul le constructeur est reference
/// (passe au constructeur d'un `ContentObserver`).
open class Handler(looper: Looper)

/// Objet Parcel : buffer de serialisation Android. Seuls `obtain`, `readInt` et
/// `writeInt` sont lus (le reste du protocole est en commentaire cote appelant).
open class Parcel {
    fun readInt(): Int = TODO("K/N android stub")
    fun writeInt(value: Int): Unit = TODO("K/N android stub")

    companion object {
        fun obtain(): Parcel = TODO("K/N android stub")
    }
}

/// Contrat de serialisation Android. Implemente par `ComposeStateSaver.Key`.
interface Parcelable {
    fun writeToParcel(dest: Parcel, flags: Int): Unit
    fun describeContents(): Int

    /// Fabrique associee (contrat `CREATOR`).
    interface Creator<T> {
        fun createFromParcel(source: Parcel): T
        fun newArray(size: Int): Array<T?>
    }
}

/// Descripteur de fichier transportable. Le code l'ouvre en lecture seule pour
/// alimenter `PdfRenderer`, puis le ferme.
open class ParcelFileDescriptor {
    fun close(): Unit = TODO("K/N android stub")

    companion object {
        /// Mode d'ouverture en lecture seule.
        const val MODE_READ_ONLY: Int = 0x10000000

        fun open(file: java.io.File, mode: Int): ParcelFileDescriptor = TODO("K/N android stub")
    }
}
