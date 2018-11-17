@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package org.example.kpad

import gtk3.*
import kotlinx.cinterop.*
import platform.posix.*

internal fun createOpenFileDialog(parent: CPointer<GtkWindow>) {
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
    if (resp == GTK_RESPONSE_ACCEPT) {
        val filePath = gtk_file_chooser_get_filename(dialog?.reinterpret())?.toKString()!!
        updateStatusBar("Opening $filePath...")
        updateEditor(filePath)
        updateMainWindowTitle("KPad - ${fileName(filePath)}")
        updateStatusBar("Ready")
    }
    gtk_widget_destroy(dialog)
}

internal fun fileName(filePath: String): String = if ('/' in filePath && filePath.length >= 2) {
    filePath.slice((filePath.lastIndexOf("/") + 1)..(filePath.length - 1))
} else {
    ""
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

fun main() {
    val app = gtk_application_new("org.example.kpad", G_APPLICATION_FLAGS_NONE)!!
    connectGtkSignal(obj = app, actionName = "activate", action = staticCFunction(::activateMainWindow))
    val status = g_application_run(app.reinterpret(), 0, null)
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

internal fun readTextFile(filePath: String): String {
    val size = fileSize(filePath)
    var result = ""
    val file = fopen(filePath, "r")

    memScoped {
        val buffer = allocArray<ByteVar>(size)
        // Read the entire file and store the contents into the buffer.
        fread(buffer, size.toULong(), 1, file)
        result = buffer.toKString()
    }
    fclose(file)
    return result
}

internal fun fileSize(filePath: String): Long {
    val file = fopen(filePath, "r")
    fseek(file, 0, SEEK_END)
    return ftell(file)
}
