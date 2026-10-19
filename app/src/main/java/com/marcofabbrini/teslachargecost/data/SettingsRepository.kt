package com.marcofabbrini.teslachargecost.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_HOME_TARIFF = 0.25 // €/kWh
const val DEFAULT_BATTERY_KWH = 60.5 // kWh utili Model 3 Standard

data class Settings(
    val homeTariff: Double = DEFAULT_HOME_TARIFF,
    val batteryCapacityKwh: Double = DEFAULT_BATTERY_KWH,
    val headerImageUri: String? = null,
    val appFont: String = "exo2",
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val TARIFF = doublePreferencesKey("home_tariff")
        val CAPACITY = doublePreferencesKey("battery_capacity_kwh")
        val HEADER_URI = stringPreferencesKey("header_image_uri")
        val APP_FONT = stringPreferencesKey("app_font")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun saveValues(homeTariff: Double, batteryCapacityKwh: Double) {
        context.dataStore.edit {
            it[Keys.TARIFF] = homeTariff
            it[Keys.CAPACITY] = batteryCapacityKwh
        }
    }

    suspend fun setHeaderImage(uri: String?) {
        context.dataStore.edit {
            if (uri == null) it.remove(Keys.HEADER_URI) else it[Keys.HEADER_URI] = uri
        }
    }

    suspend fun setAppFont(key: String) {
        context.dataStore.edit { it[Keys.APP_FONT] = key }
    }

    suspend fun resetValues() {
        context.dataStore.edit {
            it.remove(Keys.TARIFF)
            it.remove(Keys.CAPACITY)
        }
    }

    private fun Preferences.toSettings() = Settings(
        homeTariff = this[Keys.TARIFF] ?: DEFAULT_HOME_TARIFF,
        batteryCapacityKwh = this[Keys.CAPACITY] ?: DEFAULT_BATTERY_KWH,
        headerImageUri = this[Keys.HEADER_URI],
        appFont = this[Keys.APP_FONT] ?: "exo2",
    )
}
