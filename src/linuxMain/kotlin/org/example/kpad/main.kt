@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import gtk3.*
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction

private fun activate(app: CPointer<GtkApplication>, @Suppress("UNUSED_PARAMETER") userData: gpointer) {
    val win = gtk_application_window_new(app)
    gtk_window_set_title(win?.reinterpret(), "KPad")
    gtk_window_set_default_size(window = win?.reinterpret(), width = 600, height = 400)
    gtk_widget_show_all(win)
}

fun main() {
    val app = gtk_application_new("org.example.kpad", G_APPLICATION_FLAGS_NONE)
    if (app != null) connectGtkSignal(obj = app, actionName = "activate", action = staticCFunction(::activate))
    val status = g_application_run(app?.reinterpret(), 0, null)
    g_object_unref(app)
    g_print("Application Status: %d", status)
}

/**
 * Connects a signal (event) to a slot (event handler). Note that all callback parameters must be primitive types or
 * nullable C pointers.
 */
internal fun <F : CFunction<*>> connectGtkSignal(
    obj: CPointer<*>,
    actionName: String,
    action: CPointer<F>,
    data: gpointer? = null,
    connectFlags: GConnectFlags = 0u
) {
    g_signal_connect_data(
        instance = obj.reinterpret(),
        detailed_signal = actionName,
        c_handler = action.reinterpret(),
        data = data,
        destroy_data = null,
        connect_flags = connectFlags
    )
}