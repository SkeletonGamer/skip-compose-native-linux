// POC 6 render: SkipFoundation's ProcessInfo reflectively calls
// androidx.test.core.app.ApplicationProvider.getApplicationContext() to get a JVM Context (Skip's
// Robolectric path). We provide that class with a Mockito Context so the render can boot on desktop
// without a full Robolectric environment. Deep methods on the mock return Mockito defaults.
package androidx.test.core.app

object ApplicationProvider {
    private val fakeContext: android.content.Context by lazy {
        org.mockito.Mockito.mock(
            android.content.Context::class.java,
            org.mockito.Mockito.RETURNS_MOCKS,
        )
    }

    @JvmStatic
    fun getApplicationContext(): android.content.Context = fakeContext
}
