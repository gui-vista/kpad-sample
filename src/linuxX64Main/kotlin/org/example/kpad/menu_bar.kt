package org.example.kpad

import glib2.gpointer
import gtk3.GtkMenuItem
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import org.guiVista.core.fetchEmptyDataPointer
import org.guiVista.gui.widget.menu.item.menuItem
import org.guiVista.gui.widget.menu.menuBarWidget
import org.guiVista.gui.widget.menu.menuWidget
import kotlin.system.exitProcess

private val newItem by lazy {
    menuItem {
        label = "New"
        connectActivateSignal(staticCFunction(::newItemActivated), fetchEmptyDataPointer())
    }
}
private val openItem by lazy {
    menuItem {
        label = "Open"
        connectActivateSignal(staticCFunction(::openItemActivated), fetchEmptyDataPointer())
    }
}
private val saveItem by lazy {
    menuItem {
        label = "Save"
        connectActivateSignal(staticCFunction(::saveItemActivated), fetchEmptyDataPointer())
    }
}
private val quitItem by lazy {
    menuItem {
        label = "Quit"
        connectActivateSignal(staticCFunction(::quitItemActivated), fetchEmptyDataPointer())
    }
}
private val aboutItem by lazy {
    menuItem {
        label = "About"
        connectActivateSignal(staticCFunction(::aboutItemActivated), fetchEmptyDataPointer())
    }
}

private fun createFileMenu() = menuWidget {
    add(newItem)
    add(openItem)
    add(saveItem)
    add(quitItem)
}

private fun createHelpMenu() = menuWidget { add(aboutItem) }

internal fun createMenuBar() = menuBarWidget {
    val fileItem = menuItem {
        label = "File"
        subMenu = createFileMenu()
    }
    val helpItem = menuItem {
        label = "Help"
        subMenu = createHelpMenu()
    }
    append(fileItem)
    append(helpItem)
}

@Suppress("UNUSED_PARAMETER")
private fun aboutItemActivated(menuItem: CPointer<GtkMenuItem>, userData: gpointer) {
    Controller.showAboutDialog()
}


@Suppress("UNUSED_PARAMETER")
private fun quitItemActivated(menuItem: CPointer<GtkMenuItem>, userData: gpointer) {
    exitProcess(0)
}

@Suppress("UNUSED_PARAMETER")
private fun newItemActivated(menuItem: CPointer<GtkMenuItem>, userData: gpointer) {
    Controller.newFile()
}

@Suppress("UNUSED_PARAMETER")
private fun openItemActivated(menuItem: CPointer<GtkMenuItem>, userData: gpointer) {
    Controller.openFile()
}

@Suppress("UNUSED_PARAMETER")
private fun saveItemActivated(menuItem: CPointer<GtkMenuItem>, userData: gpointer) {
    Controller.saveFile()
}
