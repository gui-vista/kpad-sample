@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
package org.example.kpad

import glib2.*
import gtk3.*
import kotlinx.cinterop.*

@ThreadLocal
internal object Controller {
    private var filePath = ""
    internal lateinit var mainWin: MainWindow

    fun clearFilePath() {
        filePath = ""
    }

    fun fetchFilePath() = filePath

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
                title = "KPad - ${fileName(filePath)}"
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
        variadicArguments = arrayOf(GTK_RESPONSE_CANCEL, "gtk-open", GTK_RESPONSE_ACCEPT, null)
    )

    fun showSaveDialog(parent: CPointer<GtkWindow>?, buffer: CPointer<GtkTextBuffer>?) {
        val dialog = createSaveDialog(parent)
        val resp = gtk_dialog_run(dialog?.reinterpret())
        if (resp == GTK_RESPONSE_ACCEPT) {
            filePath = gtk_file_chooser_get_filename(dialog?.reinterpret())?.toKString() ?: ""
            with(mainWin) {
                updateStatusBar("Saving $filePath...")
                title = "KPad - ${fileName(filePath)}"
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
        variadicArguments = arrayOf(GTK_RESPONSE_CANCEL, "gtk-save", GTK_RESPONSE_ACCEPT, null)
    )
}
