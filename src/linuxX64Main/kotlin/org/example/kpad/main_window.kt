@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import glib2.gpointer
import gtk3.*
import kotlinx.cinterop.*
import org.guiVista.gui.GuiApplication
import org.guiVista.gui.layout.Container
import org.guiVista.gui.layout.boxLayout
import org.guiVista.gui.widget.Widget
import org.guiVista.gui.widget.button.Button
import org.guiVista.gui.widget.button.buttonWidget
import org.guiVista.gui.widget.display.StatusBar
import org.guiVista.gui.widget.display.statusBarWidget
import org.guiVista.gui.widget.textEditor.TextView
import org.guiVista.gui.widget.textEditor.textViewWidget
import org.guiVista.gui.widget.tool.ToolBar
import org.guiVista.gui.widget.tool.item.toolItemWidget
import org.guiVista.gui.widget.tool.toolBarWidget
import org.guiVista.gui.window.AppWindow

internal class MainWindow(app: GuiApplication) : AppWindow(app) {
    val stableRef = StableRef.create(this)
    private lateinit var _editor: TextView
    val editor
        get() = _editor
    private lateinit var statusBar: StatusBar

    fun updateStatusBar(txt: String) {
        statusBar.push(0u, txt)
    }

    fun updateEditor(filePath: String) {
        _editor.buffer.changeText(readTextFile(filePath))
        _editor.grabFocus()
    }

    override fun createMainLayout(): Container {
        val toolBar = createToolbar()
        val scrolledWin = createScrolledWindow()
        createStatusBar()
        _editor.grabFocus()
        return boxLayout(orientation = GtkOrientation.GTK_ORIENTATION_VERTICAL) {
            spacing = 0
            add(toolBar)
            if (scrolledWin != null) add(Widget(widgetPtr = scrolledWin.reinterpret()))
            add(statusBar)
        }
    }

    private fun createScrolledWindow(): CPointer<GtkScrolledWindow>? {
        val scrolledWin = gtk_scrolled_window_new(null, null)
        val margins = mapOf("left" to 10, "right" to 10, "top" to 10)
        _editor = textViewWidget {
            marginStart = margins["left"] ?: 0
            marginEnd = margins["right"] ?: 0
            marginTop = margins["top"] ?: 0
            cursorVisible = true
        }
        gtk_container_add(scrolledWin?.reinterpret(), _editor.gtkWidgetPtr)
        return scrolledWin?.reinterpret()
    }

    private fun createStatusBar() {
        statusBar = statusBarWidget {
            push(0u, "Ready")
        }
    }

    private fun createToolbar(): ToolBar {
        val toolBar = toolBarWidget {  }
        val newBtn = buttonWidget { label = "New" }
        val openBtn = buttonWidget { label = "Open" }
        val saveBtn = buttonWidget { label = "Save" }
        val newItem = toolItemWidget { add(newBtn) }
        val openItem = toolItemWidget { add(openBtn) }
        val saveItem = toolItemWidget { add(saveBtn) }
        val toolItems = arrayOf(newItem, openItem, saveItem)

        setupEvents(openBtn, newBtn, saveBtn)
        toolItems.forEachIndexed { pos, ti -> toolBar.insert(ti, pos) }
        return toolBar
    }

    private fun setupEvents(openBtn: Button, newBtn: Button, saveBtn: Button) {
        openBtn.connectClickedSignal(staticCFunction(::openBtnClicked), stableRef.asCPointer())
        newBtn.connectClickedSignal(staticCFunction(::newBtnClicked), stableRef.asCPointer())
        saveBtn.connectClickedSignal(staticCFunction(::saveBtnClicked), stableRef.asCPointer())
    }
}

@Suppress("UNUSED_PARAMETER")
private fun saveBtnClicked(widget: CPointer<GtkButton>?, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    val filePath = Controller.fetchFilePath()
    val buffer = mainWin.editor.buffer
    if (filePath.isEmpty()) {
        Controller.showSaveDialog(mainWin.gtkWindowPtr, buffer.gtkTextBufferPtr)
    } else {
        mainWin.updateStatusBar("Saving $filePath...")
        saveFile(filePath, Controller.textFromTextBuffer(buffer.gtkTextBufferPtr))
        mainWin.updateStatusBar("File saved")
        mainWin.editor.grabFocus()
    }
}

private fun openBtnClicked(@Suppress("UNUSED_PARAMETER") widget: CPointer<GtkButton>?, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    Controller.showOpenDialog(mainWin.gtkWindowPtr)
}

@Suppress("UNUSED_PARAMETER")
fun newBtnClicked(widget: CPointer<GtkButton>?, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    with(mainWin) {
        title = "KPad"
        editor.buffer.changeText("")
        Controller.clearFilePath()
        updateStatusBar("Ready")
        editor.grabFocus()
    }
}
