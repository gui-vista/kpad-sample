@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
package org.example.kpad

import gtk3.GTK_RESPONSE_ACCEPT
import gtk3.GTK_RESPONSE_CANCEL
import gtk3.GtkFileChooserAction
import org.guiVista.gui.dialog.fileChooserDialog
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
        return buffer.fetchText(start = start, end = end, includeHiddenChars = false)
    }

    fun showOpenDialog(parent: WindowBase) {
        val dialog = createOpenDialog(parent)
        val resp = dialog.run()
        if (resp == GTK_RESPONSE_ACCEPT) {
            filePath = dialog.fetchFileName()
            with(mainWin) {
                updateStatusBar("Opening $filePath...")
                updateEditor(filePath)
                title = "KPad - ${fileName(filePath)}"
                updateStatusBar("File opened")
            }
        }
        dialog.close()
    }

    private fun createOpenDialog(parent: WindowBase) = fileChooserDialog(
        parent = parent,
        title = "Open File",
        firstButtonText = "gtk-cancel",
        firstButtonResponseId = GTK_RESPONSE_CANCEL,
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_OPEN
    ) {
        addButton("gtk-open", GTK_RESPONSE_ACCEPT)
    }

    fun showSaveDialog(parent: WindowBase, buffer: TextBuffer) {
        val dialog = createSaveDialog(parent)
        val resp = dialog.run()
        if (resp == GTK_RESPONSE_ACCEPT) {
            filePath = dialog.fetchFileName()
            with(mainWin) {
                updateStatusBar("Saving $filePath...")
                title = "KPad - ${fileName(filePath)}"
                saveFile(filePath, textFromTextBuffer(buffer))
                updateStatusBar("File saved")
            }
        }
        dialog.close()
    }

    private fun createSaveDialog(parent: WindowBase) = fileChooserDialog(
        parent = parent,
        title = "Save File",
        firstButtonText = "gtk-cancel",
        firstButtonResponseId = GTK_RESPONSE_CANCEL,
        action = GtkFileChooserAction.GTK_FILE_CHOOSER_ACTION_SAVE
    ) {
        addButton("gtk-save", GTK_RESPONSE_ACCEPT)
    }
}
