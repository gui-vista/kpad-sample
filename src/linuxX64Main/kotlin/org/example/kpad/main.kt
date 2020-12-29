package org.example.kpad

import gio2.GApplication
import glib2.gpointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import org.guiVista.core.fetchEmptyDataPointer
import org.guiVista.gui.GuiApplication
import org.guiVista.gui.window.AppWindow

fun main() {
    GuiApplication(id = "org.example.basicgui").use {
        Controller.appWin = AppWindow(this)
        connectActivateSignal(staticCFunction(::activateApplication), fetchEmptyDataPointer())
        println("Application Status: ${run()}")
    }
}

@Suppress("UNUSED_PARAMETER")
private fun activateApplication(app: CPointer<GApplication>, userData: gpointer) {
    println("Application ID: ${GuiApplication(appPtr = app).appId}")
    Controller.appWin.createUi {
        changeDefaultSize(width = 600, height = 400)
        title = "KPad"
        visible = true
    }
}


//fun main() {
//    val app = gtk_application_new("org.example.kpad", G_APPLICATION_FLAGS_NONE)!!
//    Controller.connectGtkSignal(
//        obj = app,
//        actionName = "activate",
//        action = staticCFunction(::activate),
//        data = Controller.mainWin.stableRef.asCPointer()
//    )
//    val status = g_application_run(app.reinterpret(), 0, null)
//    g_object_unref(app)
//    g_print("Application Status: %d", status)
//}
//
//fun activate(app: CPointer<GtkApplication>, userData: gpointer) {
//    val mainWin = userData.asStableRef<MainWindow>().get()
//    mainWin.winPtr = gtk_application_window_new(app)
//    val mainLayout = mainWin.createMainLayout()
//    mainWin.updateMainWindowTitle("KPad")
//    gtk_window_set_default_size(window = mainWin.winPtr?.reinterpret(), width = 600, height = 400)
//    gtk_container_add(mainWin.winPtr?.reinterpret(), mainLayout?.reinterpret())
//    gtk_widget_show_all(mainWin.winPtr)
//}
