@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import glib2.FALSE
import glib2.gpointer
import gtk3.GtkOrientation
import gtk3.GtkPolicyType
import gtk3.GtkToolButton
import kotlinx.cinterop.*
import org.guiVista.gui.GuiApplication
import org.guiVista.gui.layout.Container
import org.guiVista.gui.layout.boxLayout
import org.guiVista.gui.widget.display.statusBarWidget
import org.guiVista.gui.widget.textEditor.textViewWidget
import org.guiVista.gui.widget.tool.ToolBar
import org.guiVista.gui.widget.tool.item.ToolButton
import org.guiVista.gui.widget.tool.item.toolButtonWidget
import org.guiVista.gui.widget.tool.toolBarWidget
import org.guiVista.gui.window.AppWindow
import org.guiVista.gui.window.ScrolledWindow
import kotlin.native.concurrent.freeze

internal class MainWindow(app: GuiApplication) : AppWindow(app) {
    private val editor by lazy { createEditor() }
    val buffer
        get() = editor.buffer
    private val statusBar by lazy { createStatusBar() }
    private val toolBar by lazy { createToolbar() }
    val stableRef = StableRef.create(this)

    override fun resetFocus() {
        editor.grabFocus()
    }

    private fun createEditor() = textViewWidget {
        val margins = mapOf("start" to 10, "end" to 10, "top" to 10)
        marginStart = margins["start"] ?: 0
        marginEnd = margins["end"] ?: 0
        marginTop = margins["top"] ?: 0
        cursorVisible = true
    }

    fun updateStatusBar(txt: String) {
        statusBar.push(0u, txt)
    }

    override fun createMainLayout(): Container = boxLayout(orientation = GtkOrientation.GTK_ORIENTATION_VERTICAL) {
        spacing = 5
        prependChild(toolBar)
        prependChild(child = createScrolledWindow(), fill = true, expand = true)
        appendChild(statusBar)
    }

    private fun createScrolledWindow() = ScrolledWindow().apply {
        // Using add instead of addChild to avoid the, "Attempting to add a widget with type GtkBox to a
        // GtkApplicationWindow, but as a GtkBin subclass a GtkApplicationWindow can only contain one widget at a
        // time" warning.
        add(editor)
        // Disable the infamous Overlay scrollbars.
        overlayScrolling = false
        // Always show vertical, and horizontal scrollbars.
        changePolicy(GtkPolicyType.GTK_POLICY_ALWAYS, GtkPolicyType.GTK_POLICY_ALWAYS)
    }

    private fun createStatusBar() = statusBarWidget { push(0u, "Ready") }

    private fun createToolbar(): ToolBar {
        val toolBar = toolBarWidget {}
        val newItem = toolButtonWidget(label = "New", iconWidget = null) {}
        val openItem = toolButtonWidget(label = "Open", iconWidget = null) {}
        val saveItem = toolButtonWidget(label = "Save", iconWidget = null) {}
        val toolItems = arrayOf(newItem, openItem, saveItem)

        setupToolButtonEvents(openItem, newItem, saveItem)
        toolItems.forEach { toolBar.insert(it, -1) }
        return toolBar
    }

    private fun setupToolButtonEvents(openItem: ToolButton, newItem: ToolButton, saveItem: ToolButton) {
        openItem.connectClickedSignal(staticCFunction(::openItemClicked), stableRef.asCPointer())
        newItem.connectClickedSignal(staticCFunction(::newItemClicked), stableRef.asCPointer())
        saveItem.connectClickedSignal(staticCFunction(::saveItemClicked), stableRef.asCPointer())
    }
}

private fun saveFile(@Suppress("UNUSED_PARAMETER") userData: COpaquePointer?): COpaquePointer? {
    initRuntimeIfNeeded()
    writeTextToFile(Controller.fetchFilePath(), Controller.fetchTxtBuffer())
    Controller.runOnUiThread(staticCFunction{ _: COpaquePointer? ->
        Controller.mainWin.updateStatusBar("File saved")
        Controller.mainWin.resetFocus()
        FALSE
    })
    return null
}

private fun saveItemClicked(
    @Suppress("UNUSED_PARAMETER") toolBtn: CPointer<GtkToolButton>?,
    userData: gpointer
) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    val filePath = Controller.fetchFilePath()
    if (filePath.isEmpty()) {
        Controller.showSaveDialog(mainWin, mainWin.buffer)
    } else {
        mainWin.updateStatusBar("Saving $filePath...")
        Controller.changeTxtBuffer(Controller.textFromTextBuffer(mainWin.buffer).freeze())
        Controller.runOnBackgroundThread(staticCFunction(::saveFile))
    }
}

private fun openItemClicked(
    @Suppress("UNUSED_PARAMETER") toolBtn: CPointer<GtkToolButton>?,
    userData: gpointer
) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    Controller.showOpenDialog(mainWin)
}

fun newItemClicked(
    @Suppress("UNUSED_PARAMETER") toolBtn: CPointer<GtkToolButton>?,
    userData: gpointer
) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    with(mainWin) {
        title = "KPad"
        buffer.changeText("")
        Controller.clearFilePath()
        updateStatusBar("Ready")
        resetFocus()
    }
}
