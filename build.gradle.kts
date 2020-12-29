plugins {
    kotlin("multiplatform") version "1.4.21"
}
repositories {
    mavenCentral()
}

kotlin {
    linuxX64 {
        compilations.getByName("main") {
            val headerDir = "/usr/include"
            cinterops.create("glib2") {
                includeDirs("/usr/lib/x86_64-linux-gnu/glib-2.0/include", "$headerDir/glib-2.0",
                    "$headerDir/gdk-pixbuf-2.0")
            }
            cinterops.create("gio2")
            cinterops.create("gtk3") {
                includeDirs(
                    "/usr/include/harfbuzz",
                    "/usr/lib/x86_64-linux-gnu/glib-2.0/include",
                    "$headerDir/glib-2.0",
                    "$headerDir/atk-1.0",
                    "$headerDir/cairo",
                    "$headerDir/pango-1.0",
                    "$headerDir/gtk-3.0"
                )
            }
        }
        binaries {
            executable {
                entryPoint = "org.example.kpad.main"
            }
        }
    }
}
