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

/** Base comune: scarica i dati dal sito in un thread, disegna l'immagine e la mette nel widget. Tocco = aggiorna. */
abstract class TeslaWidgetBase : AppWidgetProvider() {
    abstract fun disegna(ctx: Context, s: TeslaStato?, esito: String, w: Int, h: Int): android.graphics.Bitmap
    abstract val dimDefault: Pair<Int, Int>

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = aggiornaAsync(ctx, ids, scarica = true)

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opt: Bundle) =
        aggiornaAsync(ctx, intArrayOf(id), scarica = false)

    override fun onEnabled(ctx: Context) = programma(ctx)
    override fun onDisabled(ctx: Context) { if (!TeslaWidgetsAll.attivi(ctx)) annulla(ctx) }

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(ctx)
            aggiornaAsync(ctx, mgr.getAppWidgetIds(ComponentName(ctx, javaClass)), scarica = true)
            return
        }
        super.onReceive(ctx, intent)
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
                for (id in ids) mgr.updateAppWidget(id, viste(app, mgr, id, s, esito))
            } catch (_: Throwable) {
            } finally { pending.finish() }
        }.start()
    }

    private fun viste(ctx: Context, mgr: AppWidgetManager, id: Int, s: TeslaStato?, esito: String): RemoteViews {
        val o = mgr.getAppWidgetOptions(id)
        val d = ctx.resources.displayMetrics.density
        // in verticale contano larghezza minima e altezza massima
        var wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        var hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        if (wDp <= 0 || hDp <= 0) { wDp = dimDefault.first; hDp = dimDefault.second }
        var wPx = (wDp * d).roundToInt(); var hPx = (hDp * d).roundToInt()
        val k = minOf(1f, 1000f / wPx)
        wPx = (wPx * k).roundToInt(); hPx = (hPx * k).roundToInt()
        val rv = RemoteViews(ctx.packageName, R.layout.widget_image)
        rv.setImageViewBitmap(R.id.widget_img, disegna(ctx, s, esito, wPx, hPx))
        val i = Intent(ctx, javaClass).setAction(ACTION_REFRESH)
        rv.setOnClickPendingIntent(R.id.widget_img, PendingIntent.getBroadcast(ctx, javaClass.name.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        return rv
    }

    companion object {
        const val ACTION_REFRESH = "com.marcofabbrini.teslachargecost.WIDGET_REFRESH"
        private const val QUARTO = 15 * 60 * 1000L

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
    override fun disegna(ctx: Context, s: TeslaStato?, esito: String, w: Int, h: Int) = WidgetRenderer.piccolo(ctx, s, esito, w, h)
}

class TeslaStatusWidget : TeslaWidgetBase() {
    override val dimDefault = 360 to 220
    override fun disegna(ctx: Context, s: TeslaStato?, esito: String, w: Int, h: Int) = WidgetRenderer.grande(ctx, s, esito, w, h)
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
