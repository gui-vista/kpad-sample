@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package org.example.kpad

import glib2.FALSE
import gtk3.GTK_RESPONSE_ACCEPT
import gtk3.GTK_RESPONSE_CANCEL
import gtk3.GtkFileChooserAction
import gtk3.gdk_threads_add_idle
import kotlinx.cinterop.*
import org.guiVista.gui.dialog.fileChooserDialog
import org.guiVista.gui.text.TextBuffer
import org.guiVista.gui.text.TextBufferIterator
import org.guiVista.gui.window.WindowBase
import platform.posix.pthread_create
import platform.posix.pthread_t
import platform.posix.pthread_tVar
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

private val filePath = AtomicReference("")
private val tempTxt = AtomicReference("")

@ThreadLocal
internal object Controller {
    internal lateinit var mainWin: MainWindow

    fun clearFilePath() {
        filePath.value = ""
    }

    fun fetchFilePath() = filePath.value

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
            filePath.value = dialog.fetchFileName().freeze()
            mainWin.updateStatusBar("Opening $filePath...")
            runOnBackgroundThread(staticCFunction(::openFile))
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
            filePath.value = dialog.fetchFileName()
            with(mainWin) {
                updateStatusBar("Saving $filePath...")
                title = "KPad - ${fileName(filePath.value)}"
                saveFile(filePath.value, textFromTextBuffer(buffer))
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

private fun openFile(@Suppress("UNUSED_PARAMETER") userData: COpaquePointer?): COpaquePointer? {
    initRuntimeIfNeeded()
    tempTxt.value = readTextFile(filePath.value).freeze()
    runOnUiThread(staticCFunction { _: COpaquePointer? ->
        Controller.mainWin.buffer.changeText(tempTxt.value)
        Controller.mainWin.title = "KPad - ${fileName(filePath.value)}"
        Controller.mainWin.updateStatusBar("File opened")
        Controller.mainWin.resetFocus()
        FALSE
    })
    return null
}

private fun runOnBackgroundThread(
    func: CPointer<CFunction<(COpaquePointer?) -> COpaquePointer?>>?,
    userData: COpaquePointer? = null
): pthread_t {
    val thread = memScoped { alloc<pthread_tVar>() }
    pthread_create(thread.ptr, null, func, userData)
    return thread.value
}

private fun runOnUiThread(func: CPointer<CFunction<(COpaquePointer?) -> Int>>?, userData: COpaquePointer? = null) {
    gdk_threads_add_idle(func, userData)
}
