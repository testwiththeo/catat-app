package com.catat.app.domain.export

import com.catat.app.domain.model.BugReport
import com.catat.app.domain.model.DeviceInfo
import com.catat.app.domain.model.ExportFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportEngineTest {

    private val engine = ExportEngine()
    private val report = BugReport(
        id = 7,
        title = "Login *fails* [prod]",
        stepsToReproduce = "1. Open app\n2. Tap login",
        actualResult = "Spinner never stops",
        expectedResult = "Dashboard opens",
        annotatedScreenshotPath = "/tmp/annotated.png",
        deviceInfo = DeviceInfo(
            deviceModel = "Pixel",
            manufacturer = "Google",
            osVersion = "14",
            sdkLevel = 34,
            buildNumber = "UP1A",
            networkType = "WiFi",
            batteryLevel = 88,
            ramAvailableMb = 2048,
            storageFreeMb = 4096,
            screenResolution = "1080x2400",
            screenDensity = 440
        )
    )

    @Test
    fun `renders jira with jira escaping`() {
        val output = engine.render(report, ExportFormat.Jira)

        assertTrue(output.contains("h2. Login \\*fails\\* \\[prod]"))
        assertTrue(output.contains("{noformat}"))
        assertTrue(output.contains("!annotated.png!"))
    }

    @Test
    fun `renders github markdown`() {
        val output = engine.render(report, ExportFormat.GitHub)

        assertTrue(output.contains("# Login *fails* [prod]"))
        assertTrue(output.contains("## Steps to Reproduce"))
        assertTrue(output.contains("![annotated.png](annotated.png)"))
    }

    @Test
    fun `renders linear markdown`() {
        val output = engine.render(report, ExportFormat.Linear)

        assertTrue(output.contains("## Login *fails* [prod]"))
        assertTrue(output.contains("## Actual Result"))
    }

    @Test
    fun `renders generic markdown and extension`() {
        val output = engine.render(report, ExportFormat.Markdown)

        assertTrue(output.contains("# Login *fails* [prod]"))
        assertEquals("md", engine.fileExtension(ExportFormat.Markdown))
        assertEquals("txt", engine.fileExtension(ExportFormat.Jira))
    }
}
