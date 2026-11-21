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
    private fun tesla(ctx: Context): Typeface = runCatching { ctx.resources.getFont(R.font.tesla_title) }.getOrNull() ?: font(ctx, true)
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
        p.shader = LinearGradient(0f, 0f, w * .4f, h, Color.rgb(8, 10, 14), Color.BLACK, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(0f, 0f, w, h), r, r, p)
        p.shader = RadialGradient(w * .85f, h * 1.1f, w * .7f, Color.argb(26, 140, 190, 230), Color.TRANSPARENT, Shader.TileMode.CLAMP)
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
    private fun ora(ts: Long?): String {
        if (ts == null) return ""
        val oggi = SimpleDateFormat("yyyyMMdd", Locale.ITALIAN)
        val stesso = oggi.format(Date(ts)) == oggi.format(Date())
        return "Aggiornato " + SimpleDateFormat(if (stesso) "'alle' HH:mm" else "dd/MM 'alle' HH:mm", Locale.ITALIAN).format(Date(ts))
    }
    private val ORA = Color.argb(225, 214, 226, 238)

    /** Cornice ambra sottile con angoli più spessi e luminosi (come le staffe agli angoli) */
    fun cornice(c: Canvas, w: Float, h: Float, rad: Float) {
        val m = min(w, h) * .022f
        val r = RectF(m, m, w - m, h - m)
        val rr = rad * .85f
        val sottile = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = max(1.5f, min(w, h) * .006f); color = Color.argb(170, 231, 167, 101) }
        c.drawRoundRect(r, rr, rr, sottile)
        val spesso = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = max(3f, min(w, h) * .016f); strokeCap = Paint.Cap.ROUND; color = Color.rgb(236, 178, 104)
            setShadowLayer(min(w, h) * .02f, 0f, 0f, Color.argb(150, 231, 167, 101))
        }
        val l = rr + min(w, h) * .05f
        // un tratto per angolo: lato, curva, lato
        fun ang(x0: Float, y0: Float, sx: Float, sy: Float) {
            val p = Path()
            p.moveTo(x0 + sx * l, y0)
            p.lineTo(x0 + sx * rr, y0)
            p.quadTo(x0, y0, x0, y0 + sy * rr)
            p.lineTo(x0, y0 + sy * l)
            c.drawPath(p, spesso)
        }
        ang(r.left, r.top, 1f, 1f); ang(r.right, r.top, -1f, 1f); ang(r.left, r.bottom, 1f, -1f); ang(r.right, r.bottom, -1f, -1f)
    }

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

    /** Lucchetto chiuso (azzurro) o aperto (rosso); null = stato non noto */
    fun lucchetto(c: Canvas, cx: Float, cy: Float, size: Float, chiusa: Boolean?) {
        val col = when (chiusa) { true -> Color.rgb(127, 214, 255); false -> DANGER; else -> DIM }
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, size * .9f, Color.argb(if (chiusa == null) 0 else 60, Color.red(col), Color.green(col), Color.blue(col)), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, size * .9f, halo)
        icona(c, if (chiusa == false) "aperta" else "chiusa", cx, cy, size, col, chiusa != null)
    }

    /** Widget piccolo: batteria, percentuale, autonomia, stato, lucchetto */
    fun piccolo(ctx: Context, s: TeslaStato?, esito: String, wPx: Int, hPx: Int): Bitmap {
        val w = wPx.coerceIn(200, 900); val h = (hPx * w / wPx.toFloat()).roundToInt().coerceIn(100, 900)
        if (s == null) return messaggio(ctx, w, h, if (esito.isBlank()) "Apri l'app Tesla Charge Cost > Impostazioni e collega il widget." else esito)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        fondo(c, w.toFloat(), h.toFloat(), min(w, h) * .14f)
        val bold = font(ctx, true); val reg = font(ctx, false)
        val col = socColor(s.soc)
        val pad = min(w, h) * .09f
        val bw = min(w * .36f, h * 1.2f)
        batteria(c, pad * .7f, h * .5f - bw * .25f, bw, s.soc, s.limite, s.inCarica)
        val tx = pad * .7f + bw + w * .035f
        val lk = min(h * .34f, w * .11f)
        val big = min(h * .50f, (w - tx - lk - pad) * .48f)
        testo(c, stato(s).uppercase(), tx, h * .5f - big * .72f, big * .30f, if (s.inCarica) TEAL else DIM, reg, spacing = .06f)
        testo(c, (s.soc?.toString() ?: "—") + "%", tx, h * .5f + big * .32f, big, col, bold, glow = col)
        testo(c, s.autonomiaKm?.let { "$it km" } ?: "", tx, h * .5f + big * .32f + big * .52f, big * .40f, TEXT, reg)
        lucchetto(c, w - pad - lk * .5f, h * .5f - lk * .2f, lk, s.bloccata)
        testo(c, ora(s.ts), w - pad * .8f, h - h * .09f, min(h * .11f, w * .045f), ORA, reg, Paint.Align.RIGHT)
        cornice(c, w.toFloat(), h.toFloat(), min(w, h) * .14f)
        return bmp
    }

    /** Widget grande: foto dell'auto su fondo nero esteso a tutto il widget, batteria, lucchetto, pressioni accanto alle ruote */
    fun grande(ctx: Context, s: TeslaStato?, esito: String, wPx: Int, hPx: Int, sfondo: Boolean = true): Bitmap {
        val w = wPx.coerceIn(300, 1000); val h = (hPx * w / wPx.toFloat()).roundToInt().coerceIn(150, 1000)
        if (s == null) return messaggio(ctx, w, h, if (esito.isBlank()) "Apri l'app Tesla Charge Cost > Impostazioni e collega il widget." else esito)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val wf = w.toFloat(); val hf = h.toFloat()
        val rad = min(w, h) * .09f
        // fondo nero come quello della foto, esteso a tutto il widget
        c.drawRoundRect(RectF(0f, 0f, wf, hf), rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
        val bold = font(ctx, true); val reg = font(ctx, false)
        val col = socColor(s.soc)
        val pad = min(w, h) * .065f
        val sz = min(h * .068f, w * .036f)

        // foto (1672 x 941, fondo nero con riflesso a terra già presente), bordi sfumati nel nero
        val img = carBitmap(ctx)
        var ix = 0f; var iy = 0f; var iw = 0f; var ih = 0f
        if (img != null) {
            ih = h * .80f; iw = ih * img.width / img.height
            if (iw > w * 1.0f) { iw = wf; ih = iw * img.height / img.width }
            ix = (w - iw) / 2f; iy = h - ih * .93f
            c.save(); c.clipRect(RectF(1f, 1f, wf - 1f, hf - 1f))
            c.drawBitmap(img, null, RectF(ix, iy, ix + iw, iy + ih), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            val f = Paint()
            f.shader = LinearGradient(0f, iy, 0f, iy + ih * .25f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix, iy - 1f, ix + iw, iy + ih * .25f, f)
            f.shader = LinearGradient(ix, 0f, ix + iw * .10f, 0f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix - 1f, iy, ix + iw * .10f, hf, f)
            f.shader = LinearGradient(ix + iw, 0f, ix + iw * .90f, 0f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP); c.drawRect(ix + iw * .90f, iy, ix + iw + 1f, hf, f)
            c.restore()
        }

        // in alto a sinistra: batteria, percentuale, autonomia e stato
        val bw = min(w * .25f, h * .44f)
        val top = pad * .55f
        val bx = pad * .8f
        batteria(c, bx, top, bw, s.soc, s.limite, s.inCarica)
        val tx = bx + bw + pad * .35f
        val big = min(h * .20f, w * .10f)
        val by = top + bw * .25f + big * .36f
        testo(c, (s.soc?.toString() ?: "—") + "%", tx, by, big, col, bold, glow = col)
        testo(c, s.autonomiaKm?.let { "$it km" } ?: "", tx, by + sz * 1.55f, sz * 1.3f, TEXT, bold)
        testo(c, stato(s).uppercase(), tx, by + sz * 2.75f, sz * .85f, if (s.inCarica) TEAL else DIM, reg, spacing = .06f)

        // in alto a destra: lucchetto, scritta, temperature e ora a sinistra del lucchetto
        val lk = min(h * .19f, w * .09f)
        val lx = w - pad - lk * .5f
        testo(c, "TESLA", w - pad, top + sz * .85f, sz * .8f, Color.argb(235, 233, 238, 243), tesla(ctx), Paint.Align.RIGHT, spacing = .35f)
        val ly = top + sz * 1.5f + lk * .55f
        lucchetto(c, lx, ly, lk, s.bloccata)
        val rx = lx - lk * .8f
        testo(c, when (s.bloccata) { true -> "CHIUSA"; false -> "APERTA"; else -> "" }, rx, ly + sz * .42f, sz * 1.15f,
            when (s.bloccata) { false -> DANGER; else -> ICE }, bold, Paint.Align.RIGHT, spacing = .12f)
        testo(c, listOfNotNull(s.tempInt?.let { f1(it) + "°" }, s.tempEst?.let { "fuori " + f1(it) + "°" }).joinToString(" · "), rx, ly + sz * 1.87f, sz, TEXT, reg, Paint.Align.RIGHT)
        testo(c, ora(s.ts), rx, ly + sz * 3.2f, sz * .95f, ORA, reg, Paint.Align.RIGHT)

        // collegamenti dalla foto (punti nella foto 1672 x 941): ruote -> pressioni, pacco batteria -> batteria, portiera -> lucchetto
        if (img != null) {
            fun pt(px: Float, py: Float) = Pair(ix + px * iw / 1672f, iy + py * ih / 941f)
            val g = s.gomme
            fun warn(a: Double?, b: Double?) = (a != null && a < 2.6) || (b != null && b < 2.6)
            fun valTxt(a: Double?, b: Double?) = if (a == null && b == null) "—" else f1(a) + " · " + f1(b) + " bar"
            richiamo(c, pt(710f, 605f), Pair(w * .05f + pad * .3f, h - pad * .75f), "ANTERIORI", valTxt(g.getOrNull(0), g.getOrNull(1)), warn(g.getOrNull(0), g.getOrNull(1)), sz, reg, bold, Paint.Align.LEFT)
            richiamo(c, pt(1475f, 542f), Pair(w * .95f - pad * .3f, h - pad * .75f), "POSTERIORI", valTxt(g.getOrNull(2), g.getOrNull(3)), warn(g.getOrNull(2), g.getOrNull(3)), sz, reg, bold, Paint.Align.RIGHT)
            stanghetta(c, percorso(pt(1120f, 365f), Pair(lx, ly + lk * .6f), h * .42f), sz)
        }
        cornice(c, wf, hf, rad)
        return bmp
    }

    /** Percorso a angoli aperti: diagonale a 45° fino alla quota ym, tratto orizzontale, diagonale fino all'arrivo */
    private fun percorso(p0: Pair<Float, Float>, p3: Pair<Float, Float>, ym: Float): List<Pair<Float, Float>> {
        val sg = if (p3.first >= p0.first) 1f else -1f
        val p1 = Pair(p0.first + sg * kotlin.math.abs(p0.second - ym), ym)
        val p2 = Pair(p3.first - sg * kotlin.math.abs(p3.second - ym), ym)
        return if ((p2.first - p1.first) * sg < 0) listOf(p0, p3) else listOf(p0, p1, p2, p3)
    }

    /** Linea sottile luminosa con un pallino piccolo a ciascuna estremità */
    private fun stanghetta(c: Canvas, pts: List<Pair<Float, Float>>, sz: Float, warn: Boolean = false) {
        val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = max(1.5f, sz * .09f); strokeJoin = Paint.Join.ROUND
            color = Color.argb(215, 238, 243, 248); setShadowLayer(sz * .25f, 0f, 0f, if (warn) DANGER else GLOW)
        }
        val path = Path().apply { moveTo(pts[0].first, pts[0].second); for (p in pts.drop(1)) lineTo(p.first, p.second) }
        c.drawPath(path, ln)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (warn) Color.rgb(255, 179, 186) else Color.WHITE; setShadowLayer(sz * .3f, 0f, 0f, if (warn) DANGER else GLOW) }
        for (p in listOf(pts.first(), pts.last())) c.drawCircle(p.first, p.second, max(2.5f, sz * .16f), dot)
    }

    /** Ruota -> etichetta in basso: stanghetta verticale e poi orizzontale, titolo piccolo e valore */
    private fun richiamo(c: Canvas, dot: Pair<Float, Float>, end: Pair<Float, Float>, titolo: String, valore: String, warn: Boolean, sz: Float, reg: Typeface, bold: Typeface, align: Paint.Align) {
        val kx = end.first; val ky = end.second - sz * 2.6f
        stanghetta(c, percorso(dot, Pair(kx, ky), ky), sz, warn)
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
