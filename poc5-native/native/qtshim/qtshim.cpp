// The C++ side of the Qt shim. This file is the cost of Qt.
//
// Compose itself is untouched by any of this: the point of the third embedder is that the SAME compose klib,
// with the same 42 Linux actuals, drives a Qt window without a single line changing inside androidx/.
#include "qtshim.h"

#include <QGuiApplication>
#include <QWindow>
#include <QOpenGLContext>
#include <QSurfaceFormat>
#include <QTimer>
#include <QMouseEvent>
#include <QClipboard>
#include <QString>

#include <string>

static qt_render_cb g_render = nullptr;
static qt_mouse_cb g_mouse = nullptr;
static QOpenGLContext *g_ctx = nullptr;
static QWindow *g_win = nullptr;
static std::string g_clipboardText;

// Qt delivers input by virtual method override, not by callback, so the shim has to subclass QWindow and
// forward. This is exactly the marshalling work that a C toolkit does not impose.
class ComposeWindow : public QWindow {
public:
    ComposeWindow() { setSurfaceType(QWindow::OpenGLSurface); }

protected:
    void mousePressEvent(QMouseEvent *e) override {
        if (g_mouse) g_mouse(1, e->position().x(), e->position().y());
    }

    void mouseReleaseEvent(QMouseEvent *e) override {
        if (g_mouse) g_mouse(0, e->position().x(), e->position().y());
    }
};

static int g_argc = 1;
static char g_arg0[] = "poc5-qt";
static char *g_argv[] = {g_arg0, nullptr};

int qt_start(int width, int height, const char *title, qt_render_cb render, qt_mouse_cb mouse) {
    g_render = render;
    g_mouse = mouse;

    static QGuiApplication app(g_argc, g_argv);

    // GLES, for the same reason the GLFW and GTK builds use it: desktop libGL carries GLX, which drags in
    // libX11 and would break a Wayland-only system. Skia is happy on either.
    QSurfaceFormat fmt;
    fmt.setRenderableType(QSurfaceFormat::OpenGLES);
    fmt.setStencilBufferSize(8); // Skia needs stencil
    fmt.setDepthBufferSize(0);
    QSurfaceFormat::setDefaultFormat(fmt);

    ComposeWindow *w = new ComposeWindow();
    w->setFormat(fmt);
    w->setTitle(QString::fromUtf8(title));
    w->resize(width, height);
    w->show();
    g_win = w;

    g_ctx = new QOpenGLContext();
    g_ctx->setFormat(fmt);
    if (!g_ctx->create()) return 1;

    QTimer *timer = new QTimer();
    QObject::connect(timer, &QTimer::timeout, []() {
        if (!g_win->isExposed()) return;
        if (!g_ctx->makeCurrent(g_win)) return;
        if (g_render) g_render(qt_default_fbo());
        g_ctx->swapBuffers(g_win);
    });
    timer->start(16);

    return app.exec();
}

int qt_default_fbo(void) {
    return g_ctx ? (int) g_ctx->defaultFramebufferObject() : 0;
}

void qt_quit(void) {
    QGuiApplication::quit();
}

const char *qt_clipboard_get(void) {
    g_clipboardText = QGuiApplication::clipboard()->text().toStdString();
    return g_clipboardText.c_str();
}

void qt_clipboard_set(const char *text) {
    QGuiApplication::clipboard()->setText(QString::fromUtf8(text ? text : ""));
}
