package com.marcofabbrini.teslachargecost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.marcofabbrini.teslachargecost.data.Settings
import com.marcofabbrini.teslachargecost.data.SettingsRepository
import com.marcofabbrini.teslachargecost.data.TripRecord
import com.marcofabbrini.teslachargecost.data.TripRepository
import com.marcofabbrini.teslachargecost.ui.HistoryScreen
import com.marcofabbrini.teslachargecost.ui.HomeScreen
import com.marcofabbrini.teslachargecost.ui.SettingsScreen
import com.marcofabbrini.teslachargecost.ui.theme.TeslaChargeCostTheme
import kotlinx.coroutines.launch

private enum class Screen { HOME, SETTINGS, HISTORY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(applicationContext)
        val tripRepository = TripRepository(applicationContext)

        setContent {
            TeslaChargeCostTheme {
                val scope = rememberCoroutineScope()
                val settings by settingsRepository.settings.collectAsState(initial = Settings())
                val trips by tripRepository.records.collectAsState(initial = emptyList<TripRecord>())
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

                when (screen) {
                    Screen.HOME -> HomeScreen(
                        settings = settings,
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onOpenHistory = { screen = Screen.HISTORY },
                        onSaveTrip = { record -> scope.launch { tripRepository.add(record) } },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSaveValues = { tariff, capacity ->
                            scope.launch { settingsRepository.saveValues(tariff, capacity) }
                        },
                        onResetValues = { scope.launch { settingsRepository.resetValues() } },
                        onHeaderImageChanged = { uri ->
                            scope.launch { settingsRepository.setHeaderImage(uri) }
                        },
                        onBack = { screen = Screen.HOME },
                    )

                    Screen.HISTORY -> HistoryScreen(
                        records = trips,
                        onDelete = { id -> scope.launch { tripRepository.delete(id) } },
                        onBack = { screen = Screen.HOME },
                    )
                }
            }
        }
    }
}
