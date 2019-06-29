@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
package org.example.kpad

import gtk3.*
import kotlinx.cinterop.*

@ThreadLocal
internal object Controller {
    private var filePath = ""
    val mainWin = MainWindow()

    fun clearFilePath() {
        filePath = ""
    }

    fun fetchFilePath() = filePath

    /**
     * Connects a signal (event) to a slot (event handler). Note that all callback parameters must be primitive types or
     * nullable C pointers.
     */
    fun <F : CFunction<*>> connectGtkSignal(
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

    fun addWidgetToBoxFromTop(
        box: CPointer<GtkBox>?,
        widget: CPointer<GtkWidget>?,
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

    fun addWidgetToBoxFromBottom(
        box: CPointer<GtkBox>?,
        widget: CPointer<GtkWidget>?,
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

    fun textFromTextBuffer(buffer: CPointer<GtkTextBuffer>?): String = memScoped {
        val start = alloc<GtkTextIter>().ptr
        val end = alloc<GtkTextIter>().ptr
        gtk_text_buffer_get_start_iter(buffer, start)
        gtk_text_buffer_get_end_iter(buffer, end)
        return gtk_text_buffer_get_text(
            buffer = buffer,
            start = start,
            end = end,
            include_hidden_chars = FALSE
        )?.toKString() ?: ""
    }

    fun showOpenDialog(parent: CPointer<GtkWindow>?) {
        val dialog = createOpenDialog(parent)
        val resp = gtk_dialog_run(dialog?.reinterpret())
        if (resp == GTK_RESPONSE_ACCEPT) {
            filePath = gtk_file_chooser_get_filename(dialog?.reinterpret())?.toKString() ?: ""
            with(mainWin) {
                updateStatusBar("Opening $filePath...")
                updateEditor(filePath)
                updateMainWindowTitle("KPad - ${fileName(filePath)}")
                updateStatusBar("File opened")
            }
        }
        gtk_widget_destroy(dialog)
    }

    private fun createOpenDialog(parent: CPointer<GtkWindow>?) = gtk_file_chooser_dialog_new(
        parent = parent,
        title = "Open File",
        first_button_text = "gtk-cancel",
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_OPEN,
        variadicArguments = *arrayOf(GTK_RESPONSE_CANCEL, "gtk-open", GTK_RESPONSE_ACCEPT, null)
    )

    fun showSaveDialog(parent: CPointer<GtkWindow>?, buffer: CPointer<GtkTextBuffer>?) {
        val dialog = createSaveDialog(parent)
        val resp = gtk_dialog_run(dialog?.reinterpret())
        if (resp == GTK_RESPONSE_ACCEPT) {
            filePath = gtk_file_chooser_get_filename(dialog?.reinterpret())?.toKString() ?: ""
            with(mainWin) {
                updateStatusBar("Saving $filePath...")
                updateMainWindowTitle("KPad - ${fileName(filePath)}")
                saveFile(filePath, textFromTextBuffer(buffer))
                updateStatusBar("File saved")
            }
        }
        gtk_widget_destroy(dialog)
    }

    private fun createSaveDialog(parent: CPointer<GtkWindow>?) = gtk_file_chooser_dialog_new(
        parent = parent,
        title = "Save File",
        first_button_text = "gtk-cancel",
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_SAVE,
        variadicArguments = *arrayOf(GTK_RESPONSE_CANCEL, "gtk-save", GTK_RESPONSE_ACCEPT, null)
    )
}
