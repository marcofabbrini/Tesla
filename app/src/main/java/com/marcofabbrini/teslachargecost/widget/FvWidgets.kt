package com.marcofabbrini.teslachargecost.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.marcofabbrini.teslachargecost.R
import kotlin.math.roundToInt

/**
 * Base dei widget del fotovoltaico: scarica i dati dal sito in un thread, disegna l'immagine e la mette nel widget.
 * Tocco = aggiorna. La sveglia ogni 5 minuti è la stessa dei widget della Tesla (vedi TeslaWidgetBase e TeslaWidgetTimer).
 */
abstract class FvWidgetBase : AppWidgetProvider() {
    abstract fun viste(ctx: Context, s: FvStato?, esito: String, wDp: Int, hDp: Int): RemoteViews
    abstract val dimDefault: Pair<Int, Int>

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        TeslaWidgetBase.programma(ctx)
        aggiornaAsync(ctx, ids, scarica = true)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opt: Bundle) =
        aggiornaAsync(ctx, intArrayOf(id), scarica = false)

    override fun onEnabled(ctx: Context) = TeslaWidgetBase.programma(ctx)
    override fun onDisabled(ctx: Context) { if (!TeslaWidgetsAll.attivi(ctx)) TeslaWidgetBase.annulla(ctx) }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            TeslaWidgetBase.ACTION_REFRESH -> aggiornaAsync(ctx, ids(ctx), scarica = true)
            else -> super.onReceive(ctx, intent)
        }
    }

    private fun ids(ctx: Context): IntArray = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, javaClass))

    private fun aggiornaAsync(ctx: Context, ids: IntArray, scarica: Boolean) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        val app = ctx.applicationContext
        Thread {
            try {
                val s = if (scarica) FvApi.aggiorna(app) else FvApi.parse(FvPrefs.cache(app))
                val esito = FvPrefs.ultimoEsito(app)
                val mgr = AppWidgetManager.getInstance(app)
                for (id in ids) mgr.updateAppWidget(id, costruisci(app, mgr, id, s, esito))
            } catch (_: Throwable) {
            } finally { pending.finish() }
        }.start()
    }

    private fun costruisci(ctx: Context, mgr: AppWidgetManager, id: Int, s: FvStato?, esito: String): RemoteViews {
        val o = mgr.getAppWidgetOptions(id)
        // in verticale contano larghezza minima e altezza massima
        var wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        var hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        if (wDp <= 0 || hDp <= 0) { wDp = dimDefault.first; hDp = dimDefault.second }
        val rv = viste(ctx, s, esito, wDp, hDp)
        val i = Intent(ctx, javaClass).setAction(TeslaWidgetBase.ACTION_REFRESH)
        rv.setOnClickPendingIntent(R.id.widget_img, PendingIntent.getBroadcast(ctx, javaClass.name.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        return rv
    }

    protected fun px(ctx: Context, dp: Int): Int = (dp * ctx.resources.displayMetrics.density).roundToInt()
}

class FvSmallWidget : FvWidgetBase() {
    override val dimDefault = 250 to 110
    override fun viste(ctx: Context, s: FvStato?, esito: String, wDp: Int, hDp: Int): RemoteViews {
        val (w, h) = TeslaWidgetBase.scala(px(ctx, wDp), px(ctx, hDp))
        return RemoteViews(ctx.packageName, R.layout.widget_image).apply {
            setImageViewBitmap(R.id.widget_img, FvRenderer.piccolo(ctx, s, esito, w, h))
        }
    }
}

class FvLargeWidget : FvWidgetBase() {
    override val dimDefault = 360 to 400
    override fun viste(ctx: Context, s: FvStato?, esito: String, wDp: Int, hDp: Int): RemoteViews {
        val (w, h) = TeslaWidgetBase.scala(px(ctx, wDp), px(ctx, hDp))
        return RemoteViews(ctx.packageName, R.layout.widget_image).apply {
            setImageViewBitmap(R.id.widget_img, FvRenderer.grande(ctx, s, esito, w, h))
        }
    }
}
