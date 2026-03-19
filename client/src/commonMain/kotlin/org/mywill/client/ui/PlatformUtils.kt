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

/**
 * Проигрывает аудио по URL с заголовком авторизации.
 */
@androidx.compose.runtime.Composable
expect fun AudioPlayer(url: String, authToken: String?)

/**
 * Проигрывает видео по URL с заголовком авторизации.
 */
@androidx.compose.runtime.Composable
expect fun VideoPlayer(url: String, authToken: String?, onClose: () -> Unit)

/**
 * Показывает изображение по URL с заголовком авторизации.
 */
@androidx.compose.runtime.Composable
expect fun ImageViewer(url: String, authToken: String?, onClose: () -> Unit)

/**
 * Запускает запись аудио.
 */
expect fun startAudioRecording()

/**
 * Останавливает запись аудио и возвращает результат в колбэк.
 */
expect fun stopAudioRecording(onResult: (SelectedFile?) -> Unit)

/**
 * Запускает запись видео.
 */
expect fun startVideoRecording()

/**
 * Останавливает запись видео и возвращает результат в колбэк.
 */
expect fun stopVideoRecording(onResult: (SelectedFile?) -> Unit)

/**
 * Поддерживается ли запись аудио на данной платформе.
 */
expect val isAudioRecordingSupported: Boolean

/**
 * Поддерживается ли запись видео на данной платформе.
 */
expect val isVideoRecordingSupported: Boolean
