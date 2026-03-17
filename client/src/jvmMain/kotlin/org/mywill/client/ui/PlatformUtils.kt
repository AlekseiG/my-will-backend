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
actual fun downloadFile(url: String, fileName: String, bytes: ByteArray?) {
    if (bytes != null) {
        // Если байты предоставлены, сохраняем файл через диалог
        val dialog = FileDialog(null as Frame?, "Сохранить файл", FileDialog.SAVE)
        dialog.file = fileName
        dialog.isVisible = true
        if (dialog.directory != null && dialog.file != null) {
            val file = java.io.File(dialog.directory, dialog.file)
            file.writeBytes(bytes)
            println("[DEBUG_LOG] File saved to ${file.absolutePath}")
        }
    } else {
        // Если только URL, открываем в браузере (может не работать без авторизации)
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        } else {
            println("[DEBUG_LOG] Open URL requested for $url as $fileName")
        }
    }
}
