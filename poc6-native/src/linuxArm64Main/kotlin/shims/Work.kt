// POC 6 K/N Linux : androidx.work (WorkManager est Android-only ; pas d'artefact K/N Linux).
// Cales compile-only : surface strictement limitée à ce que UserNotifications.kt référence.
package androidx.work

// Data + Data.Builder : construction (put*) et lecture (get*, keyValueMap) utilisées par SkipUI.
class Data {
    fun getInt(key: String, defaultValue: Int): Int = TODO("K/N stub")
    fun getString(key: String): String? = TODO("K/N stub")
    val keyValueMap: Map<String, Any?>
        get() = TODO("K/N stub")

    class Builder {
        fun putString(key: String, value: String?): Builder = TODO("K/N stub")
        fun putInt(key: String, value: Int): Builder = TODO("K/N stub")
        fun putBoolean(key: String, value: Boolean): Builder = TODO("K/N stub")
        fun putDouble(key: String, value: Double): Builder = TODO("K/N stub")
        fun build(): Data = TODO("K/N stub")
    }
}

abstract class WorkRequest
class OneTimeWorkRequest : WorkRequest()

// Chaîne fluide construite par SkipUI : setInitialDelay / setInputData / addTag / build.
class OneTimeWorkRequestBuilder<T : ListenableWorker> {
    fun setInitialDelay(duration: Long, timeUnit: java.util.concurrent.TimeUnit): OneTimeWorkRequestBuilder<T> = TODO("K/N stub")
    fun setInputData(inputData: Data): OneTimeWorkRequestBuilder<T> = TODO("K/N stub")
    fun addTag(tag: String): OneTimeWorkRequestBuilder<T> = TODO("K/N stub")
    fun build(): OneTimeWorkRequest = TODO("K/N stub")
}

abstract class WorkManager {
    abstract fun enqueue(request: WorkRequest)

    companion object {
        fun getInstance(context: android.content.Context): WorkManager = TODO("K/N stub")
    }
}

class WorkerParameters

abstract class ListenableWorker(val context: android.content.Context, val params: WorkerParameters) {
    fun getInputData(): Data = TODO("K/N stub")

    sealed class Result {
        companion object {
            fun success(): Result = TODO("K/N stub")
        }
    }
}

abstract class Worker(context: android.content.Context, params: WorkerParameters) : ListenableWorker(context, params) {
    abstract fun doWork(): Result
}
