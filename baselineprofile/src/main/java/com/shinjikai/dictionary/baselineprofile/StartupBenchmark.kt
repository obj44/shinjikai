package com.shinjikai.dictionary.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun bottomTabSwitching() = benchmarkRule.measureRepeated(
        packageName = TargetPackage,
        metrics = listOf(FrameTimingMetric()),
        iterations = 8,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            pressHome()
            startActivityAndWait()
            dismissIntroductionIfVisible()
        }
    ) {
        openPrimaryTabs()
    }
}
