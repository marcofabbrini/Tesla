package com.marcofabbrini.teslachargecost.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.marcofabbrini.teslachargecost.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Disegna i widget come immagini, nello stile di marcofabbrini.com: fondo blu notte, batteria a segmenti
 * (rosso -> verde) con fulmine in ricarica, richiami azzurri luminosi con le pressioni accanto alle ruote.
 */
object WidgetRenderer {
    private val BG1 = Color.rgb(13, 20, 34)
    private val BG2 = Color.rgb(5, 8, 14)
    private val TEXT = Color.rgb(238, 243, 248)
    private val DIM = Color.rgb(143, 161, 179)
    private val ICE = Color.rgb(201, 232, 249)
    private val RING = Color.rgb(223, 241, 255)
    private val GLOW = Color.rgb(120, 200, 255)
    private val DANGER = Color.rgb(255, 131, 142)
    private val TEAL = Color.rgb(92, 196, 181)
    private val ACCENT = Color.rgb(231, 167, 101)

    private var car: Bitmap? = null
    private fun carBitmap(ctx: Context): Bitmap? {
        if (car == null) car = runCatching { BitmapFactory.decodeResource(ctx.resources, R.drawable.widget_car) }.getOrNull()
        return car
    }
    private fun font(ctx: Context, bold: Boolean): Typeface =
        runCatching { ctx.resources.getFont(if (bold) R.font.rajdhani_bold else R.font.rajdhani_semibold) }.getOrNull()
            ?: Typeface.create("sans-serif-condensed", if (bold) Typeface.BOLD else Typeface.NORMAL)

    /** Colore della carica: tinta dal rosso (0%) al verde (100%), come sul sito */
    fun hsl(h: Float, s: Float, l: Float): Int {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val hp = (h % 360) / 60f
        val x = c * (1 - kotlin.math.abs(hp % 2 - 1))
        val (r1, g1, b1) = when {
            hp < 1 -> Triple(c, x, 0f); hp < 2 -> Triple(x, c, 0f); hp < 3 -> Triple(0f, c, x)
            hp < 4 -> Triple(0f, x, c); hp < 5 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        val m = l - c / 2
        return Color.rgb(((r1 + m) * 255).roundToInt(), ((g1 + m) * 255).roundToInt(), ((b1 + m) * 255).roundToInt())
    }
    fun socColor(soc: Int?): Int = hsl((soc ?: 0).coerceIn(0, 100) * 1.2f, .85f, .56f)

    private fun fondo(c: Canvas, w: Float, h: Float, r: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w * .4f, h, BG1, BG2, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(0f, 0f, w, h), r, r, p)
        p.shader = RadialGradient(w * .85f, h * 1.1f, w * .7f, Color.argb(46, 231, 167, 101), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(0f, 0f, w, h), r, r, p)
        val b = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1.5f, w / 400f); color = Color.argb(40, 201, 232, 249) }
        c.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), r, r, b)
    }

    /** Batteria: involucro bianco arrotondato, quattro segmenti inclinati, segno del limite, fulmine. Geometria su 200 x 100. */
    fun batteria(c: Canvas, x: Float, y: Float, w: Float, soc: Int?, limite: Int?, inCarica: Boolean) {
        val s = w / 200f
        fun r(l: Float, t: Float, rr: Float, b: Float) = RectF(x + l * s, y + t * s, x + rr * s, y + b * s)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val pct = (soc ?: 0).coerceIn(0, 100)
        val h = pct * 1.2f
        p.style = Paint.Style.FILL; p.color = Color.argb(185, 5, 8, 14)
        c.drawRoundRect(r(8f, 16f, 164f, 84f), 20 * s, 20 * s, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 5 * s; p.color = if (pct <= 20) DANGER else Color.rgb(233, 238, 243)
        p.setShadowLayer(6 * s, 0f, 0f, Color.argb(90, 233, 238, 243))
        c.drawRoundRect(r(8f, 16f, 164f, 84f), 20 * s, 20 * s, p)
        p.clearShadowLayer(); p.style = Paint.Style.FILL; p.color = Color.rgb(215, 221, 228)
        c.drawRoundRect(r(167f, 37f, 181f, 63f), 6 * s, 6 * s, p)
        val x0 = 22f; val y0 = 27f; val hh = 46f; val ww = 24f; val sl = 6f; val gap = 7f
        for (i in 0 until 4) {
            val sx = x0 + i * (ww + gap)
            val path = Path().apply {
                moveTo(x + (sx + sl) * s, y + y0 * s); lineTo(x + (sx + ww + sl) * s, y + y0 * s)
                lineTo(x + (sx + ww) * s, y + (y0 + hh) * s); lineTo(x + sx * s, y + (y0 + hh) * s); close()
            }
            val slot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(14, 255, 255, 255) }
            c.drawPath(path, slot)
            val lv = ((pct - i * 25) / 25f).coerceIn(0f, 1f)
            if (lv > 0f) {
                val c1 = hsl(h, .80f, .30f + i * .09f); val c2 = hsl(h, .85f, .40f + i * .09f)
                val seg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(x + sx * s, y + (y0 + hh) * s, x + (sx + ww + sl) * s, y + y0 * s, c1, c2, Shader.TileMode.CLAMP)
                    setShadowLayer(5 * s, 0f, 0f, c2)
                }
                c.save()
                c.clipRect(x + (sx - 2) * s, y + (y0 - 2) * s, x + (sx - 2 + (ww + sl + 4) * lv) * s, y + (y0 + hh + 2) * s)
                c.drawPath(path, seg)
                c.restore()
            }
        }
        if (limite != null) {
            val lx = x + (x0 + (4 * (ww + gap) - gap + sl) * limite / 100f) * s
            val tri = Path().apply { moveTo(lx - 5 * s, y + 6 * s); lineTo(lx + 5 * s, y + 6 * s); lineTo(lx, y + 13 * s); close() }
            c.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(205, 233, 238, 243) })
        }
        val bolt = Path().apply {
            moveTo(x + 150 * s, y + 29 * s); lineTo(x + 134 * s, y + 52 * s); lineTo(x + 144 * s, y + 52 * s)
            lineTo(x + 139 * s, y + 72 * s); lineTo(x + 156 * s, y + 47 * s); lineTo(x + 146 * s, y + 47 * s); close()
        }
        val bp = Paint(Paint.ANTI_ALIAS_FLAG)
        if (inCarica) { bp.color = hsl(h, .9f, .62f); bp.setShadowLayer(7 * s, 0f, 0f, bp.color) } else bp.color = Color.argb(20, 255, 255, 255)
        c.drawPath(bolt, bp)
    }

    private fun testo(c: Canvas, t: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface, align: Paint.Align = Paint.Align.LEFT, glow: Int? = null, spacing: Float = 0f) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = tf; textAlign = align; letterSpacing = spacing }
        if (glow != null) p.setShadowLayer(size * .35f, 0f, 0f, glow)
        c.drawText(t, x, y, p)
    }

    private fun f1(v: Double?) = if (v == null) "—" else String.format(Locale.ITALIAN, "%.1f", v)
    private fun stato(s: TeslaStato): String = when {
        s.inCarica -> "In carica" + (s.potenzaKw?.let { " · " + f1(it) + " kW" } ?: "")
        s.carica == "Complete" -> "Carica completata"
        s.carica == "Stopped" -> "Ricarica in pausa"
        s.carica == "Disconnected" || s.carica.isEmpty() -> if (s.casa) "A casa · scollegata" else "Scollegata"
        else -> s.carica
    }
    private fun ora(ts: Long?) = if (ts == null) "" else SimpleDateFormat("dd/MM HH:mm", Locale.ITALIAN).format(Date(ts))

    private fun messaggio(ctx: Context, w: Int, h: Int, msg: String): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        fondo(c, w.toFloat(), h.toFloat(), min(w, h) * .12f)
        val tf = font(ctx, false)
        batteria(c, w * .06f, h * .5f - w * .14f, w * .28f, null, null, false)
        val words = msg.split(" "); var line = ""; var yy = h * .38f; val size = min(h * .13f, w * .055f)
        val p = Paint().apply { textSize = size; typeface = tf }
        for (wd in words) {
            val t = if (line.isEmpty()) wd else "$line $wd"
            if (p.measureText(t) > w * .58f) { testo(c, line, w * .38f, yy, size, DIM, tf); yy += size * 1.25f; line = wd } else line = t
        }
        if (line.isNotEmpty()) testo(c, line, w * .38f, yy, size, DIM, tf)
        return bmp
    }

    /** Widget piccolo: batteria, percentuale, autonomia, stato */
    fun piccolo(ctx: Context, s: TeslaStato?, esito: String, wPx: Int, hPx: Int): Bitmap {
        val w = wPx.coerceIn(200, 900); val h = (hPx * w / wPx.toFloat()).roundToInt().coerceIn(100, 900)
        if (s == null) return messaggio(ctx, w, h, if (esito.isBlank()) "Apri l'app Tesla Charge Cost > Impostazioni e collega il widget." else esito)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        fondo(c, w.toFloat(), h.toFloat(), min(w, h) * .14f)
        val bold = font(ctx, true); val reg = font(ctx, false)
        val col = socColor(s.soc)
        val bw = min(w * .44f, h * 1.25f)
        batteria(c, w * .05f, h * .5f - bw * .5f + bw * .02f, bw, s.soc, s.limite, s.inCarica)
        val tx = w * .05f + bw + w * .05f
        val big = min(h * .40f, (w - tx) * .42f)
        testo(c, (s.soc?.toString() ?: "—") + "%", tx, h * .5f + big * .1f, big, col, bold, glow = col)
        testo(c, (s.autonomiaKm?.let { "$it km" } ?: ""), tx, h * .5f + big * .1f + big * .55f, big * .42f, TEXT, reg)
        testo(c, stato(s).uppercase(), tx, h * .5f - big * .82f, big * .26f, if (s.inCarica) TEAL else DIM, reg, spacing = .08f)
        testo(c, ora(s.ts), w - w * .05f, h - h * .08f, min(h * .09f, w * .04f), Color.argb(150, 143, 161, 179), reg, Paint.Align.RIGHT)
        return bmp
    }

    /** Widget grande: foto dell'auto, batteria, pressioni accanto alle ruote, stato */
    fun grande(ctx: Context, s: TeslaStato?, esito: String, wPx: Int, hPx: Int, sfondo: Boolean = true): Bitmap {
        val w = wPx.coerceIn(300, 1000); val h = (hPx * w / wPx.toFloat()).roundToInt().coerceIn(150, 1000)
        if (s == null) return messaggio(ctx, w, h, if (esito.isBlank()) "Apri l'app Tesla Charge Cost > Impostazioni e collega il widget." else esito)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val rad = min(w, h) * .09f
        if (sfondo) fondo(c, w.toFloat(), h.toFloat(), rad)
        val bold = font(ctx, true); val reg = font(ctx, false)
        val col = socColor(s.soc)
        val pad = min(w, h) * .07f

        // auto: in basso, centrata, con i bordi sfumati nel fondo
        val img = carBitmap(ctx)
        var ix = 0f; var iy = 0f; var iw = 0f; var ih = 0f
        if (img != null) {
            ih = h * .64f; iw = ih * img.width / img.height
            if (iw > w * .92f) { iw = w * .92f; ih = iw * img.height / img.width }
            ix = (w - iw) / 2f; iy = h - ih - h * .1f
            c.save(); c.clipRect(RectF(2f, 2f, w - 2f, h - 2f))
            c.drawBitmap(img, null, RectF(ix, iy, ix + iw, iy + ih), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            // sfumatura dei bordi della foto
            val fade = Paint(Paint.ANTI_ALIAS_FLAG)
            fade.shader = LinearGradient(ix, 0f, ix + iw * .12f, 0f, BG2, Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix, iy, ix + iw * .12f, iy + ih, fade)
            fade.shader = LinearGradient(ix + iw, 0f, ix + iw * .88f, 0f, BG2, Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix + iw * .88f, iy, ix + iw, iy + ih, fade)
            fade.shader = LinearGradient(0f, iy, 0f, iy + ih * .2f, Color.rgb(9, 14, 24), Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix, iy, ix + iw, iy + ih * .2f, fade)
            fade.shader = LinearGradient(0f, iy + ih, 0f, iy + ih * .85f, BG2, Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix, iy + ih * .85f, ix + iw, iy + ih, fade)
            c.restore()
        }

        // in alto a sinistra: batteria, percentuale, autonomia e stato
        val bw = min(w * .26f, h * .48f)
        batteria(c, pad, pad * .7f, bw, s.soc, s.limite, s.inCarica)
        val tx = pad + bw + pad * .5f
        val big = min(h * .17f, w * .085f)
        testo(c, (s.soc?.toString() ?: "—") + "%", tx, pad * .7f + bw * .36f + big * .35f, big, col, bold, glow = col)
        testo(c, listOfNotNull(s.autonomiaKm?.let { "$it km" }, stato(s)).joinToString(" · "), tx, pad * .7f + bw * .36f + big * .35f + big * .55f, big * .36f, if (s.inCarica) TEAL else TEXT, reg)

        // in alto a destra: chiusura e temperatura
        val sz = min(h * .055f, w * .028f)
        val dx = w - pad
        testo(c, when (s.bloccata) { true -> "CHIUSA"; false -> "APERTA"; else -> "" }, dx, pad * .7f + sz * 1.6f, sz * 1.15f, if (s.bloccata == false) DANGER else ICE, reg, Paint.Align.RIGHT, spacing = .12f)
        testo(c, listOfNotNull(s.tempInt?.let { f1(it) + "° dentro" }, s.tempEst?.let { f1(it) + "° fuori" }).joinToString(" · "), dx, pad * .7f + sz * 3.1f, sz, DIM, reg, Paint.Align.RIGHT)
        testo(c, ora(s.ts), dx, pad * .7f + sz * 4.5f, sz * .9f, Color.argb(150, 143, 161, 179), reg, Paint.Align.RIGHT)
        val cm = s.comando
        if (cm != null && cm.ts != null && System.currentTimeMillis() - cm.ts < 5 * 60000) {
            val (t, col2) = when (cm.stato) {
                "in_coda", "inviato" -> nomeComando(cm.cmd) + " · in corso…" to ACCENT
                "ok" -> nomeComando(cm.cmd) + " · fatto" to TEAL
                "errore" -> nomeComando(cm.cmd) + " · non riuscito" to DANGER
                else -> nomeComando(cm.cmd) + " · nessuna risposta" to DANGER
            }
            testo(c, t, dx, pad * .7f + sz * 5.9f, sz * .95f, col2, reg, Paint.Align.RIGHT)
        }

        // richiami: pressioni accanto alle ruote (coordinate della foto 1580 x 690)
        if (img != null) {
            fun pt(px: Float, py: Float) = Pair(ix + px * iw / 1580f, iy + py * ih / 690f)
            val g = s.gomme
            fun warn(a: Double?, b: Double?) = (a != null && a < 2.6) || (b != null && b < 2.6)
            fun valTxt(a: Double?, b: Double?) = if (a == null && b == null) "—" else f1(a) + " · " + f1(b) + " bar"
            richiamo(c, pt(650f, 495f), Pair(w * .14f, h - pad * .55f), "ANTERIORI", valTxt(g.getOrNull(0), g.getOrNull(1)), warn(g.getOrNull(0), g.getOrNull(1)), sz, reg, bold, Paint.Align.LEFT)
            richiamo(c, pt(1415f, 432f), Pair(w * .86f, h - pad * .55f), "POSTERIORI", valTxt(g.getOrNull(2), g.getOrNull(3)), warn(g.getOrNull(2), g.getOrNull(3)), sz, reg, bold, Paint.Align.RIGHT)
        }
        return bmp
    }

    /** Anello azzurro luminoso sul punto, linea sottile, etichetta (titolo piccolo + valore) */
    private fun richiamo(c: Canvas, dot: Pair<Float, Float>, end: Pair<Float, Float>, titolo: String, valore: String, warn: Boolean, sz: Float, reg: Typeface, bold: Typeface, align: Paint.Align) {
        val ring = if (warn) Color.rgb(255, 179, 186) else RING
        val glow = if (warn) DANGER else GLOW
        val r = sz * .75f
        val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(215, 238, 243, 248); strokeWidth = max(1.5f, sz * .09f); setShadowLayer(sz * .25f, 0f, 0f, GLOW) }
        // linea: dal bordo dell'anello verso la riga dell'etichetta, poi in orizzontale
        val kx = end.first; val ky = end.second - sz * 2.6f
        c.drawLine(dot.first, dot.second + r, dot.first, ky, ln)
        c.drawLine(dot.first, ky, kx, ky, ln)
        c.drawCircle(kx, ky, sz * .14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT })
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(28, 173, 222, 255) }
        c.drawCircle(dot.first, dot.second, r * 1.8f, halo)
        val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = r * .32f; color = ring; setShadowLayer(r * .9f, 0f, 0f, glow) }
        c.drawCircle(dot.first, dot.second, r, rp)
        c.drawCircle(dot.first, dot.second, r * .38f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; setShadowLayer(r * .5f, 0f, 0f, GLOW) })
        testo(c, titolo, kx, ky + sz * 1.25f, sz * .82f, Color.argb(200, 201, 232, 249), reg, align, spacing = .14f)
        testo(c, valore, kx, ky + sz * 2.55f, sz * 1.25f, if (warn) Color.rgb(255, 154, 163) else Color.WHITE, bold, align)
    }

    fun nomeComando(c: String) = when (c) {
        "blocca" -> "Chiusura"; "sblocca" -> "Apertura"; "fari" -> "Fari"; "clima_on" -> "Clima acceso"; "clima_off" -> "Clima spento"
        "sportello" -> "Sportello"; "sentinella_on" -> "Sentinella attiva"; "sentinella_off" -> "Sentinella spenta"; else -> c
    }

    /** Pulsante dei comandi: vetro scuro, icona e scritta. stato: 0 normale, 1 attivo (azzurro), 2 in corso (ambra), 3 da confermare (rosso) */
    fun pulsante(ctx: Context, tipo: String, etichetta: String, stato: Int, wPx: Int, hPx: Int): Bitmap {
        val w = wPx.coerceIn(60, 400); val h = hPx.coerceIn(50, 300)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val col = when (stato) { 1 -> Color.rgb(127, 214, 255); 2 -> ACCENT; 3 -> DANGER; else -> ICE }
        val m = min(w, h) * .06f
        val box = RectF(m, m, w - m, h - m)
        val r = min(w, h) * .26f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(20, 29, 46), Color.rgb(8, 12, 20), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(box, r, r, bg)
        val br = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = max(1.5f, min(w, h) * .025f)
            color = if (stato == 0) Color.argb(60, 201, 232, 249) else col
            if (stato != 0) setShadowLayer(min(w, h) * .08f, 0f, 0f, col)
        }
        c.drawRoundRect(box, r, r, br)
        val labH = h * .24f
        val isz = min(w * .42f, (h - labH) * .52f)
        icona(c, tipo, w / 2f, m + (h - 2 * m - labH) * .55f, isz, if (stato == 0) Color.rgb(233, 238, 243) else col, stato != 0)
        testo(c, etichetta.uppercase(), w / 2f, h - m - labH * .38f, min(labH * .62f, w * .13f), if (stato == 0) DIM else col, font(ctx, false), Paint.Align.CENTER, spacing = .06f)
        return bmp
    }

    /** Icone semplici su griglia 24 x 24 centrata in (cx, cy), lato s */
    private fun icona(c: Canvas, tipo: String, cx: Float, cy: Float, s: Float, col: Int, glow: Boolean) {
        val k = s / 24f
        fun X(v: Float) = cx + (v - 12f) * k
        fun Y(v: Float) = cy + (v - 12f) * k
        val st = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f * k; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = col
            if (glow) setShadowLayer(3f * k, 0f, 0f, col)
        }
        val fi = Paint(st).apply { style = Paint.Style.FILL }
        when (tipo) {
            "chiusa", "aperta" -> {
                c.drawRoundRect(RectF(X(5f), Y(11f), X(19f), Y(21f)), 2.5f * k, 2.5f * k, st)
                c.drawCircle(X(12f), Y(15.5f), 1.4f * k, fi)
                val a = Path()
                if (tipo == "chiusa") { a.moveTo(X(8f), Y(11f)); a.lineTo(X(8f), Y(7.5f)); a.cubicTo(X(8f), Y(2.5f), X(16f), Y(2.5f), X(16f), Y(7.5f)); a.lineTo(X(16f), Y(11f)) }
                else { a.moveTo(X(8f), Y(11f)); a.lineTo(X(8f), Y(7.5f)); a.cubicTo(X(8f), Y(2.5f), X(16f), Y(2.5f), X(16f), Y(6.5f)) }
                c.drawPath(a, st)
            }
            "fari" -> {
                val p = Path().apply { moveTo(X(11f), Y(5f)); cubicTo(X(4f), Y(5f), X(4f), Y(19f), X(11f), Y(19f)); close() }
                c.drawPath(p, st)
                for (yy in floatArrayOf(7f, 10.3f, 13.7f, 17f)) c.drawLine(X(14f), Y(yy), X(21f), Y(yy + 1.2f), st)
            }
            "clima" -> {
                c.drawCircle(X(12f), Y(12f), 1.8f * k, fi)
                for (i in 0 until 3) {
                    c.save(); c.rotate(i * 120f, X(12f), Y(12f))
                    val p = Path().apply { moveTo(X(12f), Y(10f)); cubicTo(X(9f), Y(7f), X(9f), Y(3f), X(12.5f), Y(3f)); cubicTo(X(16f), Y(3f), X(15.5f), Y(7f), X(13.6f), Y(10.6f)) }
                    c.drawPath(p, st); c.restore()
                }
            }
            "sportello" -> {
                c.drawRoundRect(RectF(X(4f), Y(4f), X(20f), Y(20f)), 4f * k, 4f * k, st)
                val b = Path().apply { moveTo(X(13.5f), Y(6.5f)); lineTo(X(8.5f), Y(13f)); lineTo(X(12f), Y(13f)); lineTo(X(10.5f), Y(17.5f)); lineTo(X(15.5f), Y(11f)); lineTo(X(12f), Y(11f)); close() }
                c.drawPath(b, fi)
            }
            "sentinella" -> {
                val p = Path().apply { moveTo(X(2.5f), Y(12f)); cubicTo(X(6f), Y(5.5f), X(18f), Y(5.5f), X(21.5f), Y(12f)); cubicTo(X(18f), Y(18.5f), X(6f), Y(18.5f), X(2.5f), Y(12f)); close() }
                c.drawPath(p, st)
                c.drawCircle(X(12f), Y(12f), 3.2f * k, st)
                c.drawCircle(X(12f), Y(12f), 1.2f * k, fi)
            }
        }
    }
}
