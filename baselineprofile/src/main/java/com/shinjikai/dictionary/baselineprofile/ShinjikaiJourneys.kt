package com.shinjikai.dictionary.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val TargetPackage = "com.shinjikai.dictionary"

internal fun MacrobenchmarkScope.dismissIntroductionIfVisible() {
    val skipLabel = targetString("intro_action_skip")
    device.wait(Until.findObject(By.text(skipLabel)), 1_000)?.click()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.openPrimaryTabs() {
    listOf(
        "nav_browse",
        "history_title",
        "nav_bookmarks",
        "nav_settings",
        "nav_search"
    ).forEach { resourceName ->
        val label = targetString(resourceName)
        val tab = device.wait(Until.findObject(By.text(label)), 2_000)
            ?: device.wait(Until.findObject(By.desc(label)), 1_000)
            ?: error("Bottom tab not found: $label")
        tab.click()
        device.waitForIdle()
    }
}

private fun targetString(resourceName: String): String {
    val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
    val targetContext = instrumentationContext.createPackageContext(TargetPackage, 0)
    val resourceId = targetContext.resources.getIdentifier(resourceName, "string", TargetPackage)
    check(resourceId != 0) { "String resource not found: $resourceName" }
    return targetContext.getString(resourceId)
}
