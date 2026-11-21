package com.marcofabbrini.teslachargecost.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Widget del fotovoltaico, disegnati come immagini nello stile di quelli della Tesla.
 * Colori come sul sito: ambra = sole, verde acqua = batteria e immissione, azzurro = casa, rosa = prelievo dalla rete.
 */
object FvRenderer {
    private val SOLE = Color.rgb(231, 167, 101)
    private val BATT = Color.rgb(92, 196, 181)
    private val CASA = Color.rgb(127, 180, 230)
    private val RETE = Color.rgb(255, 131, 142)

    private fun n2(v: Double?) = if (v == null) "—" else String.format(Locale.ITALIAN, "%.2f", v)
    private fun n1(v: Double?) = if (v == null) "—" else String.format(Locale.ITALIAN, "%.1f", v)

    /** Sole stilizzato: cerchio pieno e otto raggi, centrato in (cx, cy) */
    private fun sole(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        val fi = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; setShadowLayer(r * .6f, 0f, 0f, col) }
        c.drawCircle(cx, cy, r * .55f, fi)
        val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1.5f, r * .16f); strokeCap = Paint.Cap.ROUND; color = col }
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            c.drawLine(cx + (cos(a) * r * .8).toFloat(), cy + (sin(a) * r * .8).toFloat(), cx + (cos(a) * r * 1.1).toFloat(), cy + (sin(a) * r * 1.1).toFloat(), st)
        }
    }

    private fun messaggio(ctx: Context, w: Int, h: Int, msg: String): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        WidgetRenderer.fondo(c, w.toFloat(), h.toFloat(), min(w, h) * .12f)
        val tf = WidgetRenderer.font(ctx, false)
        val size = min(h * .13f, w * .055f)
        sole(c, w * .12f, h * .5f, min(w * .07f, h * .22f), SOLE)
        val p = Paint().apply { textSize = size; typeface = tf }
        var line = ""; var yy = h * .42f
        for (wd in msg.split(" ")) {
            val t = if (line.isEmpty()) wd else "$line $wd"
            if (p.measureText(t) > w * .66f) { WidgetRenderer.testo(c, line, w * .24f, yy, size, WidgetRenderer.DIM, tf); yy += size * 1.25f; line = wd } else line = t
        }
        if (line.isNotEmpty()) WidgetRenderer.testo(c, line, w * .24f, yy, size, WidgetRenderer.DIM, tf)
        return bmp
    }

    private fun aggiornato(s: FvStato): String = (if (s.online) "" else "Dati non aggiornati · ") + WidgetRenderer.ora(s.ts)

    /** Widget piccolo: una colonna per impianto con carica della batteria e produzione attuale */
    fun piccolo(ctx: Context, s: FvStato?, esito: String, wPx: Int, hPx: Int): Bitmap {
        val w = wPx.coerceIn(200, 900); val h = (hPx * w / wPx.toFloat()).roundToInt().coerceIn(100, 500)
        if (s == null) return messaggio(ctx, w, h, if (esito.isBlank()) "Apri l'app Tesla Charge Cost > Impostazioni e collega il widget." else esito)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val wf = w.toFloat(); val hf = h.toFloat()
        WidgetRenderer.fondo(c, wf, hf, min(wf, hf) * .14f)
        val bold = WidgetRenderer.font(ctx, true); val reg = WidgetRenderer.font(ctx, false)
        val lista = s.impianti.take(2)
        val pad = min(wf, hf) * .10f
        val colW = (wf - pad * 2) / lista.size
        val nameSz = hf * .14f; val socSz = hf * .34f; val pvSz = hf * .15f
        for ((i, imp) in lista.withIndex()) {
            val x0 = pad + i * colW + (if (i > 0) pad * .45f else 0f)
            val yN = pad * .9f + nameSz
            WidgetRenderer.testo(c, imp.nome.uppercase(), x0, yN, nameSz, WidgetRenderer.DIM, reg, spacing = .08f)
            val col = WidgetRenderer.socColor(imp.soc?.roundToInt())
            val ySoc = yN + socSz * 1.05f
            WidgetRenderer.testo(c, (imp.soc?.roundToInt()?.toString() ?: "—") + "%", x0, ySoc, socSz, col, bold, glow = col)
            val yPv = ySoc + pvSz * 1.4f
            sole(c, x0 + pvSz * .5f, yPv - pvSz * .33f, pvSz * .5f, SOLE)
            WidgetRenderer.testo(c, n2(imp.pvKw) + " kW", x0 + pvSz * 1.4f, yPv, pvSz, WidgetRenderer.TEXT, reg)
            if (i > 0) {
                val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(45, 201, 232, 249); strokeWidth = max(1f, hf * .008f) }
                val lx = x0 - pad * .45f - pad * .25f
                c.drawLine(lx, pad * .9f, lx, hf - pad * 1.1f, ln)
            }
        }
        WidgetRenderer.testo(c, aggiornato(s), wf - pad * .8f, hf - hf * .07f, min(hf * .09f, wf * .04f), if (s.online) WidgetRenderer.ORA else WidgetRenderer.DANGER, reg, Paint.Align.RIGHT)
        WidgetRenderer.cornice(c, wf, hf, min(wf, hf) * .14f)
        return bmp
    }

    /** Widget grande: un pannello per impianto con batteria, flussi di potenza, curva di oggi e totali */
    fun grande(ctx: Context, s: FvStato?, esito: String, wPx: Int, hPx: Int): Bitmap {
        val w = wPx.coerceIn(300, 1000); val h = (hPx * w / wPx.toFloat()).roundToInt().coerceIn(200, 1200)
        if (s == null) return messaggio(ctx, w, h, if (esito.isBlank()) "Apri l'app Tesla Charge Cost > Impostazioni e collega il widget." else esito)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val wf = w.toFloat(); val hf = h.toFloat(); val mn = min(wf, hf)
        WidgetRenderer.fondo(c, wf, hf, mn * .09f)
        val bold = WidgetRenderer.font(ctx, true); val reg = WidgetRenderer.font(ctx, false)
        val lista = s.impianti.take(2)
        val pad = mn * .07f
        val gap = pad * .5f
        val tsSz = mn * .042f
        val bottom = pad * .35f + tsSz * 1.7f
        val ph = (hf - pad - bottom - gap * (lista.size - 1)) / lista.size
        val pw = wf - pad * 2
        val pi = ph * .07f   // margine interno del pannello

        for ((i, imp) in lista.withIndex()) {
            val px = pad; val py = pad + i * (ph + gap)
            // pannello
            val r = RectF(px, py, px + pw, py + ph)
            val rr = mn * .035f
            c.drawRoundRect(r, rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(26, 255, 255, 255) })
            c.drawRoundRect(r, rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1f, mn * .003f); color = Color.argb(45, 201, 232, 249) })
            val ix = px + pi; val iw = pw - pi * 2

            // intestazione: nome e produzione di oggi
            val hs = ph * .115f
            val yh = py + pi + hs * .9f
            WidgetRenderer.testo(c, imp.nome.uppercase(), ix, yh, hs, WidgetRenderer.TEXT, bold, spacing = .08f)
            WidgetRenderer.testo(c, "OGGI " + n1(imp.oggiPv) + " kWh", ix + iw, yh, hs * .85f, SOLE, bold, Paint.Align.RIGHT, spacing = .04f)

            // blocco centrale: batteria a sinistra, flussi a destra
            val midTop = py + ph * .24f; val midH = ph * .40f
            val leftW = iw * .44f
            val bw = min(leftW * .9f, midH * 1.1f)
            val soc = imp.soc?.roundToInt()
            val carica = (imp.battKw ?: 0.0) > 0.02
            WidgetRenderer.batteria(c, ix - bw * .03f, midTop, bw, soc, null, carica)
            val col = WidgetRenderer.socColor(soc)
            val socSz = midH * .40f
            WidgetRenderer.testo(c, (soc?.toString() ?: "—") + "%", ix, midTop + bw * .46f + socSz * .95f, socSz, col, bold, glow = col)

            val rx0 = ix + leftW + iw * .04f
            val rowH = midH / 4f
            val rs = ph * .075f
            fun riga(k: Int, etichetta: String, valore: String, colore: Int) {
                val y = midTop + rowH * k + rowH * .72f
                c.drawCircle(rx0 + rs * .3f, y - rs * .32f, rs * .26f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colore; setShadowLayer(rs * .3f, 0f, 0f, colore) })
                WidgetRenderer.testo(c, etichetta, rx0 + rs * .9f, y, rs, WidgetRenderer.DIM, reg)
                WidgetRenderer.testo(c, valore, ix + iw, y, rs * 1.05f, WidgetRenderer.TEXT, bold, Paint.Align.RIGHT)
            }
            riga(0, "Solare", n2(imp.pvKw) + " kW", SOLE)
            riga(1, "Casa", n2(imp.casaKw) + " kW", CASA)
            val rete = imp.reteKw
            riga(2, when { rete == null -> "Rete"; rete > 0.02 -> "Prelievo"; rete < -0.02 -> "Immissione"; else -> "Rete" },
                n2(rete?.let { abs(it) }) + " kW", if (rete != null && rete > 0.02) RETE else BATT)
            val bk = imp.battKw
            riga(3, when { bk == null -> "Batteria"; bk > 0.02 -> "In carica"; bk < -0.02 -> "In scarica"; else -> "Batteria ferma" },
                n2(bk?.let { abs(it) }) + " kW", BATT)

            // curva di produzione di oggi
            val cy0 = py + ph * .68f; val chh = ph * .19f
            val base = cy0 + chh
            c.drawLine(ix, base, ix + iw, base, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 201, 232, 249); strokeWidth = max(1f, mn * .003f) })
            if (imp.curva.size >= 2) {
                val pmax = max(1000, imp.curva.maxOf { it.second }).toFloat()
                val linea = Path(); val area = Path()
                var primo = true; var xl = ix
                for ((m, wt) in imp.curva) {
                    val x = ix + iw * (m.coerceIn(0, 1440) / 1440f)
                    val y = base - chh * (wt.coerceAtLeast(0) / pmax)
                    if (primo) { linea.moveTo(x, y); area.moveTo(x, base); area.lineTo(x, y); primo = false } else { linea.lineTo(x, y); area.lineTo(x, y) }
                    xl = x
                }
                area.lineTo(xl, base); area.close()
                c.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, cy0, 0f, base, Color.argb(120, 231, 167, 101), Color.argb(0, 231, 167, 101), Shader.TileMode.CLAMP) })
                c.drawPath(linea, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(2f, mn * .006f); strokeJoin = Paint.Join.ROUND; color = SOLE; setShadowLayer(mn * .012f, 0f, 0f, SOLE) })
            } else {
                WidgetRenderer.testo(c, "Curva di oggi non ancora disponibile", ix, cy0 + chh * .65f, ph * .06f, WidgetRenderer.DIM, reg)
            }

            // totali
            val fs = ph * .068f
            val tot = listOfNotNull(imp.ieriPv?.let { "Ieri " + n1(it) + " kWh" }, imp.mesePv?.let { "Mese " + n1(it) + " kWh" }).joinToString("  ·  ")
            if (tot.isNotEmpty()) WidgetRenderer.testo(c, tot, ix, py + ph - pi * .8f, fs, WidgetRenderer.ICE, reg)
        }
        WidgetRenderer.testo(c, aggiornato(s), wf - pad, hf - pad * .35f - tsSz * .3f, tsSz, if (s.online) WidgetRenderer.ORA else WidgetRenderer.DANGER, reg, Paint.Align.RIGHT)
        WidgetRenderer.cornice(c, wf, hf, mn * .09f)
        return bmp
    }
}
