package com.marcofabbrini.teslachargecost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.marcofabbrini.teslachargecost.data.Settings
import com.marcofabbrini.teslachargecost.data.SettingsRepository
import com.marcofabbrini.teslachargecost.ui.HomeScreen
import com.marcofabbrini.teslachargecost.ui.SettingsScreen
import com.marcofabbrini.teslachargecost.ui.theme.TeslaChargeCostTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SettingsRepository(applicationContext)

        setContent {
            TeslaChargeCostTheme {
                val scope = rememberCoroutineScope()
                val settings by repository.settings.collectAsState(initial = Settings())
                var showSettings by rememberSaveable { mutableStateOf(false) }

                BackHandler(enabled = showSettings) { showSettings = false }

                if (showSettings) {
                    SettingsScreen(
                        settings = settings,
                        onSaveValues = { tariff, capacity ->
                            scope.launch { repository.saveValues(tariff, capacity) }
                        },
                        onResetValues = { scope.launch { repository.resetValues() } },
                        onHeaderImageChanged = { uri ->
                            scope.launch { repository.setHeaderImage(uri) }
                        },
                        onBack = { showSettings = false },
                    )
                } else {
                    HomeScreen(
                        settings = settings,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}
