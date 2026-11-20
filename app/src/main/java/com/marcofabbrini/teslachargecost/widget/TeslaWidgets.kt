package com.marcofabbrini.teslachargecost.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.marcofabbrini.teslachargecost.R
import kotlin.math.roundToInt

/** Base comune: scarica i dati dal sito in un thread, disegna le immagini e le mette nel widget. Tocco sull'immagine = aggiorna. */
abstract class TeslaWidgetBase : AppWidgetProvider() {
    abstract fun viste(ctx: Context, s: TeslaStato?, esito: String, wDp: Int, hDp: Int): RemoteViews
    abstract val dimDefault: Pair<Int, Int>

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = aggiornaAsync(ctx, ids, scarica = true)

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opt: Bundle) =
        aggiornaAsync(ctx, intArrayOf(id), scarica = false)

    override fun onEnabled(ctx: Context) = programma(ctx)
    override fun onDisabled(ctx: Context) { if (!TeslaWidgetsAll.attivi(ctx)) annulla(ctx) }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH -> aggiornaAsync(ctx, ids(ctx), scarica = true)
            ACTION_CMD -> comando(ctx, intent.getStringExtra(EXTRA_BTN) ?: return)
            else -> super.onReceive(ctx, intent)
        }
    }

    protected fun ids(ctx: Context): IntArray = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, javaClass))

    /** Pulsante premuto: sceglie il comando dallo stato attuale, lo manda al sito e segue l'esito per ~30 secondi */
    private fun comando(ctx: Context, btn: String) {
        val app = ctx.applicationContext
        val s = TeslaApi.parse(WidgetPrefs.cache(app))
        val cmd = when (btn) {
            "serratura" -> if (s?.bloccata == false) "blocca" else "sblocca"
            "clima" -> if (s?.clima == true) "clima_off" else "clima_on"
            "sentinella" -> if (s?.sentinella == true) "sentinella_off" else "sentinella_on"
            "fari" -> "fari"
            "sportello" -> "sportello"
            else -> return
        }
        // l'apertura chiede un secondo tocco entro 6 secondi
        if (cmd == "sblocca" && !WidgetPrefs.armato(app)) {
            WidgetPrefs.arma(app, true)
            aggiornaAsync(ctx, ids(ctx), scarica = false)
            return
        }
        WidgetPrefs.arma(app, false)
        WidgetPrefs.setAttesa(app, btn)
        val pending = goAsync()
        Thread {
            try {
                disegnaTutti(app, s)
                val err = TeslaApi.invia(app, cmd)
                if (err != null) {
                    WidgetPrefs.setAttesa(app, null)
                    WidgetPrefs.salvaCache(app, null, err)
                } else {
                    for (i in 0 until 6) {
                        Thread.sleep(5000)
                        val n = TeslaApi.aggiorna(app)
                        val c = n?.comando
                        if (c != null && !c.inCorso) break
                    }
                    WidgetPrefs.setAttesa(app, null)
                }
                disegnaTutti(app, TeslaApi.parse(WidgetPrefs.cache(app)))
            } catch (_: Throwable) {
            } finally { pending.finish() }
        }.start()
    }

    private fun disegnaTutti(app: Context, s: TeslaStato?) {
        val mgr = AppWidgetManager.getInstance(app)
        val esito = WidgetPrefs.ultimoEsito(app)
        for (id in ids(app)) mgr.updateAppWidget(id, costruisci(app, mgr, id, s, esito))
    }

    private fun aggiornaAsync(ctx: Context, ids: IntArray, scarica: Boolean) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        val app = ctx.applicationContext
        Thread {
            try {
                val s = if (scarica) TeslaApi.aggiorna(app) else TeslaApi.parse(WidgetPrefs.cache(app))
                val esito = WidgetPrefs.ultimoEsito(app)
                val mgr = AppWidgetManager.getInstance(app)
                for (id in ids) mgr.updateAppWidget(id, costruisci(app, mgr, id, s, esito))
            } catch (_: Throwable) {
            } finally { pending.finish() }
        }.start()
    }

    private fun costruisci(ctx: Context, mgr: AppWidgetManager, id: Int, s: TeslaStato?, esito: String): RemoteViews {
        val o = mgr.getAppWidgetOptions(id)
        // in verticale contano larghezza minima e altezza massima
        var wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        var hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        if (wDp <= 0 || hDp <= 0) { wDp = dimDefault.first; hDp = dimDefault.second }
        val rv = viste(ctx, s, esito, wDp, hDp)
        rv.setOnClickPendingIntent(R.id.widget_img, broadcast(ctx, ACTION_REFRESH, null, 0))
        return rv
    }

    protected fun px(ctx: Context, dp: Int): Int = (dp * ctx.resources.displayMetrics.density).roundToInt()

    protected fun broadcast(ctx: Context, action: String, btn: String?, code: Int): PendingIntent {
        val i = Intent(ctx, javaClass).setAction(action)
        if (btn != null) i.putExtra(EXTRA_BTN, btn)
        return PendingIntent.getBroadcast(ctx, javaClass.name.hashCode() + code, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_REFRESH = "com.marcofabbrini.teslachargecost.WIDGET_REFRESH"
        const val ACTION_CMD = "com.marcofabbrini.teslachargecost.WIDGET_CMD"
        const val EXTRA_BTN = "btn"
        private const val QUARTO = 15 * 60 * 1000L

        /** Riduce le dimensioni in pixel a un massimo di 1000 px di larghezza, mantenendo le proporzioni */
        fun scala(wPx: Int, hPx: Int): Pair<Int, Int> {
            val k = minOf(1f, 1000f / wPx)
            return (wPx * k).roundToInt() to (hPx * k).roundToInt()
        }

        private fun allarme(ctx: Context): PendingIntent {
            val i = Intent(ctx, TeslaWidgetTimer::class.java).setAction(ACTION_REFRESH)
            return PendingIntent.getBroadcast(ctx, 7, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        fun programma(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + QUARTO, QUARTO, allarme(ctx))
        }
        fun annulla(ctx: Context) {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(allarme(ctx))
        }
    }
}

class TeslaBatteryWidget : TeslaWidgetBase() {
    override val dimDefault = 250 to 110
    override fun viste(ctx: Context, s: TeslaStato?, esito: String, wDp: Int, hDp: Int): RemoteViews {
        val (w, h) = scala(px(ctx, wDp), px(ctx, hDp))
        return RemoteViews(ctx.packageName, R.layout.widget_image).apply {
            setImageViewBitmap(R.id.widget_img, WidgetRenderer.piccolo(ctx, s, esito, w, h))
        }
    }
}

class TeslaStatusWidget : TeslaWidgetBase() {
    override val dimDefault = 360 to 260

    override fun viste(ctx: Context, s: TeslaStato?, esito: String, wDp: Int, hDp: Int): RemoteViews {
        val barDp = 58
        val (w, h) = scala(px(ctx, wDp), px(ctx, (hDp - barDp).coerceAtLeast(90)))
        val rv = RemoteViews(ctx.packageName, R.layout.widget_controls)
        rv.setImageViewBitmap(R.id.widget_img, WidgetRenderer.grande(ctx, s, esito, w, h, sfondo = false))

        val attesa = WidgetPrefs.attesa(ctx)
        val bw = px(ctx, (wDp - 12) / 5); val bh = px(ctx, barDp - 6)
        fun st(btn: String, attivo: Boolean) = if (attesa == btn) 2 else if (attivo) 1 else 0
        val armato = WidgetPrefs.armato(ctx)
        val chiusa = s?.bloccata != false
        val voci = listOf(
            Triple(R.id.btn_serratura, "serratura", Triple(if (chiusa) "chiusa" else "aperta", if (armato) "Conferma" else if (chiusa) "Apri" else "Chiudi",
                if (armato) 3 else st("serratura", !chiusa))),
            Triple(R.id.btn_fari, "fari", Triple("fari", "Fari", st("fari", false))),
            Triple(R.id.btn_clima, "clima", Triple("clima", "Clima", st("clima", s?.clima == true))),
            Triple(R.id.btn_sportello, "sportello", Triple("sportello", "Sportello", st("sportello", false))),
            Triple(R.id.btn_sentinella, "sentinella", Triple("sentinella", "Sentinella", st("sentinella", s?.sentinella == true))),
        )
        voci.forEachIndexed { i, (vid, btn, d) ->
            rv.setImageViewBitmap(vid, WidgetRenderer.pulsante(ctx, d.first, d.second, d.third, bw, bh))
            rv.setOnClickPendingIntent(vid, broadcast(ctx, ACTION_CMD, btn, 10 + i))
        }
        return rv
    }
}

/** Riceve l'allarme ogni 15 minuti (circa) e chiede l'aggiornamento di tutti i widget. */
class TeslaWidgetTimer : android.content.BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = TeslaWidgetsAll.aggiornaTutti(ctx)
}

object TeslaWidgetsAll {
    private val classi = listOf(TeslaBatteryWidget::class.java, TeslaStatusWidget::class.java)
    fun attivi(ctx: Context): Boolean {
        val mgr = AppWidgetManager.getInstance(ctx)
        return classi.any { mgr.getAppWidgetIds(ComponentName(ctx, it)).isNotEmpty() }
    }
    /** Chiede a ogni tipo di widget presente di scaricare i dati e ridisegnarsi */
    fun aggiornaTutti(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for (c in classi) {
            if (mgr.getAppWidgetIds(ComponentName(ctx, c)).isEmpty()) continue
            ctx.sendBroadcast(Intent(ctx, c).setAction(TeslaWidgetBase.ACTION_REFRESH))
        }
    }
}
