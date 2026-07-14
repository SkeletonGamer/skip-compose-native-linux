// The C face of Qt, hand-written, because Kotlin/Native cannot see Qt at all.
//
// cinterop binds C and Objective-C only (the .def file's `language` property accepts nothing else), and Qt
// is C++ with no C API. So a third embedder needs this: an extern "C" layer that owns the QObjects and
// marshals Qt's signals back to plain function pointers. GTK4 needed none of this, being plain C.
//
// This header is deliberately tiny. It is the honest measure of what a C++ toolkit costs a Kotlin/Native
// Compose port: everything Qt can do has to be re-exposed by hand, one function at a time.
#ifndef QTSHIM_H
#define QTSHIM_H

#ifdef __cplusplus
extern "C" {
#endif

/** Called once per frame, with the framebuffer Qt wants us to draw into. */
typedef void (*qt_render_cb)(int fbo);

/** Called on every mouse button event. pressed = 1 on press, 0 on release. */
typedef void (*qt_mouse_cb)(int pressed, double x, double y);

/** Creates the window and the GL context, then runs Qt's event loop. Blocks until qt_quit(). */
int qt_start(int width, int height, const char *title, qt_render_cb render, qt_mouse_cb mouse);

/** Qt, like GTK, may not render into framebuffer 0. Ask it, never assume. */
int qt_default_fbo(void);

void qt_quit(void);

/** The Qt system clipboard, so the embedder can fill Compose's clipboard seam with it. */
const char *qt_clipboard_get(void);
void qt_clipboard_set(const char *text);

#ifdef __cplusplus
}
#endif

#endif
