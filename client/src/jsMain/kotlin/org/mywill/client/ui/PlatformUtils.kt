package org.mywill.client.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.files.FileReader

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
