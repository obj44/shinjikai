package com.shinjikai.dictionary.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = TargetPackage,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        dismissIntroductionIfVisible()
    }

    @Test
    fun criticalUserJourneys() = baselineProfileRule.collect(
        packageName = TargetPackage,
        includeInStartupProfile = false
    ) {
        pressHome()
        startActivityAndWait()
        dismissIntroductionIfVisible()
        openPrimaryTabs()
    }
}
