// A minimal Wayland input method, for testing the IME path end to end.
//
// Why this exists: proving that the app's zwp_text_input_v3 client works needs something on the other side
// of the compositor. A real IME (fcitx5, ibus) would do, but it needs engines, config and a keyboard layout,
// and it composes only for certain languages, which makes the test slow and non-deterministic.
//
// This is the other half of the protocol instead: an input method (zwp_input_method_v2) that, as soon as the
// compositor tells it a text field became active, sends a preedit string and then commits text. Exactly what
// a virtual keyboard (maliit, squeekboard) or a CJK engine does, minus the intelligence.
//
// If the app prints the preedit and the commit, the whole chain works:
//   app -> text-input-v3 -> compositor -> input-method-v2 -> here -> back to the app.
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <wayland-client.h>
#include "input-method-v2-client-protocol.h"

static struct zwp_input_method_manager_v2 *manager = NULL;
static struct wl_seat *seat = NULL;
static struct zwp_input_method_v2 *input_method = NULL;
static int active = 0;
static uint32_t serial = 0;

static void registry_global(void *data, struct wl_registry *registry, uint32_t name,
                            const char *interface, uint32_t version) {
    if (strcmp(interface, "zwp_input_method_manager_v2") == 0) {
        manager = wl_registry_bind(registry, name, &zwp_input_method_manager_v2_interface, 1);
    } else if (strcmp(interface, "wl_seat") == 0) {
        seat = wl_registry_bind(registry, name, &wl_seat_interface, version < 7 ? version : 7);
    }
}
static void registry_global_remove(void *data, struct wl_registry *r, uint32_t name) {}
static const struct wl_registry_listener registry_listener = { registry_global, registry_global_remove };

// The compositor tells us a text field is now taking input.
static void im_activate(void *data, struct zwp_input_method_v2 *im) {
    active = 1;
    fprintf(stderr, "fake-ime: activate (a text field is asking for input)\n");
}
static void im_deactivate(void *data, struct zwp_input_method_v2 *im) { active = 0; }
static void im_surrounding_text(void *data, struct zwp_input_method_v2 *im,
                                const char *text, uint32_t cursor, uint32_t anchor) {}
static void im_text_change_cause(void *data, struct zwp_input_method_v2 *im, uint32_t cause) {}
static void im_content_type(void *data, struct zwp_input_method_v2 *im, uint32_t hint, uint32_t purpose) {}

// The serial passed to commit() must be the number of `done` events received, not a counter of our own.
// Getting this wrong makes the compositor drop the request silently: the first version of this file sent
// the preedit fine and then lost the commit_string, because its second commit carried a serial the
// compositor had never issued.
static int step = 0;

static void im_done(void *data, struct zwp_input_method_v2 *im) {
    serial++;              // one `done` received
    if (!active) return;

    if (step >= 3) return;   // a few rounds is plenty; do not spam
    step++;

    // Wait before answering. Firing straight from `activate` is too early: the compositor has not yet given
    // the app's surface text focus (its text-input `enter` has not arrived), so it drops everything we send.
    // Compose also tears the session down and re-establishes it right after focus, so the first activation
    // is not the one that sticks. Answering EVERY activation (not just the first) is what makes this land.
    // A real IME never hits any of this, because a human takes time to type.
    sleep(2);

    // Both messages go out on THIS done, not one per done. The compositor only emits `done` when the text
    // field's state changes, and the app calls enable() once, so waiting for a second `done` waits forever
    // (that is why an earlier version sent the preedit and never the commit). A real IME gets a `done` per
    // keystroke; this one has no keystrokes to react to.
    zwp_input_method_v2_set_preedit_string(im, "compo", 5, 5);
    zwp_input_method_v2_commit(im, serial);
    fprintf(stderr, "fake-ime: sent preedit_string 'compo'\n");

    // The committed text: THIS is what a real IME drops into the text field when composition ends.
    zwp_input_method_v2_commit_string(im, "IME-OK");
    zwp_input_method_v2_commit(im, serial);
    fprintf(stderr, "fake-ime: sent commit_string 'IME-OK'\n");

    active = 0;   // wait for the next activate
}
static void im_unavailable(void *data, struct zwp_input_method_v2 *im) {
    fprintf(stderr, "fake-ime: another input method already owns the seat\n");
}

static const struct zwp_input_method_v2_listener im_listener = {
    im_activate, im_deactivate, im_surrounding_text, im_text_change_cause,
    im_content_type, im_done, im_unavailable,
};

int main(void) {
    struct wl_display *display = wl_display_connect(NULL);
    if (!display) { fprintf(stderr, "fake-ime: cannot connect to the compositor\n"); return 1; }

    struct wl_registry *registry = wl_display_get_registry(display);
    wl_registry_add_listener(registry, &registry_listener, NULL);
    wl_display_roundtrip(display);
    wl_display_roundtrip(display);

    if (!manager || !seat) {
        fprintf(stderr, "fake-ime: compositor has no zwp_input_method_manager_v2 (manager=%p seat=%p)\n",
                (void *)manager, (void *)seat);
        return 1;
    }
    input_method = zwp_input_method_manager_v2_get_input_method(manager, seat);
    zwp_input_method_v2_add_listener(input_method, &im_listener, NULL);
    fprintf(stderr, "fake-ime: registered as the input method for this seat\n");

    while (wl_display_dispatch(display) != -1) { }
    return 0;
}
