@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package org.example.kpad

import gtk3.*
import kotlinx.cinterop.*

private fun createOpenFileDialog(parent: CPointer<GtkWindow>) {
    val gtkStockCancel = "gtk-cancel"
    val gtkStockOpen = "gtk-open"
    val dialogArgs = arrayOf(GTK_RESPONSE_CANCEL, gtkStockOpen, GTK_RESPONSE_ACCEPT, null)
    val dialog = gtk_file_chooser_dialog_new(
        parent = parent,
        title = "Open File",
        first_button_text = gtkStockCancel,
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_OPEN,
        variadicArguments = *dialogArgs
    )
    val resp = gtk_dialog_run(dialog?.reinterpret())
    g_print("Response: %d\n", resp)
    if (resp == GTK_RESPONSE_ACCEPT) {
        // TODO: Open file and display its contents in the editor.
        g_print("File: %s\n\n", gtk_file_chooser_get_filename(dialog?.reinterpret())?.toKString())
    }
    gtk_widget_destroy(dialog)
}

private fun openBtnClicked(@Suppress("UNUSED_PARAMETER") widget: CPointer<GtkWidget>, userData: gpointer) {
    createOpenFileDialog(userData.reinterpret())
}

private fun activate(app: CPointer<GtkApplication>, @Suppress("UNUSED_PARAMETER") userData: gpointer) {
    val win = gtk_application_window_new(app)!!
    val mainLayout = createMainLayout(win.reinterpret())
    gtk_window_set_title(win.reinterpret(), "KPad")
    gtk_window_set_default_size(window = win.reinterpret(), width = 600, height = 400)
    gtk_container_add(win.reinterpret(), mainLayout.reinterpret())
    gtk_widget_show_all(win)
}

private fun createMainLayout(win: CPointer<GtkWindow>): CPointer<GtkContainer> {
    val mainLayout = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!
    val toolbar = createToolbar(win)
    val editor = createEditor()
    val statusBar = createStatusBar()

    gtk_container_add(mainLayout.reinterpret(), toolbar.reinterpret())
    addWidgetToBoxFromTop(box = mainLayout.reinterpret(), widget = editor.reinterpret(), expand = true)
    addWidgetToBoxFromBottom(box = mainLayout.reinterpret(), widget = statusBar.reinterpret())
    gtk_widget_grab_focus(editor.reinterpret())
    return mainLayout.reinterpret()
}

internal fun addWidgetToBoxFromTop(
    box: CPointer<GtkBox>,
    widget: CPointer<GtkWidget>,
    fill: Boolean = true,
    expand: Boolean = false,
    padding: UInt = 0u
): Unit = gtk_box_pack_start(
    box = box,
    child = widget,
    expand = if (expand) TRUE else FALSE,
    fill = if (fill) TRUE else FALSE,
    padding = padding
)

internal fun addWidgetToBoxFromBottom(
    box: CPointer<GtkBox>,
    widget: CPointer<GtkWidget>,
    fill: Boolean = true,
    expand: Boolean = false,
    padding: UInt = 0u
): Unit = gtk_box_pack_end(
    box = box,
    child = widget,
    expand = if (expand) TRUE else FALSE,
    fill = if (fill) TRUE else FALSE,
    padding = padding
)

private fun createEditor(): CPointer<GtkTextView> {
    val margins = mapOf("left" to 10, "right" to 10, "top" to 10)
    val editor = gtk_text_view_new()!!
    gtk_widget_set_margin_left(editor, margins["left"]!!)
    gtk_widget_set_margin_right(editor, margins["right"]!!)
    gtk_widget_set_margin_top(editor, margins["top"]!!)
    gtk_text_view_set_cursor_visible(editor.reinterpret(), TRUE)
    return editor.reinterpret()
}

private fun createStatusBar(): CPointer<GtkStatusbar> {
    val statusBar = gtk_statusbar_new()!!
    gtk_statusbar_push(statusbar = statusBar.reinterpret(), text = "Ready", context_id = 0)
    return statusBar.reinterpret()
}

private fun createToolbar(win: CPointer<GtkWindow>): CPointer<GtkToolbar> {
    val toolbar = gtk_toolbar_new()!!
    val newBtn = gtk_button_new_with_label("New")
    val openBtn = gtk_button_new_with_label("Open")
    val saveBtn = gtk_button_new_with_label("Save")
    val newItem = gtk_tool_item_new()
    val openItem = gtk_tool_item_new()
    val saveItem = gtk_tool_item_new()
    val toolItems = arrayOf(newItem, openItem, saveItem)

    if (openBtn != null) {
        connectGtkSignal(obj = openBtn, actionName = "clicked", data = win, action = staticCFunction(::openBtnClicked))
    }
    gtk_container_add(newItem?.reinterpret(), newBtn?.reinterpret())
    gtk_container_add(openItem?.reinterpret(), openBtn?.reinterpret())
    gtk_container_add(saveItem?.reinterpret(), saveBtn?.reinterpret())
    toolItems.forEachIndexed { pos, ti ->
        gtk_toolbar_insert(toolbar.reinterpret(), ti?.reinterpret(), pos)
    }
    return toolbar.reinterpret()
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