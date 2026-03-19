package org.mywill.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import mywill.client.generated.resources.Res
import mywill.client.generated.resources.mic
import mywill.client.generated.resources.stop
import mywill.client.generated.resources.play_arrow
import mywill.client.generated.resources.download
import mywill.client.generated.resources.image
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
private var videoChunks = mutableListOf<Blob>()
private var recordingPreview: org.w3c.dom.HTMLVideoElement? = null

actual val isAudioRecordingSupported: Boolean = true
actual val isVideoRecordingSupported: Boolean = true

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
        println("[ERROR_LOG] Failed to start audio recording: $err")
    }
}

actual fun stopAudioRecording(onResult: (SelectedFile?) -> Unit) {
    stopRecording("audio/webm", audioChunks, "voice_message", onResult)
}

actual fun startVideoRecording() {
    videoChunks.clear()
    val navigator = window.navigator.unsafeCast<JsNavigator>()
    navigator.mediaDevices.getUserMedia(js("{audio: true, video: true}")).then { stream: dynamic ->
        mediaRecorder = js("new MediaRecorder(stream)")
        mediaRecorder.ondataavailable = { event: dynamic ->
            videoChunks.add(event.data as Blob)
        }

        // Показываем предпросмотр
        val video = document.createElement("video") as org.w3c.dom.HTMLVideoElement
        video.id = "video-recording-preview"
        video.srcObject = stream
        video.autoplay = true
        video.muted = true // Чтобы не было эха при записи
        video.style.position = "fixed"
        video.style.bottom = "20px"
        video.style.right = "20px"
        video.style.width = "300px"
        video.style.height = "225px"
        video.style.zIndex = "9999"
        video.style.border = "2px solid red"
        video.style.borderRadius = "8px"
        video.style.backgroundColor = "black"
        document.body?.appendChild(video)
        recordingPreview = video

        mediaRecorder.start()
    }.catch { err: dynamic ->
        println("[ERROR_LOG] Failed to start video recording: $err")
    }
}

actual fun stopVideoRecording(onResult: (SelectedFile?) -> Unit) {
    stopRecording("video/webm", videoChunks, "video_message", onResult)
}

private fun stopRecording(
    mimeType: String,
    chunks: MutableList<Blob>,
    fileNamePrefix: String,
    onResult: (SelectedFile?) -> Unit
) {
    if (mediaRecorder == null) {
        onResult(null)
        return
    }

    mediaRecorder.onstop = {
        val blob = Blob(chunks.toTypedArray(), BlobPropertyBag(type = mimeType))
        val reader = FileReader()
        reader.onload = { loadEvent ->
            val result = loadEvent.target.asDynamic().result
            if (result != null) {
                val arrayBuffer = result.unsafeCast<org.khronos.webgl.ArrayBuffer>()
                val bytes = Int8Array(arrayBuffer).unsafeCast<ByteArray>()
                val timestamp = js("new Date().getTime()").toString()
                onResult(SelectedFile("${fileNamePrefix}_$timestamp.webm", bytes))
            } else {
                onResult(null)
            }
        }
        reader.readAsArrayBuffer(blob)

        // Остановка треков стрима
        val stream = mediaRecorder.stream
        if (stream != null) {
            val tracks = stream.getTracks().unsafeCast<Array<dynamic>>()
            tracks.forEach { track ->
                try {
                    track.stop()
                } catch (e: Exception) {
                    println("[ERROR_LOG] Failed to stop track: $e")
                }
            }
        }

        // Удаление превью
        recordingPreview?.let {
            it.pause()
            it.srcObject = null
            document.body?.removeChild(it)
            recordingPreview = null
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
        modifier = androidx.compose.ui.Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        androidx.compose.material3.IconButton(onClick = {
            val a = audio ?: return@IconButton
            if (isPlaying) a.pause() else a.play()
        }) {
            androidx.compose.material3.Icon(
                painter = painterResource(if (isPlaying) Res.drawable.stop else Res.drawable.play_arrow),
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
    }
}

@androidx.compose.runtime.Composable
actual fun VideoPlayer(url: String, authToken: String?, onClose: () -> Unit) {
    var isLoading by androidx.compose.runtime.remember(url) { androidx.compose.runtime.mutableStateOf(true) }
    var loadError by androidx.compose.runtime.remember(url) { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
            .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        val videoId = "video-element-${url.hashCode()}"

        androidx.compose.runtime.DisposableEffect(url, authToken) {
            val element = document.createElement("video") as org.w3c.dom.HTMLVideoElement
            element.id = videoId
            element.style.width = "100%"
            element.style.height = "100%"
            element.style.objectFit = "contain"
            element.controls = true

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
                        isLoading = false
                    } else {
                        loadError = true
                    }
                } else {
                    loadError = true
                }
            }
            xhr.onerror = {
                loadError = true
                isLoading = false
            }
            xhr.send()

            element.style.position = "fixed"
            element.style.top = "50%"
            element.style.left = "50%"
            element.style.transform = "translate(-50%, -50%)"
            element.style.zIndex = "1000"
            element.style.maxWidth = "90vw"
            element.style.maxHeight = "80vh"
            element.style.boxShadow = "0 0 20px rgba(0,0,0,0.5)"
            element.style.backgroundColor = "black"

            // Добавляем кнопку закрытия для видеоплеера
            val closeBtn = document.createElement("button") as org.w3c.dom.HTMLButtonElement
            closeBtn.innerText = "Закрыть видео"
            closeBtn.style.position = "fixed"
            closeBtn.style.top = "10px"
            closeBtn.style.right = "10px"
            closeBtn.style.zIndex = "1001"
            closeBtn.style.padding = "8px 16px"
            closeBtn.style.cursor = "pointer"
            closeBtn.onclick = {
                onClose()
            }
            document.body?.appendChild(closeBtn)

            document.body?.appendChild(element)

            onDispose {
                xhr.abort()
                if (element.src.startsWith("blob:")) {
                    URL.revokeObjectURL(element.src)
                }
                element.pause()
                document.body?.removeChild(element)
                document.body?.removeChild(closeBtn)
            }
        }

        if (isLoading) {
            androidx.compose.material3.Text("Загрузка видео...", color = androidx.compose.ui.graphics.Color.White)
        } else if (loadError) {
            androidx.compose.material3.Text("Ошибка загрузки", color = androidx.compose.ui.graphics.Color.Red)
        }
    }
}

@androidx.compose.runtime.Composable
actual fun ImageViewer(url: String, authToken: String?, onClose: () -> Unit) {
    var isLoading by androidx.compose.runtime.remember(url) { androidx.compose.runtime.mutableStateOf(true) }
    var loadError by androidx.compose.runtime.remember(url) { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
            .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.runtime.DisposableEffect(url, authToken) {
            val element = document.createElement("img") as org.w3c.dom.HTMLImageElement
            element.style.position = "fixed"
            element.style.top = "50%"
            element.style.left = "50%"
            element.style.transform = "translate(-50%, -50%)"
            element.style.zIndex = "1000"
            element.style.maxWidth = "90vw"
            element.style.maxHeight = "80vh"
            element.style.boxShadow = "0 0 20px rgba(0,0,0,0.5)"
            element.style.backgroundColor = "black"
            element.style.objectFit = "contain"

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
                        isLoading = false
                    } else {
                        loadError = true
                    }
                } else {
                    loadError = true
                }
            }
            xhr.onerror = {
                loadError = true
                isLoading = false
            }
            xhr.send()

            val closeBtn = document.createElement("button") as org.w3c.dom.HTMLButtonElement
            closeBtn.innerText = "Закрыть фото"
            closeBtn.style.position = "fixed"
            closeBtn.style.top = "10px"
            closeBtn.style.right = "10px"
            closeBtn.style.zIndex = "1001"
            closeBtn.style.padding = "8px 16px"
            closeBtn.style.cursor = "pointer"
            closeBtn.onclick = {
                onClose()
            }
            document.body?.appendChild(closeBtn)
            document.body?.appendChild(element)

            onDispose {
                xhr.abort()
                if (element.src.startsWith("blob:")) {
                    URL.revokeObjectURL(element.src)
                }
                document.body?.removeChild(element)
                document.body?.removeChild(closeBtn)
            }
        }

        if (isLoading) {
            androidx.compose.material3.Text("Загрузка фото...", color = androidx.compose.ui.graphics.Color.White)
        } else if (loadError) {
            androidx.compose.material3.Text("Ошибка загрузки", color = androidx.compose.ui.graphics.Color.Red)
        }
    }
}
