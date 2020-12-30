@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
package org.example.kpad

import glib2.*
import gtk3.*
import kotlinx.cinterop.*
import org.guiVista.gui.text.TextBuffer
import org.guiVista.gui.text.TextBufferIterator
import org.guiVista.gui.window.WindowBase

@ThreadLocal
internal object Controller {
    private var filePath = ""
    internal lateinit var mainWin: MainWindow

    fun clearFilePath() {
        filePath = ""
    }

    fun fetchFilePath() = filePath

    fun textFromTextBuffer(buffer: TextBuffer): String {
        val start = TextBufferIterator()
        val end = TextBufferIterator()
        buffer.fetchStartIterator(start)
        buffer.fetchEndIterator(end)
        return gtk_text_buffer_get_text(
            buffer = buffer.gtkTextBufferPtr,
            start = start.gtkTextIterPtr,
            end = end.gtkTextIterPtr,
            include_hidden_chars = FALSE
        )?.toKString() ?: ""
    }

    fun showOpenDialog(parent: WindowBase) {
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

    private fun createOpenDialog(parent: WindowBase) = gtk_file_chooser_dialog_new(
        parent = parent.gtkWindowPtr,
        title = "Open File",
        first_button_text = "gtk-cancel",
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_OPEN,
        variadicArguments = arrayOf(GTK_RESPONSE_CANCEL, "gtk-open", GTK_RESPONSE_ACCEPT, null)
    )

    fun showSaveDialog(parent: WindowBase, buffer: TextBuffer) {
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

    private fun createSaveDialog(parent: WindowBase) = gtk_file_chooser_dialog_new(
        parent = parent.gtkWindowPtr,
        title = "Save File",
        first_button_text = "gtk-cancel",
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_SAVE,
        variadicArguments = arrayOf(GTK_RESPONSE_CANCEL, "gtk-save", GTK_RESPONSE_ACCEPT, null)
    )
}
