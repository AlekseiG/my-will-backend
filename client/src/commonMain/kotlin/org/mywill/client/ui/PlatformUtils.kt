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
 * @param url URL файла.
 * @param fileName Имя файла для сохранения.
 * @param bytes Если предоставлены, использовать эти байты вместо скачивания по URL (для обхода авторизации).
 */
expect fun downloadFile(url: String, fileName: String, bytes: ByteArray? = null)
