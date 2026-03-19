package org.mywill.client.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import mywill.client.generated.resources.Res
import mywill.client.generated.resources.mic
import mywill.client.generated.resources.stop
import org.jetbrains.compose.resources.painterResource
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader

internal external interface JsMediaStream {
    fun getTracks(): Array<dynamic>
}

internal external interface JsMediaDevices {
    fun getUserMedia(constraints: dynamic): kotlin.js.Promise<dynamic>
}

internal external interface JsNavigator {
    // Включаем свойство прямо в интерфейс, чтобы избежать конфликтов с расширениями
    val mediaDevices: JsMediaDevices
}

// Убираем расширение, так как оно больше не нужно (свойство есть в интерфейсе)

/**
 * Реализация выбора файлов для браузера (JS).
 */
actual fun openFilePicker(onFilesSelected: (List<SelectedFile>) -> Unit) {
    try {
        val input = document.createElement("input") as? org.w3c.dom.HTMLInputElement
        if (input != null) {
            input.type = "file"
            input.multiple = true
            input.onchange = { event: dynamic ->
                val files = event.target.files
                if (files != null && files.length > 0) {
                    val selectedFiles = mutableListOf<SelectedFile>()
                    var processedCount = 0
                    val totalFiles = files.length as Int

                    for (i in 0 until totalFiles) {
                        val file = files[i]!!
                        val reader = FileReader()
                        reader.onload = { loadEvent ->
                            val arrayBuffer = loadEvent.target.asDynamic().result as? org.khronos.webgl.ArrayBuffer
                            if (arrayBuffer != null) {
                                val bytes = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
                                selectedFiles.add(SelectedFile(file.name, bytes))
                            }
                            processedCount++
                            if (processedCount == totalFiles) {
                                onFilesSelected(selectedFiles)
                            }
                        }
                        reader.readAsArrayBuffer(file)
                    }
                }
            }
            input.click()
        }
    } catch (e: Exception) {
        println("[ERROR_LOG] Native file picker failed in JS: ${e.message}")
    }
}

/**
 * Реализация скачивания файла для браузера (JS).
 */
actual fun downloadFile(url: String, fileName: String, bytes: ByteArray?) {
    if (bytes != null) {
        // Если байты предоставлены (уже скачаны с авторизацией), создаем Blob
        // В Kotlin/JS ByteArray - это Int8Array. Blob понимает его.
        val blob = org.w3c.files.Blob(arrayOf(bytes), org.w3c.files.BlobPropertyBag(type = "application/octet-stream"))
        val blobUrl = org.w3c.dom.url.URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = blobUrl
        anchor.download = fileName
        anchor.click()
        org.w3c.dom.url.URL.revokeObjectURL(blobUrl)
    } else {
        // Иначе обычное скачивание (может не работать, если нужен Bearer)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = fileName
        anchor.click()
    }
}

private fun Int8Array(buffer: org.khronos.webgl.ArrayBuffer): ByteArray {
    val view = org.khronos.webgl.Int8Array(buffer)
    val len = view.length
    val bytes = ByteArray(len)
    for (i in 0 until len) {
        bytes[i] = view.asDynamic()[i] as Byte
    }
    return bytes
}

private var mediaRecorder: dynamic = null
private var audioChunks = mutableListOf<Blob>()

actual val isAudioRecordingSupported: Boolean = true

actual fun startAudioRecording() {
    audioChunks.clear()
    val navigator = window.navigator.unsafeCast<JsNavigator>()
    navigator.mediaDevices.getUserMedia(js("{audio: true}")).then { stream: dynamic ->
        mediaRecorder = js("new MediaRecorder(stream)")
        mediaRecorder.ondataavailable = { event: dynamic ->
            audioChunks.add(event.data as Blob)
        }
        mediaRecorder.start()
    }.catch { err: dynamic ->
        println("[ERROR_LOG] Failed to start recording: $err")
    }
}

actual fun stopAudioRecording(onResult: (SelectedFile?) -> Unit) {
    if (mediaRecorder == null) {
        onResult(null)
        return
    }

    mediaRecorder.onstop = {
        val blob = Blob(audioChunks.toTypedArray(), BlobPropertyBag(type = "audio/webm"))
        val reader = FileReader()
        reader.onload = { loadEvent ->
            val arrayBuffer = loadEvent.target.asDynamic().result as? org.khronos.webgl.ArrayBuffer
            if (arrayBuffer != null) {
                val bytes = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
                val timestamp = js("new Date().getTime()").toString()
                onResult(SelectedFile("voice_message_$timestamp.webm", bytes))
            } else {
                onResult(null)
            }
        }
        reader.readAsArrayBuffer(blob)

        // Остановка треков стрима
        val stream = mediaRecorder.stream
        if (stream != null) {
            val tracks = stream.getTracks().unsafeCast<Array<dynamic>>()
            tracks.forEach { track -> track.asDynamic().stop() }
        }
        mediaRecorder = null
    }
    mediaRecorder.stop()
}

@androidx.compose.runtime.Composable
actual fun AudioPlayer(url: String, authToken: String?) {
    var isPlaying by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var audio: HTMLAudioElement? by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }

    androidx.compose.runtime.DisposableEffect(url, authToken) {
        val element = document.createElement("audio") as HTMLAudioElement
        audio = element

        val xhr = org.w3c.xhr.XMLHttpRequest()
        xhr.open("GET", url)
        if (authToken != null) {
            xhr.setRequestHeader("Authorization", "Bearer $authToken")
        }
        xhr.responseType = "blob".asDynamic()
        xhr.onload = {
            if (xhr.status == 200.toShort()) {
                val blob = xhr.response as? Blob
                if (blob != null) {
                    val blobUrl = URL.createObjectURL(blob)
                    element.src = blobUrl
                }
            }
        }
        xhr.onerror = {
            println("[ERROR_LOG] XHR request failed for $url")
        }
        xhr.send()

        element.onplay = { isPlaying = true }
        element.onpause = { isPlaying = false }
        element.onended = { isPlaying = false }

        onDispose {
            xhr.abort()
            if (element.src.startsWith("blob:")) {
                URL.revokeObjectURL(element.src)
            }
            element.pause()
            audio = null
        }
    }

    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        androidx.compose.material3.IconButton(onClick = {
            val a = audio ?: return@IconButton
            if (isPlaying) a.pause() else a.play()
        }) {
            androidx.compose.material3.Icon(
                painter = painterResource(if (isPlaying) Res.drawable.stop else Res.drawable.mic),
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        androidx.compose.material3.Text(
            text = if (isPlaying) "Прослушивание..." else "Голосовое сообщение",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = androidx.compose.ui.Modifier.padding(start = 8.dp)
        )
    }
}
