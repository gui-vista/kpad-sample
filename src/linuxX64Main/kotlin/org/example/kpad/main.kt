package org.example.kpad

import gio2.GApplication
import glib2.gpointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import org.guiVista.core.fetchEmptyDataPointer
import org.guiVista.gui.GuiApplication

fun main() {
    GuiApplication(id = "org.example.kpad").use {
        Controller.mainWin = MainWindow(this)
        connectActivateSignal(staticCFunction(::activateApplication), fetchEmptyDataPointer())
        println("Application Status: ${run()}")
    }
}

@Suppress("UNUSED_PARAMETER")
private fun activateApplication(app: CPointer<GApplication>, userData: gpointer) {
    println("Application ID: ${GuiApplication(appPtr = app).appId}")
    Controller.mainWin.createUi {
        changeDefaultSize(width = 600, height = 400)
        title = "KPad"
        visible = true
    }
}
