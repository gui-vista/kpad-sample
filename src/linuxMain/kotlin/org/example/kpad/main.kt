@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import gtk3.*
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction

private var editor: CPointer<GtkWidget>? = null

private fun activate(app: CPointer<GtkApplication>, @Suppress("UNUSED_PARAMETER") userData: gpointer) {
    val win = gtk_application_window_new(app)
    val mainLayout = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)
    gtk_window_set_title(win?.reinterpret(), "KPad")
    gtk_window_set_default_size(window = win?.reinterpret(), width = 600, height = 400)
    gtk_container_add(win?.reinterpret(), mainLayout)
    if (mainLayout != null) createToolbar(mainLayout.reinterpret())
    if (mainLayout != null) createEditor(mainLayout.reinterpret())
    if (mainLayout != null) createStatusBar(mainLayout.reinterpret())
    gtk_widget_grab_focus(editor)
    gtk_widget_show_all(win)
}

private fun createEditor(container: CPointer<GtkContainer>) {
    val margins = mapOf("left" to 10, "right" to 10, "top" to 10)
    editor = gtk_text_view_new()
    gtk_widget_set_margin_left(editor, margins["left"]!!)
    gtk_widget_set_margin_right(editor, margins["right"]!!)
    gtk_widget_set_margin_top(editor, margins["top"]!!)
    gtk_text_view_set_cursor_visible(editor?.reinterpret(), TRUE)
    gtk_box_pack_start(box = container.reinterpret(), child = editor, fill = TRUE, expand = TRUE, padding = 0)
}

private fun createStatusBar(container: CPointer<GtkContainer>) {
    val statusBar = gtk_statusbar_new()
    gtk_statusbar_push(statusbar = statusBar?.reinterpret(), text = "Ready", context_id = 0)
    gtk_box_pack_end(
        box = container.reinterpret(),
        child = statusBar?.reinterpret(),
        fill = TRUE,
        expand = FALSE,
        padding = 0
    )
}

private fun createToolbar(container: CPointer<GtkContainer>) {
    val toolbar = gtk_toolbar_new()
    val newBtn = gtk_button_new_with_label("New")
    val openBtn = gtk_button_new_with_label("Open")
    val saveBtn = gtk_button_new_with_label("Save")
    val newItem = gtk_tool_item_new()
    val openItem = gtk_tool_item_new()
    val saveItem = gtk_tool_item_new()
    val toolItems = arrayOf(newItem, openItem, saveItem)

    gtk_container_add(newItem?.reinterpret(), newBtn?.reinterpret())
    gtk_container_add(openItem?.reinterpret(), openBtn?.reinterpret())
    gtk_container_add(saveItem?.reinterpret(), saveBtn?.reinterpret())
    toolItems.forEachIndexed { pos, ti ->
        gtk_toolbar_insert(toolbar?.reinterpret(), ti?.reinterpret(), pos)
    }
    gtk_container_add(container, toolbar)
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