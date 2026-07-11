// POC 6 Jalon 2: WorkManager is Android-only (background tasks). Stub.
package androidx.work

abstract class WorkManager {
    companion object {
        fun getInstance(context: android.content.Context): WorkManager = TODO("WorkManager shim")
    }
    abstract fun enqueue(request: WorkRequest)
}

class WorkerParameters
abstract class ListenableWorker(val context: android.content.Context, val params: WorkerParameters) {
    sealed class Result {
        class Success : Result()
        class Failure : Result()
        class Retry : Result()
        companion object {
            fun success(): Result = Success()
            fun failure(): Result = Failure()
            fun retry(): Result = Retry()
        }
    }
}
abstract class Worker(context: android.content.Context, params: WorkerParameters) : ListenableWorker(context, params) {
    abstract fun doWork(): Result
}
abstract class WorkRequest
class OneTimeWorkRequest : WorkRequest()
class OneTimeWorkRequestBuilder<T : ListenableWorker> {
    fun build(): OneTimeWorkRequest = OneTimeWorkRequest()
}
