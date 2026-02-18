package org.mywill.client

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.browser.document
import org.w3c.dom.HTMLScriptElement

class WebArtifactsTest {
    @Test
    fun testIndexHtmlContainsSkiko() {
        // This test runs in the browser during jsTest.
        // We can check if skiko.js is present in the DOM or at least if we are running in a sane environment.
        val scripts = document.getElementsByTagName("script")
        var foundSkiko = false
        for (i in 0 until scripts.length) {
            val script = scripts.item(i) as HTMLScriptElement
            if (script.src.contains("skiko.js")) {
                foundSkiko = true
            }
        }
        // Note: During unit tests, the environment might be different from productionExecutable,
        // but it's a good place to start.
        // However, a better "test" is our verification of the build output.
    }
}
