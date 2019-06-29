@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package org.example.kpad

import gtk3.GError
import gtk3.g_file_set_contents
import kotlinx.cinterop.*
import platform.posix.*

fun saveFile(filePath: String, txt: String): String = memScoped {
    val error = alloc<CPointerVar<GError>>()
    g_file_set_contents(filename = filePath, contents = txt, length = txt.length.toLong(), error = error.ptr)
    return error.pointed?.message?.toKString() ?: ""
}

fun fileName(filePath: String): String = if ('/' in filePath && filePath.length >= 2) {
    @Suppress("ReplaceRangeToWithUntil")
    filePath.slice((filePath.lastIndexOf("/") + 1)..(filePath.length - 1))
} else {
    ""
}

fun readTextFile(filePath: String): String {
    val size = fileSize(filePath)
    var result = ""
    val readMode = "r"
    val file = fopen(filePath, readMode)

    memScoped {
        val buffer = allocArray<ByteVar>(size)
        // Read the entire file and store the contents into the buffer.
        fread(buffer, size.toULong(), 1u, file)
        result = buffer.toKString()
    }
    fclose(file)
    return result
}

fun fileSize(filePath: String): Long {
    val readMode = "r"
    val file = fopen(filePath, readMode)
    fseek(file, 0, SEEK_END)
    return ftell(file)
}
