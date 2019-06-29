@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import gtk3.*
import kotlinx.cinterop.*

internal class MainWindow {
    val stableRef = StableRef.create(this)
    private var _editor: CPointer<GtkWidget>? = null
    val editor
        get() = _editor
    private var statusBar: CPointer<GtkWidget>? = null
    var winPtr: CPointer<GtkWidget>? = null

    fun updateMainWindowTitle(title: String) {
        gtk_window_set_title(winPtr?.reinterpret(), title)
    }

    fun updateStatusBar(txt: String) {
        gtk_statusbar_push(statusbar = statusBar?.reinterpret(), text = txt, context_id = 0u)
    }

    fun updateEditor(filePath: String) {
        val buffer = gtk_text_view_get_buffer(_editor?.reinterpret())
        gtk_text_buffer_set_text(buffer = buffer, text = readTextFile(filePath), len = fileSize(filePath).toInt())
        gtk_widget_grab_focus(_editor)
    }

    fun createMainLayout(): CPointer<GtkContainer>? {
        val mainLayout = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)
        val toolbar = createToolbar()
        val scrolledWin = createScrolledWindow()
        createStatusBar()

        gtk_container_add(mainLayout?.reinterpret(), toolbar?.reinterpret())
        Controller.addWidgetToBoxFromTop(box = mainLayout?.reinterpret(), widget = scrolledWin?.reinterpret(),
            expand = true)
        Controller.addWidgetToBoxFromBottom(box = mainLayout?.reinterpret(), widget = statusBar?.reinterpret())
        gtk_widget_grab_focus(_editor?.reinterpret())
        return mainLayout?.reinterpret()
    }

    private fun createScrolledWindow(): CPointer<GtkScrolledWindow>? {
        val scrolledWin = gtk_scrolled_window_new(null, null)
        val margins = mapOf("left" to 10, "right" to 10, "top" to 10)
        _editor = gtk_text_view_new()
        gtk_widget_set_margin_left(scrolledWin, margins["left"] ?: 0)
        gtk_widget_set_margin_right(scrolledWin, margins["right"] ?: 0)
        gtk_widget_set_margin_top(scrolledWin, margins["top"] ?: 0)
        gtk_text_view_set_cursor_visible(_editor?.reinterpret(), TRUE)
        gtk_container_add(scrolledWin?.reinterpret(), _editor)
        return scrolledWin?.reinterpret()
    }

    private fun createStatusBar() {
        statusBar = gtk_statusbar_new()
        gtk_statusbar_push(statusbar = statusBar?.reinterpret(), text = "Ready", context_id = 0u)
    }

    private fun createToolbar(): CPointer<GtkToolbar>? {
        val toolbar = gtk_toolbar_new()
        val newBtn = gtk_button_new_with_label("New")
        val openBtn = gtk_button_new_with_label("Open")
        val saveBtn = gtk_button_new_with_label("Save")
        val newItem = gtk_tool_item_new()
        val openItem = gtk_tool_item_new()
        val saveItem = gtk_tool_item_new()
        val toolItems = arrayOf(newItem, openItem, saveItem)

        if (openBtn != null && newBtn != null && saveBtn != null) {
            setupEvents(openBtn, newBtn, saveBtn)
            gtk_container_add(newItem?.reinterpret(), newBtn.reinterpret())
            gtk_container_add(openItem?.reinterpret(), openBtn.reinterpret())
            gtk_container_add(saveItem?.reinterpret(), saveBtn.reinterpret())
        }
        toolItems.forEachIndexed { pos, ti ->
            gtk_toolbar_insert(toolbar?.reinterpret(), ti?.reinterpret(), pos)
        }
        return toolbar?.reinterpret()
    }

    private fun setupEvents(openBtn: CPointer<GtkWidget>, newBtn: CPointer<GtkWidget>, saveBtn: CPointer<GtkWidget>) {
        Controller.connectGtkSignal(obj = openBtn, actionName = "clicked", data = stableRef.asCPointer(),
            action = staticCFunction(::openBtnClicked))
        Controller.connectGtkSignal(obj = newBtn, actionName = "clicked", data = stableRef.asCPointer(),
            action = staticCFunction(::newBtnClicked))
        Controller.connectGtkSignal(obj = saveBtn, actionName = "clicked", action = staticCFunction(::saveBtnClicked),
            data = stableRef.asCPointer())
    }
}

@Suppress("UNUSED_PARAMETER")
private fun saveBtnClicked(widget: CPointer<GtkWidget>, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    val filePath = Controller.fetchFilePath()
    val buffer = gtk_text_view_get_buffer(mainWin.editor?.reinterpret())
    if (filePath.isEmpty()) {
        Controller.showSaveDialog(mainWin.winPtr?.reinterpret(), buffer)
    } else {
        mainWin.updateStatusBar("Saving $filePath...")
        saveFile(filePath, Controller.textFromTextBuffer(buffer))
        mainWin.updateStatusBar("File saved")
        gtk_widget_grab_focus(mainWin.editor)
    }
}

private fun openBtnClicked(@Suppress("UNUSED_PARAMETER") widget: CPointer<GtkWidget>, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    Controller.showOpenDialog(mainWin.winPtr?.reinterpret())
}

@Suppress("UNUSED_PARAMETER")
fun newBtnClicked(widget: CPointer<GtkWidget>, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    mainWin.updateMainWindowTitle("KPad")
    val buffer = gtk_text_view_get_buffer(mainWin.editor?.reinterpret())
    gtk_text_buffer_set_text(buffer = buffer, text = "", len = 0)
    Controller.clearFilePath()
    mainWin.updateStatusBar("Ready")
    gtk_widget_grab_focus(mainWin.editor)
}
