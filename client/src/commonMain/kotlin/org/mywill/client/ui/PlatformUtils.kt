package org.mywill.client.ui

/**
 * Информация о выбранном файле.
 */
class SelectedFile(
    val name: String,
    val bytes: ByteArray
)

/**
 * Платформенно-зависимый выбор файлов.
 */
expect fun openFilePicker(onFilesSelected: (List<SelectedFile>) -> Unit)

/**
 * Платформенно-зависимое скачивание файла.
 */
expect fun downloadFile(url: String, fileName: String)
