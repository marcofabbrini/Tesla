package com.marcofabbrini.teslachargecost.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Un APK più recente di quello installato, pubblicato nella release "latest" del repository */
data class Aggiornamento(val build: Long, val url: String, val dimensione: Long)

/**
 * Controllo degli aggiornamenti dalla release "latest" di GitHub (repository pubblico, nessuna chiave).
 * La CI usa il numero della build come versionCode e lo scrive nel titolo della release: "... (ultima build 31)".
 */
object AppUpdate {
    private const val API = "https://api.github.com/repos/marcofabbrini/Tesla/releases/tags/latest"

    fun versioneInstallata(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    /** Da chiamare fuori dal thread principale. Restituisce l'aggiornamento se esiste una build più nuova, altrimenti null (anche senza rete). */
    fun controlla(ctx: Context): Aggiornamento? {
        var con: HttpURLConnection? = null
        return try {
            con = URL(API).openConnection() as HttpURLConnection
            con.connectTimeout = 8000
            con.readTimeout = 10000
            con.setRequestProperty("Accept", "application/vnd.github+json")
            if (con.responseCode != 200) return null
            val o = JSONObject(con.inputStream.bufferedReader().use { it.readText() })
            val build = Regex("build\\s+(\\d+)").find(o.optString("name"))?.groupValues?.get(1)?.toLongOrNull() ?: return null
            val assets = o.optJSONArray("assets") ?: return null
            var url = ""; var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) { url = a.optString("browser_download_url"); size = a.optLong("size"); break }
            }
            if (url.isEmpty() || build <= versioneInstallata(ctx)) null else Aggiornamento(build, url, size)
        } catch (e: Exception) {
            null
        } finally {
            con?.disconnect()
        }
    }

    /** Scarica l'APK nella cache dell'app (fuori dal thread principale). onProgresso riceve 0..100. Null se il download fallisce. */
    fun scarica(ctx: Context, a: Aggiornamento, onProgresso: (Int) -> Unit): File? {
        var con: HttpURLConnection? = null
        return try {
            val dir = File(ctx.cacheDir, "apk").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "TeslaChargeCost.apk")
            con = URL(a.url).openConnection() as HttpURLConnection
            con.connectTimeout = 10000
            con.readTimeout = 30000
            if (con.responseCode != 200) return null
            val totale = if (con.contentLengthLong > 0) con.contentLengthLong else a.dimensione
            var letti = 0L; var ultimo = -1
            con.inputStream.use { inp ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n); letti += n
                        if (totale > 0) {
                            val p = (letti * 100 / totale).toInt().coerceIn(0, 100)
                            if (p != ultimo) { ultimo = p; onProgresso(p) }
                        }
                    }
                }
            }
            if (totale > 0 && letti < totale) null else file
        } catch (e: Exception) {
            null
        } finally {
            con?.disconnect()
        }
    }

    fun puoInstallare(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    /** Apre la schermata di Android dove consentire a questa app di installare aggiornamenti (serve una volta sola) */
    fun apriPermessoInstallazione(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Avvia l'installazione di Android: l'aggiornamento si sovrappone all'app (stessa firma) e i dati restano */
    fun installa(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
