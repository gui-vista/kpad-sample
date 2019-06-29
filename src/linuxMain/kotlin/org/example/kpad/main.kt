package org.example.kpad

import gtk3.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction

fun main() {
    val app = gtk_application_new("org.example.kpad", G_APPLICATION_FLAGS_NONE)!!
    Controller.connectGtkSignal(
        obj = app,
        actionName = "activate",
        action = staticCFunction(::activate),
        data = Controller.mainWin.stableRef.asCPointer()
    )
    val status = g_application_run(app.reinterpret(), 0, null)
    g_object_unref(app)
    g_print("Application Status: %d", status)
}

fun activate(app: CPointer<GtkApplication>, userData: gpointer) {
    val mainWin = userData.asStableRef<MainWindow>().get()
    mainWin.winPtr = gtk_application_window_new(app)
    val mainLayout = mainWin.createMainLayout()
    mainWin.updateMainWindowTitle("KPad")
    gtk_window_set_default_size(window = mainWin.winPtr?.reinterpret(), width = 600, height = 400)
    gtk_container_add(mainWin.winPtr?.reinterpret(), mainLayout?.reinterpret())
    gtk_widget_show_all(mainWin.winPtr)
}
