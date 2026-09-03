package com.haitranduc.fittrack

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureSmokeTest {

    @Test
    fun m0_testHarness_isWorking() {
        assertTrue("Test harness is functioning correctly", true)
    }

    @Test
    fun m0_packageSkeleton_allDirectoriesExist() {
        val expectedPackages = listOf(
            "core/database",
            "core/designsystem/theme",
            "core/navigation",
            "core/util",
            "data/local/dao",
            "data/local/entity",
            "data/local/relation",
            "data/mapper",
            "data/repository",
            "domain/model",
            "domain/repository",
            "domain/usecase",
            "presentation/exercise",
            "presentation/workout",
            "presentation/activeworkout",
            "presentation/history",
            "presentation/settings"
        )

        val srcMain = File("src/main/java/com/haitranduc/fittrack")
        for (pkg in expectedPackages) {
            val dir = File(srcMain, pkg)
            assertTrue("Expected package directory does not exist: $pkg", dir.exists() && dir.isDirectory)
        }
    }
}
