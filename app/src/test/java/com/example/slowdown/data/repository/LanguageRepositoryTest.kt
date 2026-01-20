package com.sharonZ.slowdown.data.repository

import app.cash.turbine.test
import com.sharonZ.slowdown.data.preferences.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * TDD Tests for LanguageRepository
 * 
 * These tests define the expected behavior of the LanguageRepository:
 * - Get/set the preferred language (zh/en)
 * - Default language should be English (en)
 */
class LanguageRepositoryTest {

    private lateinit var languageRepository: LanguageRepository
    private lateinit var userPreferences: UserPreferences

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
    }

    @Test
    fun `getLanguage should return default English when no preference is set`() = runTest {
        // Use MutableStateFlow instead of flowOf to prevent premature completion
        val languageFlow = MutableStateFlow("en")
        every { userPreferences.appLanguage } returns languageFlow
        
        languageRepository = LanguageRepository(userPreferences)
        
        languageRepository.appLanguage.test {
            assertEquals("en", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLanguage to Chinese should update preferences`() = runTest {
        every { userPreferences.appLanguage } returns flowOf("en")
        coEvery { userPreferences.setAppLanguage(any()) } returns Unit
        
        languageRepository = LanguageRepository(userPreferences)
        languageRepository.setAppLanguage("zh")
        
        coVerify { userPreferences.setAppLanguage("zh") }
    }

    @Test
    fun `setLanguage to English should update preferences`() = runTest {
        every { userPreferences.appLanguage } returns flowOf("zh")
        coEvery { userPreferences.setAppLanguage(any()) } returns Unit
        
        languageRepository = LanguageRepository(userPreferences)
        languageRepository.setAppLanguage("en")
        
        coVerify { userPreferences.setAppLanguage("en") }
    }
}
