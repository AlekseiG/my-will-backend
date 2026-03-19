package org.mywill.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

actual fun openFilePicker(onFilesSelected: (List<SelectedFile>) -> Unit) {
    // В Android обычно используется Intent.ACTION_GET_CONTENT, 
    // что требует контекста Activity и обработки в onActivityResult.
    // Оставляем пустую реализацию или выводим лог, так как в App.kt для Android
    // вызов идет через контроллер или напрямую.
    println("[DEBUG_LOG] openFilePicker not implemented for Android in PlatformUtils")
}

actual fun downloadFile(url: String, fileName: String, bytes: ByteArray?) {
    println("[DEBUG_LOG] downloadFile requested for $url as $fileName")
}

@Composable
actual fun AudioPlayer(url: String, authToken: String?) {
    Text("Прослушивание аудио на Android пока не реализовано")
}

@Composable
actual fun VideoPlayer(url: String, authToken: String?, onClose: () -> Unit) {
    Text("Просмотр видео на Android пока не реализовано")
}

@Composable
actual fun ImageViewer(url: String, authToken: String?, onClose: () -> Unit) {
    Text("Просмотр фото на Android пока не реализовано")
}

actual fun startAudioRecording() {
    println("[DEBUG_LOG] startAudioRecording (mock for Android)")
}

actual fun stopAudioRecording(onResult: (SelectedFile?) -> Unit) {
    println("[DEBUG_LOG] stopAudioRecording (mock for Android)")
    onResult(null)
}

actual fun startVideoRecording() {
    println("[DEBUG_LOG] startVideoRecording (mock for Android)")
}

actual fun stopVideoRecording(onResult: (SelectedFile?) -> Unit) {
    println("[DEBUG_LOG] stopVideoRecording (mock for Android)")
    onResult(null)
}

actual val isAudioRecordingSupported: Boolean = false
actual val isVideoRecordingSupported: Boolean = false
