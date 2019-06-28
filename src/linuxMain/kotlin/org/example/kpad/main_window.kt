@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package org.example.kpad

import gtk3.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction

private var editor: CPointer<GtkWidget>? = null
private var statusBar: CPointer<GtkWidget>? = null
private var win: CPointer<GtkWidget>? = null

private fun openBtnClicked(@Suppress("UNUSED_PARAMETER") widget: CPointer<GtkWidget>, userData: gpointer) {
    createOpenFileDialog(userData.reinterpret())
}

@Suppress("UNUSED_PARAMETER")
private fun saveBtnClicked(widget: CPointer<GtkWidget>, userData: gpointer) {
    val filePath = fetchFilePath()
    val buffer = gtk_text_view_get_buffer(editor?.reinterpret())!!
    if (filePath.isEmpty()) {
        createSaveFileDialog(userData.reinterpret(), buffer)
    } else {
        updateStatusBar("Saving $filePath...")
        saveFile(filePath, textFromTextBuffer(buffer))
        updateStatusBar("File saved")
        gtk_widget_grab_focus(editor)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun newBtnClicked(widget: CPointer<GtkWidget>, userData: gpointer) {
    updateMainWindowTitle("KPad")
    val buffer = gtk_text_view_get_buffer(editor?.reinterpret())
    gtk_text_buffer_set_text(buffer = buffer, text = "", len = 0)
    clearFilePath()
    updateStatusBar("Ready")
    gtk_widget_grab_focus(editor)
}

internal fun updateMainWindowTitle(title: String) {
    gtk_window_set_title(win?.reinterpret(), title)
}

internal fun activateMainWindow(
    app: CPointer<GtkApplication>,
    @Suppress("UNUSED_PARAMETER") userData: gpointer
) {
    win = gtk_application_window_new(app)
    val mainLayout = createMainLayout()
    updateMainWindowTitle("KPad")
    gtk_window_set_default_size(window = win?.reinterpret(), width = 600, height = 400)
    gtk_container_add(win?.reinterpret(), mainLayout.reinterpret())
    gtk_widget_show_all(win)
}

internal fun updateStatusBar(txt: String) {
    gtk_statusbar_push(statusbar = statusBar?.reinterpret(), text = txt, context_id = 0u)
}

internal fun updateEditor(filePath: String) {
    val buffer = gtk_text_view_get_buffer(editor?.reinterpret())
    gtk_text_buffer_set_text(buffer = buffer, text = readTextFile(filePath), len = fileSize(filePath).toInt())
    gtk_widget_grab_focus(editor)
}

private fun createMainLayout(): CPointer<GtkContainer> {
    val mainLayout = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!
    val toolbar = createToolbar()
    val scrolledWin = createScrolledWindow()
    createStatusBar()

    gtk_container_add(mainLayout.reinterpret(), toolbar.reinterpret())
    addWidgetToBoxFromTop(box = mainLayout.reinterpret(), widget = scrolledWin.reinterpret(), expand = true)
    addWidgetToBoxFromBottom(box = mainLayout.reinterpret(), widget = statusBar!!.reinterpret())
    gtk_widget_grab_focus(editor?.reinterpret())
    return mainLayout.reinterpret()
}

private fun createScrolledWindow(): CPointer<GtkScrolledWindow> {
    val scrolledWin = gtk_scrolled_window_new(null, null)!!
    val margins = mapOf("left" to 10, "right" to 10, "top" to 10)
    editor = gtk_text_view_new()
    gtk_widget_set_margin_left(scrolledWin, margins["left"] ?: 0)
    gtk_widget_set_margin_right(scrolledWin, margins["right"] ?: 0)
    gtk_widget_set_margin_top(scrolledWin, margins["top"] ?: 0)
    gtk_text_view_set_cursor_visible(editor?.reinterpret(), TRUE)
    gtk_container_add(scrolledWin.reinterpret(), editor)
    return scrolledWin.reinterpret()
}

private fun createStatusBar() {
    statusBar = gtk_statusbar_new()
    gtk_statusbar_push(statusbar = statusBar?.reinterpret(), text = "Ready", context_id = 0u)
}

private fun createToolbar(): CPointer<GtkToolbar> {
    val toolbar = gtk_toolbar_new()!!
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
        gtk_toolbar_insert(toolbar.reinterpret(), ti?.reinterpret(), pos)
    }
    return toolbar.reinterpret()
}

private fun setupEvents(openBtn: CPointer<GtkWidget>, newBtn: CPointer<GtkWidget>, saveBtn: CPointer<GtkWidget>) {
    connectGtkSignal(obj = openBtn, actionName = "clicked", data = win, action = staticCFunction(::openBtnClicked))
    connectGtkSignal(obj = newBtn, actionName = "clicked", action = staticCFunction(::newBtnClicked))
    connectGtkSignal(obj = saveBtn, actionName = "clicked", action = staticCFunction(::saveBtnClicked), data = win)
}
