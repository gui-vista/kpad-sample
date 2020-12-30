@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import glib2.gpointer
import gtk3.*
import kotlinx.cinterop.*
import org.guiVista.gui.GuiApplication
import org.guiVista.gui.layout.Container
import org.guiVista.gui.layout.boxLayout
import org.guiVista.gui.widget.Widget
import org.guiVista.gui.widget.display.statusBarWidget
import org.guiVista.gui.widget.textEditor.textViewWidget
import org.guiVista.gui.widget.tool.ToolBar
import org.guiVista.gui.widget.tool.item.ToolButton
import org.guiVista.gui.widget.tool.item.toolButtonWidget
import org.guiVista.gui.widget.tool.toolBarWidget
import org.guiVista.gui.window.AppWindow

internal class MainWindow(app: GuiApplication) : AppWindow(app) {
    private val editor by lazy { createEditor() }
    private val statusBar by lazy { createStatusBar() }
    private val toolBar by lazy { createToolbar() }
    val textBufferPtr
        get() = editor.buffer.gtkTextBufferPtr
    val stableRef = StableRef.create(this)

    fun changeEditorText(txt: String) {
        editor.buffer.changeText(txt)
    }

    override fun resetFocus() {
        editor.grabFocus()
    }

    private fun createEditor() = textViewWidget {
        val margins = mapOf("left" to 10, "right" to 10, "top" to 10)
        marginStart = margins["left"] ?: 0
        marginEnd = margins["right"] ?: 0
        marginTop = margins["top"] ?: 0
        cursorVisible = true
    }

    fun updateStatusBar(txt: String) {
        statusBar.push(0u, txt)
    }

    fun updateEditor(filePath: String) {
        editor.buffer.changeText(readTextFile(filePath))
        resetFocus()
    }

    override fun createMainLayout(): Container = boxLayout(orientation = GtkOrientation.GTK_ORIENTATION_VERTICAL) {
        val scrolledWin = createScrolledWindow()
        spacing = 5
        prependChild(toolBar)
        if (scrolledWin != null) {
            prependChild(child = Widget(widgetPtr = scrolledWin.reinterpret()), fill = true, expand = true)
        }
        appendChild(statusBar)
    }

    private fun createScrolledWindow(): CPointer<GtkScrolledWindow>? {
        val scrolledWin = gtk_scrolled_window_new(null, null)
        gtk_container_add(scrolledWin?.reinterpret(), editor.gtkWidgetPtr)
        return scrolledWin?.reinterpret()
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

private fun saveItemClicked(
    @Suppress("UNUSED_PARAMETER") toolBtn: CPointer<GtkToolButton>?,
    userData: gpointer
) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    val filePath = Controller.fetchFilePath()
    if (filePath.isEmpty()) {
        Controller.showSaveDialog(mainWin.gtkWindowPtr, mainWin.textBufferPtr)
    } else {
        mainWin.updateStatusBar("Saving $filePath...")
        saveFile(filePath, Controller.textFromTextBuffer(mainWin.textBufferPtr))
        mainWin.updateStatusBar("File saved")
        mainWin.resetFocus()
    }
}

private fun openItemClicked(
    @Suppress("UNUSED_PARAMETER") toolBtn: CPointer<GtkToolButton>?,
    userData: gpointer
) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    Controller.showOpenDialog(mainWin.gtkWindowPtr)
}

fun newItemClicked(
    @Suppress("UNUSED_PARAMETER") toolBtn: CPointer<GtkToolButton>?,
    userData: gpointer
) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    with(mainWin) {
        title = "KPad"
        changeEditorText("")
        Controller.clearFilePath()
        updateStatusBar("Ready")
        resetFocus()
    }
}
