package org.mywill.client.ui

import java.awt.FileDialog
import java.awt.Frame

/**
 * Реализация выбора файлов для JVM (Desktop).
 */
actual fun openFilePicker(onFilesSelected: (List<SelectedFile>) -> Unit) {
    val dialog = FileDialog(null as Frame?, "Выберите файлы", FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    val files = dialog.files
    if (files != null && files.isNotEmpty()) {
        val selectedFiles = files.map { file ->
            SelectedFile(file.name, file.readBytes())
        }
        onFilesSelected(selectedFiles)
    }
}

/**
 * Реализация скачивания файла для JVM.
 */
actual fun downloadFile(url: String, fileName: String) {
    // В десктопной версии можно открыть браузер или скачать программно.
    // Пока просто выведем в лог, так как URL может быть локальным API.
    println("[DEBUG_LOG] Download requested for $url as $fileName")
}
