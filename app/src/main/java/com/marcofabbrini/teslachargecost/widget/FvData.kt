package com.marcofabbrini.teslachargecost.widget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Un impianto fotovoltaico, come lo restituisce marcofabbrini.com/api/fv-widget.php (potenze in kW, energie in kWh) */
data class FvImpianto(
    val id: String,
    val nome: String,
    val soc: Double?,
    val pvKw: Double?,
    val casaKw: Double?,
    /** positivo = prelievo dalla rete, negativo = immissione */
    val reteKw: Double?,
    /** positivo = batteria in carica, negativo = in scarica */
    val battKw: Double?,
    val oggiPv: Double?,
    val oggiExport: Double?,
    val oggiImport: Double?,
    val oggiLoad: Double?,
    val ieriPv: Double?,
    val mesePv: Double?,
    /** produzione di oggi: (minuti dalla mezzanotte, watt) */
    val curva: List<Pair<Int, Int>>,
)

data class FvStato(val ts: Long?, val online: Boolean, val impianti: List<FvImpianto>)

/** Cache e ultimo esito dei widget fotovoltaico (stesse SharedPreferences dei widget Tesla, chiavi proprie) */
object FvPrefs {
    private const val NOME = "tesla_widget"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(NOME, Context.MODE_PRIVATE)
    fun ultimoEsito(ctx: Context): String = prefs(ctx).getString("fv_esito", "") ?: ""
    fun cache(ctx: Context): String? = prefs(ctx).getString("fv_cache", null)
    fun salvaCache(ctx: Context, json: String?, esito: String) {
        val e = prefs(ctx).edit().putString("fv_esito", esito)
        if (json != null) e.putString("fv_cache", json)
        e.apply()
    }
}

object FvApi {
    /** Scarica i dati (da chiamare fuori dal thread principale). Restituisce la lettura nuova o, se manca la rete, quella in cache. */
    fun aggiorna(ctx: Context): FvStato? {
        val chiave = WidgetPrefs.chiave(ctx)
        if (!Regex("^[a-f0-9]{48}$").matches(chiave)) {
            FvPrefs.salvaCache(ctx, null, "Chiave mancante: creala sul sito e incollala nelle impostazioni dell'app.")
            return parse(FvPrefs.cache(ctx))
        }
        var con: HttpURLConnection? = null
        try {
            con = URL(WidgetPrefs.sito(ctx) + "/api/fv-widget.php").openConnection() as HttpURLConnection
            con.connectTimeout = 12000
            con.readTimeout = 20000
            con.setRequestProperty("Authorization", "Bearer $chiave")
            con.setRequestProperty("Accept", "application/json")
            val code = con.responseCode
            if (code == 200) {
                val body = con.inputStream.bufferedReader().use { it.readText() }
                val s = parse(body)
                FvPrefs.salvaCache(ctx, if (s != null) body else null, if (s != null) "Aggiornato" else "Nessun dato dagli impianti")
                return s ?: parse(FvPrefs.cache(ctx))
            }
            FvPrefs.salvaCache(ctx, null, if (code == 401) "Chiave non valida" else "Errore del sito ($code)")
        } catch (e: Exception) {
            FvPrefs.salvaCache(ctx, null, "Sito non raggiungibile")
        } finally {
            con?.disconnect()
        }
        return parse(FvPrefs.cache(ctx))
    }

    fun parse(json: String?): FvStato? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            if (!o.optBoolean("ok")) return null
            val arr = o.optJSONArray("impianti") ?: return null
            val lista = (0 until arr.length()).mapNotNull { idx -> arr.optJSONObject(idx)?.let { impianto(it) } }
            if (lista.isEmpty()) return null
            FvStato(
                ts = if (o.isNull("ts")) null else runCatching { Instant.parse(o.getString("ts")).toEpochMilli() }.getOrNull(),
                online = o.optBoolean("online"),
                impianti = lista,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun impianto(o: JSONObject): FvImpianto {
        fun d(x: JSONObject?, k: String): Double? = if (x == null || x.isNull(k)) null else x.optDouble(k).takeIf { !it.isNaN() }
        val oggi = o.optJSONObject("oggi")
        val c = o.optJSONArray("curva")
        val curva = if (c == null) emptyList() else (0 until c.length()).mapNotNull { i ->
            c.optJSONArray(i)?.let { p -> if (p.length() >= 2) Pair(p.optInt(0), p.optInt(1)) else null }
        }
        return FvImpianto(
            id = o.optString("id"), nome = o.optString("nome", o.optString("id")),
            soc = d(o, "soc"), pvKw = d(o, "pv_kw"), casaKw = d(o, "casa_kw"), reteKw = d(o, "rete_kw"), battKw = d(o, "batteria_kw"),
            oggiPv = d(oggi, "pv"), oggiExport = d(oggi, "export"), oggiImport = d(oggi, "import"), oggiLoad = d(oggi, "load"),
            ieriPv = d(o, "ieri_pv"), mesePv = d(o, "mese_pv"), curva = curva,
        )
    }
}
