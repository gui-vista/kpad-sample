@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package org.example.kpad

import glib2.*
import gtk3.*
import kotlinx.cinterop.*
import org.guiVista.gui.dialog.fileChooserDialog
import org.guiVista.gui.dialog.messageDialog
import org.guiVista.gui.keyboard.AcceleratorGroup
import org.guiVista.gui.text.TextBuffer
import org.guiVista.gui.text.TextBufferIterator
import org.guiVista.gui.window.WindowBase
import platform.posix.pthread_create
import platform.posix.pthread_t
import platform.posix.pthread_tVar
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

@Suppress("ObjectPropertyName")
private val _filePath = AtomicReference("")
@Suppress("ObjectPropertyName")
private val _txtBuffer = AtomicReference("")

@ThreadLocal
internal object Controller {
    internal lateinit var mainWin: MainWindow
    val filePath: String
        get() = _filePath.value
    var txtBuffer: String
        get() = _txtBuffer.value
        set(value) {
            _txtBuffer.value = value
        }

    fun textFromTextBuffer(buffer: TextBuffer): String {
        val start = TextBufferIterator()
        val end = TextBufferIterator()
        buffer.fetchStartIterator(start)
        buffer.fetchEndIterator(end)
        return buffer.fetchText(start = start, end = end, includeHiddenChars = false)
    }

    fun showAboutDialog() {
        val msg = "KPad - A desktop application written using Kotlin & GUI Vista."
        val dialog = messageDialog(parent = mainWin, type = GtkMessageType.GTK_MESSAGE_INFO, messageFormat = msg)
        dialog.run()
        dialog.close()
    }

    fun showOpenDialog(parent: WindowBase) {
        val dialog = createOpenDialog(parent)
        val resp = dialog.run()
        if (resp == GTK_RESPONSE_ACCEPT) {
            _filePath.value = dialog.fetchFileName().freeze()
            mainWin.updateStatusBar("Opening ${_filePath.value}...")
            runOnBackgroundThread(staticCFunction(::runOpenFileTask))
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
            _filePath.value = dialog.fetchFileName().freeze()
            mainWin.updateStatusBar("Saving ${_filePath.value}...")
            _txtBuffer.value = textFromTextBuffer(buffer).freeze()
            runOnBackgroundThread(staticCFunction(::runSaveFileTask))
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

    fun runOnBackgroundThread(
        func: CPointer<CFunction<(COpaquePointer?) -> COpaquePointer?>>,
        userData: COpaquePointer? = null
    ): pthread_t {
        val thread = memScoped { alloc<pthread_tVar>() }
        pthread_create(thread.ptr, null, func, userData)
        return thread.value
    }

    fun runOnUiThread(func: CPointer<CFunction<(COpaquePointer?) -> Int>>, userData: COpaquePointer? = null) {
        gdk_threads_add_idle(func, userData)
    }

    fun newFile() {
        with(mainWin) {
            title = "KPad"
            buffer.changeText("")
            _filePath.value = ""
            updateStatusBar("Ready")
            resetFocus()
        }
    }

    fun setupMainWindowEvents() {
        val accelGroup = AcceleratorGroup().apply {
            registerKeyboardShortcut(GDK_CONTROL_MASK to 'o', staticCFunction(::openFileKeyPressed))
            registerKeyboardShortcut(GDK_CONTROL_MASK to 's', staticCFunction(::saveFileKeyPressed))
            registerKeyboardShortcut(GDK_CONTROL_MASK to 'n', staticCFunction(::newFileKeyPressed))
        }
        mainWin.addAccelGroup(accelGroup)
    }

    fun saveFile() {
        val filePath = Controller.filePath
        if (filePath.isEmpty()) {
            showSaveDialog(mainWin, mainWin.buffer)
        } else {
            mainWin.updateStatusBar("Saving $filePath...")
            txtBuffer = textFromTextBuffer(mainWin.buffer).freeze()
            runOnBackgroundThread(staticCFunction(::runSaveFileTask))
        }
    }

    fun openFile() {
        showOpenDialog(mainWin)
    }
}

internal fun AcceleratorGroup.registerKeyboardShortcut(
    shortcut: Pair<UInt, Char>,
    eventHandler: CPointer<CFunction<(COpaquePointer) -> Unit>>
) {
    gtk_accel_group_connect(
        accel_group = gtkAcceleratorGroupPtr,
        accel_mods = shortcut.first,
        accel_key = shortcut.second.toInt().toUInt(),
        accel_flags = 0u,
        closure = g_cclosure_new(eventHandler.reinterpret(), null, null)
    )
}

private fun runOpenFileTask(@Suppress("UNUSED_PARAMETER") userData: COpaquePointer?): COpaquePointer? {
    initRuntimeIfNeeded()
    _txtBuffer.value = readTextFile(_filePath.value).freeze()
    Controller.runOnUiThread(staticCFunction { _: COpaquePointer? ->
        Controller.mainWin.buffer.changeText(_txtBuffer.value)
        Controller.mainWin.title = "KPad - ${fileName(_filePath.value)}"
        Controller.mainWin.updateStatusBar("File opened")
        Controller.mainWin.resetFocus()
        FALSE
    })
    return null
}

private fun runSaveFileTask(@Suppress("UNUSED_PARAMETER") userData: COpaquePointer?): COpaquePointer? {
    initRuntimeIfNeeded()
    writeTextToFile(_filePath.value, _txtBuffer.value)
    Controller.runOnUiThread(staticCFunction { _: COpaquePointer? ->
        Controller.mainWin.title = "KPad - ${fileName(_filePath.value)}"
        Controller.mainWin.updateStatusBar("File saved")
        FALSE
    })
    return null
}

private fun openFileKeyPressed(@Suppress("UNUSED_PARAMETER") ptr: COpaquePointer) {
    initRuntimeIfNeeded()
    Controller.showOpenDialog(Controller.mainWin)
}

private fun saveFileKeyPressed(@Suppress("UNUSED_PARAMETER") ptr: COpaquePointer) {
    initRuntimeIfNeeded()
    val filePath = Controller.filePath
    if (filePath.isEmpty()) {
        Controller.showSaveDialog(Controller.mainWin, Controller.mainWin.buffer)
    } else {
        Controller.mainWin.updateStatusBar("Saving $filePath...")
        Controller.txtBuffer = Controller.textFromTextBuffer(Controller.mainWin.buffer).freeze()
        Controller.runOnBackgroundThread(staticCFunction(::runSaveFileTask))
    }
}

private fun newFileKeyPressed(@Suppress("UNUSED_PARAMETER") ptr: COpaquePointer) {
    initRuntimeIfNeeded()
    Controller.newFile()
}
