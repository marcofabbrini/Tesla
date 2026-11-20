package com.marcofabbrini.teslachargecost.widget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Ultima lettura della Tesla, come la restituisce marcofabbrini.com/api/tesla-widget.php */
data class TeslaStato(
    val nome: String,
    val soc: Int?,
    val limite: Int?,
    val autonomiaKm: Int?,
    val inCarica: Boolean,
    val carica: String,
    val potenzaKw: Double?,
    val kwhAggiunti: Double?,
    val minutiPieno: Int?,
    val km: Int?,
    val gomme: List<Double?>,
    val tempInt: Double?,
    val tempEst: Double?,
    val bloccata: Boolean?,
    val sentinella: Boolean?,
    val casa: Boolean,
    val ts: Long?,
)

/** Impostazioni e cache dei widget (SharedPreferences: leggibili subito anche dal widget) */
object WidgetPrefs {
    const val SITO_PREDEFINITO = "https://www.marcofabbrini.com"
    private const val NOME = "tesla_widget"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    fun sito(ctx: Context): String = prefs(ctx).getString("sito", SITO_PREDEFINITO) ?: SITO_PREDEFINITO
    fun chiave(ctx: Context): String = prefs(ctx).getString("chiave", "") ?: ""
    fun ultimoEsito(ctx: Context): String = prefs(ctx).getString("esito", "") ?: ""
    fun cache(ctx: Context): String? = prefs(ctx).getString("cache", null)

    fun salva(ctx: Context, sito: String, chiave: String) {
        prefs(ctx).edit().putString("sito", sito.trim().trimEnd('/')).putString("chiave", chiave.trim()).apply()
    }

    fun salvaCache(ctx: Context, json: String?, esito: String) {
        val e = prefs(ctx).edit().putString("esito", esito)
        if (json != null) e.putString("cache", json)
        e.apply()
    }
}

object TeslaApi {
    /** Scarica l'ultima lettura (da chiamare fuori dal thread principale). Restituisce la lettura nuova o quella in cache. */
    fun aggiorna(ctx: Context): TeslaStato? {
        val chiave = WidgetPrefs.chiave(ctx)
        if (!Regex("^[a-f0-9]{48}$").matches(chiave)) {
            WidgetPrefs.salvaCache(ctx, null, "Chiave mancante: creala sul sito e incollala nelle impostazioni dell'app.")
            return parse(WidgetPrefs.cache(ctx))
        }
        var con: HttpURLConnection? = null
        try {
            con = URL(WidgetPrefs.sito(ctx) + "/api/tesla-widget.php").openConnection() as HttpURLConnection
            con.connectTimeout = 12000
            con.readTimeout = 15000
            con.setRequestProperty("Authorization", "Bearer $chiave")
            con.setRequestProperty("Accept", "application/json")
            val code = con.responseCode
            if (code == 200) {
                val body = con.inputStream.bufferedReader().use { it.readText() }
                val s = parse(body)
                WidgetPrefs.salvaCache(ctx, body, if (s != null) "Aggiornato" else "Nessuna lettura sul sito")
                return s ?: parse(WidgetPrefs.cache(ctx))
            }
            WidgetPrefs.salvaCache(ctx, null, if (code == 401) "Chiave non valida" else "Errore del sito ($code)")
        } catch (e: Exception) {
            WidgetPrefs.salvaCache(ctx, null, "Sito non raggiungibile")
        } finally {
            con?.disconnect()
        }
        return parse(WidgetPrefs.cache(ctx))
    }

    fun parse(json: String?): TeslaStato? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            if (!o.optBoolean("ok") || o.isNull("ts")) return null
            fun d(k: String): Double? = if (o.isNull(k)) null else o.optDouble(k).takeIf { !it.isNaN() }
            fun i(k: String): Int? = if (o.isNull(k)) null else o.optInt(k)
            fun b(k: String): Boolean? = if (o.isNull(k)) null else o.optBoolean(k)
            val g = o.optJSONArray("gomme")
            val gomme = (0 until 4).map { idx -> if (g == null || g.isNull(idx)) null else g.optDouble(idx).takeIf { !it.isNaN() } }
            TeslaStato(
                nome = o.optString("nome", "Tesla Model 3"),
                soc = i("soc"), limite = i("limite"), autonomiaKm = i("autonomia_km"),
                inCarica = o.optBoolean("in_carica"), carica = o.optString("carica", ""),
                potenzaKw = d("potenza_kw"), kwhAggiunti = d("kwh_aggiunti"), minutiPieno = i("minuti_pieno"),
                km = i("km"), gomme = gomme, tempInt = d("temp_int"), tempEst = d("temp_est"),
                bloccata = b("bloccata"), sentinella = b("sentinella"), casa = o.optBoolean("casa"),
                ts = runCatching { Instant.parse(o.getString("ts")).toEpochMilli() }.getOrNull(),
            )
        } catch (e: Exception) {
            null
        }
    }
}
