package com.mmc.merimusicplayer.ui.viewmodel

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class ConfigTransferViewModelUninitializedTest {
    private val uri = mock(Uri::class.java)

    @Test
    fun `transfers are ignored before initialization`() {
        val viewModel = ConfigTransferViewModel()

        viewModel.exportConfig(uri)
        viewModel.importConfig(uri)

        assertEquals(ConfigTransferUiState(), viewModel.uiState.value)
    }

    @Test
    fun `default config file name is used before initialization`() {
        val viewModel = ConfigTransferViewModel()

        assertEquals("memiplayer_config.json", viewModel.generateBackupFileName())
        assertEquals("memiplayer_config.json", viewModel.generateConfigFileName())
    }
}
