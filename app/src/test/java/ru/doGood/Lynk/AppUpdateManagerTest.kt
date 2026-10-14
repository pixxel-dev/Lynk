package ru.doGood.Lynk

import com.example.lynk.core.domain.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun isVersionNewer_comparesVersionsCorrectly() {
        val manager = AppUpdateManager()

        // Leading 'v' and 3-digit versions
        assertTrue(manager.isVersionNewer("v1.0.1", "1.0.0"))
        assertTrue(manager.isVersionNewer("v1.1.0", "v1.0.9"))
        assertFalse(manager.isVersionNewer("v1.0.0", "1.0.0"))

        // 4-digit versions
        assertTrue(manager.isVersionNewer("1.0.0.1", "1.0.0"))
        assertTrue(manager.isVersionNewer("v1.0.0.2", "v1.0.0.1"))
        assertFalse(manager.isVersionNewer("1.0.0.0", "1.0.0"))

        // Suffixes and tags
        assertTrue(manager.isVersionNewer("v2.0.0-rc1", "1.9.9"))
        assertFalse(manager.isVersionNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun accessToken_propertyHandledCorrectly() {
        val manager = AppUpdateManager("1.0.0", "pixxel-dev/Lynk", "my_secret_token")
        assertEquals("my_secret_token", manager.githubAccessToken)

        manager.githubAccessToken = "new_token"
        assertEquals("new_token", manager.githubAccessToken)
    }
}
