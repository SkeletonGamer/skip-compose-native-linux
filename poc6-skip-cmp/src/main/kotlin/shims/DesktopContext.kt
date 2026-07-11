// POC 6 render (JVM): the single shared transpiled export references android.content.DesktopContext
// as its benign desktop Context (the name the Kotlin/Native build uses). On the JVM we back it with
// the same Mockito context ApplicationProvider fakes, so ONE export compiles and renders on both the
// JVM and the Kotlin/Native Linux target.
package android.content

val DesktopContext: Context by lazy {
    org.mockito.Mockito.mock(Context::class.java, org.mockito.Mockito.RETURNS_MOCKS)
}
