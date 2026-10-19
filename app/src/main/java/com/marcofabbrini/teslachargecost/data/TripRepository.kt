package com.marcofabbrini.teslachargecost.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.tripsDataStore by preferencesDataStore(name = "trips")

/** Storico dei consumi, salvato solo sul dispositivo. */
class TripRepository(private val context: Context) {

    private val recordsKey = stringPreferencesKey("records_json")

    val records: Flow<List<TripRecord>> = context.tripsDataStore.data.map { decode(it[recordsKey]) }

    suspend fun add(record: TripRecord) {
        context.tripsDataStore.edit { prefs ->
            prefs[recordsKey] = encode(decode(prefs[recordsKey]) + record)
        }
    }

    suspend fun delete(id: Long) {
        context.tripsDataStore.edit { prefs ->
            prefs[recordsKey] = encode(decode(prefs[recordsKey]).filter { it.id != id })
        }
    }

    private fun encode(list: List<TripRecord>): String {
        val array = JSONArray()
        list.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("mode", r.mode.name)
                    put("percent", r.percent)
                    put("capacityKwh", r.capacityKwh)
                    put("kwh", r.kwh)
                    put("cost", r.cost)
                    put("costPerKwh", r.costPerKwh)
                    r.km?.let { put("km", it) }
                },
            )
        }
        return array.toString()
    }

    private fun decode(json: String?): List<TripRecord> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                TripRecord(
                    id = o.getLong("id"),
                    mode = ChargeMode.valueOf(o.getString("mode")),
                    percent = o.getDouble("percent"),
                    capacityKwh = o.getDouble("capacityKwh"),
                    kwh = o.getDouble("kwh"),
                    cost = o.getDouble("cost"),
                    costPerKwh = o.getDouble("costPerKwh"),
                    km = if (o.has("km")) o.getDouble("km") else null,
                )
            }
        }.getOrDefault(emptyList())
    }
}
