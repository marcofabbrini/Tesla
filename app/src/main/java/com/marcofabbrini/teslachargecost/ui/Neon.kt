package com.marcofabbrini.teslachargecost.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.R

/** Palette dell'interfaccia futuristica. */
object Neon {
    val BgTop = Color(0xFF101114)
    val BgBottom = Color(0xFF07080A)
    val Panel = Color(0xFF17181C)
    val Red = Color(0xFFFF2A3D)
    val Accent = Color(0xFFD4D7DC)
    val Accent2 = Color(0xFF8D929B)
    val Warn = Color(0xFFFFB020)
    val Outline = Color(0xFF3A3D44)
    val TextDim = Color(0xFFA4A8B0)
}

/** Font LCD a 7 segmenti (DSEG, licenza SIL OFL) usato solo per il numero della percentuale. */
val LcdFontFamily = FontFamily(Font(R.font.dseg7_classic_bold, FontWeight.Bold))

/** Exo 2 (Google Fonts, SIL OFL): font di tutto il testo, tranne il titolo e il numero LCD. */
val ExoFamily = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_semibold, FontWeight.SemiBold),
    Font(R.font.exo2_bold, FontWeight.Bold),
)

/** Font del titolo (TESLA, licenza CC BY-NC-SA: uso non commerciale). */
val TitleFontFamily = FontFamily(Font(R.font.tesla_title, FontWeight.Normal))

/** Titolo a tutta larghezza, rosso con un leggero bagliore: il corpo si adatta alla larghezza disponibile. */
@Composable
fun NeonTitle(text: String, modifier: Modifier = Modifier, fitTo: String = text, scale: Float = 1f) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val probeSp = 100f
        val probe = TextStyle(
            fontFamily = TitleFontFamily,
            fontSize = probeSp.sp,
            letterSpacing = 0.06.em,
        )
        val measured = remember(fitTo, probe) { measurer.measure(fitTo, probe, maxLines = 1, softWrap = false).size.width }
        val available = with(density) { maxWidth.toPx() }
        // Il bagliore sporge un poco: si lascia un margine del 4%
        val fitSp = if (measured > 0) probeSp * (available * 0.96f) / measured else 24f
        // La dimensione parte da quella che riempirebbe la larghezza con il testo [fitTo], poi si applica [scale]
        val sizeSp = fitSp.coerceIn(14f, 72f) * scale
        Text(
            text,
            maxLines = 1,
            softWrap = false,
            color = Neon.Red,
            style = probe.copy(
                fontSize = sizeSp.sp,
                shadow = Shadow(Neon.Red.copy(alpha = 0.65f), Offset.Zero, sizeSp * 0.35f),
            ),
        )
    }
}

private class Puff(random: Random) {
    val period = 14f + random.nextFloat() * 14f
    val offset = random.nextFloat()
    val baseX = random.nextFloat()
    val sway = 0.03f + random.nextFloat() * 0.06f
    val swayFreq = 0.25f + random.nextFloat() * 0.5f
    val swayPhase = random.nextFloat() * 6.28f
    val wobble = 0.01f + random.nextFloat() * 0.03f
    val wobbleFreq = 0.9f + random.nextFloat() * 1.3f
    val drift = (random.nextFloat() - 0.5f) * 0.25f
    val sizeDp = 70f + random.nextFloat() * 90f
}

/** Circuiti stampati e chip appena percettibili: tracce a 45°, piazzole e qualche componente. */
private fun DrawScope.drawCircuits() {
    val random = Random(7)
    val cell = 26.dp.toPx()
    val cols = (size.width / cell).toInt().coerceAtLeast(1)
    val rows = (size.height / cell).toInt().coerceAtLeast(1)
    val trace = Neon.Accent.copy(alpha = 0.055f)
    val stroke = 1.2.dp.toPx()
    val dx = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
    val dy = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)

    repeat(28) {
        var cx = random.nextInt(cols + 1)
        var cy = random.nextInt(rows + 1)
        var prev = Offset(cx * cell, cy * cell)
        drawCircle(trace, 3.dp.toPx(), prev, style = Stroke(stroke))
        repeat(3 + random.nextInt(3)) {
            val len = 2 + random.nextInt(5)
            val dir = random.nextInt(8)
            cx = (cx + dx[dir] * len).coerceIn(0, cols)
            cy = (cy + dy[dir] * len).coerceIn(0, rows)
            val next = Offset(cx * cell, cy * cell)
            drawLine(trace, prev, next, stroke)
            prev = next
        }
        drawCircle(trace, 3.dp.toPx(), prev, style = Stroke(stroke))
    }

    val chip = Neon.Accent.copy(alpha = 0.065f)
    repeat(6) {
        val w = (3 + random.nextInt(3)) * cell
        val h = (2 + random.nextInt(3)) * cell
        val left = random.nextFloat() * (size.width - w).coerceAtLeast(1f)
        val top = random.nextFloat() * (size.height - h).coerceAtLeast(1f)
        drawRoundRect(chip, Offset(left, top), Size(w, h), CornerRadius(4.dp.toPx()), style = Stroke(stroke))
        drawRect(chip, Offset(left + w * 0.3f, top + h * 0.3f), Size(w * 0.4f, h * 0.4f), style = Stroke(stroke))
        val pin = 10.dp.toPx()
        val len = 6.dp.toPx()
        var x = left + pin
        while (x < left + w - pin / 2) {
            drawLine(chip, Offset(x, top), Offset(x, top - len), stroke)
            drawLine(chip, Offset(x, top + h), Offset(x, top + h + len), stroke)
            x += pin
        }
        var y = top + pin
        while (y < top + h - pin / 2) {
            drawLine(chip, Offset(left, y), Offset(left - len, y), stroke)
            drawLine(chip, Offset(left + w, y), Offset(left + w + len, y), stroke)
            y += pin
        }
    }
}

/** Fumo: macchie morbide che salgono dal basso con un movimento irregolare, sempre sullo sfondo. */
@Composable
private fun SmokeLayer() {
    val puffs = remember { Random(11).let { r -> List(16) { Puff(r) } } }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { time = (it - start) / 1_000_000_000f }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val t = time
                puffs.forEach { p ->
                    val progress = (t / p.period + p.offset) % 1f
                    val y = size.height * (1.08f - progress * 0.8f)
                    val x = size.width * (
                        p.baseX + p.drift * progress +
                            p.sway * sin(t * p.swayFreq + p.swayPhase) +
                            p.wobble * sin(t * p.wobbleFreq * 2.3f + p.swayPhase * 1.7f)
                        )
                    val radius = (p.sizeDp * (0.6f + progress * 1.2f)).dp.toPx()
                    val alpha = 0.10f * sin(PI.toFloat() * progress)
                    val center = Offset(x, y)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Neon.Accent.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            },
    )
}

/**
 * Cursore rosso a pixel, come lo scanner di Supercar: una testa luminosa con una coda a gradini
 * scorre lungo il lato sinistro dall'alto verso il basso, a scatti di un blocco, con un alone morbido.
 */
@Composable
private fun FrameLayer() {
    val t by rememberInfiniteTransition(label = "scanner").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "scan",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val segH = 7.dp.toPx()
                val gap = 2.dp.toPx()
                val step = segH + gap
                val segW = 4.dp.toPx()
                val x = 3.dp.toPx()
                val count = (size.height / step).toInt()
                val tail = 12
                // Posizione a scatti: la testa salta da un blocco al successivo
                val head = (t * (count + tail)).toInt()
                for (d in 0 until tail) {
                    val i = head - d
                    if (i !in 0 until count) continue
                    val level = 1f - d / tail.toFloat()
                    val a = level * level
                    val y = i * step
                    // Alone che si apre verso destra
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Neon.Red.copy(alpha = 0.40f * a), Color.Transparent),
                            startX = x,
                            endX = x + 26.dp.toPx(),
                        ),
                        topLeft = Offset(x, y - 4.dp.toPx()),
                        size = Size(26.dp.toPx(), segH + 8.dp.toPx()),
                    )
                    drawRect(Neon.Red.copy(alpha = 0.22f * a), Offset(x - 2.dp.toPx(), y - 2.dp.toPx()), Size(segW + 4.dp.toPx(), segH + 4.dp.toPx()))
                    drawRect(Neon.Red.copy(alpha = a), Offset(x, y), Size(segW, segH))
                    if (d == 0) {
                        drawRect(Color.White.copy(alpha = 0.75f), Offset(x + 1.dp.toPx(), y + 1.dp.toPx()), Size(segW - 2.dp.toPx(), segH - 2.dp.toPx()))
                    }
                }
            },
    )
}

/** Sfondo scuro a strati: griglia e aloni, circuiti, fumo e cursore rosso. */
@Composable
fun NeonBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Neon.BgTop, Neon.BgBottom))),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Accent.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(size.width * 0.1f, 0f),
                            radius = size.width,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Neon.Accent2.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(size.width * 0.95f, size.height * 0.95f),
                            radius = size.width * 1.1f,
                        ),
                    )
                    val step = 32.dp.toPx()
                    val line = Neon.Accent.copy(alpha = 0.03f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                },
        )
        Box(modifier = Modifier.fillMaxSize().drawBehind { drawCircuits() })
        SmokeLayer()
        FrameLayer()
        content()
    }
}

/** Pannello in vetro scuro con bordo sfumato al neon. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Color = Neon.Accent,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1C1D22).copy(alpha = 0.92f), Color(0xFF131418).copy(alpha = 0.92f)),
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(glow.copy(alpha = 0.7f), Neon.Accent2.copy(alpha = 0.3f))),
                shape,
            )
            .padding(18.dp),
        content = content,
    )
}

/** Etichetta di sezione in maiuscolo, con spaziatura larga. */
@Composable
fun NeonLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = Neon.TextDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
    )
}

/**
 * Superficie con leggero effetto 3D: faccia sfumata con bordo di luce, spessore sotto e
 * abbassamento al tocco (se [onClick] è presente). [filled] = piena e colorata, altrimenti scura con contorno luminoso.
 * Il contenuto riceve il colore consigliato per testo e icone.
 */
@Composable
fun Neon3D(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    filled: Boolean = false,
    accent: Color = Neon.Accent,
    depth: Dp = 4.dp,
    content: @Composable BoxScope.(contentColor: Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(14.dp)

    val slab = when {
        !enabled -> Color(0xFF0B0C0E)
        filled -> lerp(accent, Color.Black, 0.55f)
        else -> lerp(accent, Color.Black, 0.72f)
    }
    val faceBrush = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0xFF2A2C32), Color(0xFF191A1E)))
        filled -> Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.30f), accent, lerp(accent, Color.Black, 0.22f)))
        else -> Brush.verticalGradient(listOf(Color(0xFF3A3D45), Color(0xFF24262C)))
    }
    val borderBrush = when {
        !enabled -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent))
        filled -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), accent.copy(alpha = 0.2f)))
        else -> Brush.verticalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.25f)))
    }
    val contentColor = when {
        !enabled -> Neon.TextDim
        filled -> Color(0xFF14151A)
        else -> accent
    }

    Box(
        modifier = modifier
            .drawBehind {
                // Bagliore leggero attorno agli elementi attivi
                if (enabled) {
                    drawRoundRect(
                        color = accent.copy(alpha = if (filled) 0.16f else 0.08f),
                        topLeft = Offset(-3.dp.toPx(), -1.dp.toPx()),
                        size = Size(size.width + 6.dp.toPx(), size.height + 4.dp.toPx()),
                        cornerRadius = CornerRadius(17.dp.toPx()),
                    )
                }
            },
        propagateMinConstraints = true,
    ) {
        // Spessore
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .background(slab, shape),
        )
        // Faccia
        Box(
            modifier = Modifier
                .padding(bottom = depth)
                .offset(y = if (pressed && enabled && onClick != null) depth - 1.dp else 0.dp)
                .clip(shape)
                .background(faceBrush, shape)
                .border(1.dp, borderBrush, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content(contentColor)
        }
    }
}

/** Pulsante 3D con testo. */
@Composable
fun NeonButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    Neon3D(modifier = modifier, onClick = onClick, enabled = enabled, filled = filled) { color ->
        Text(
            label.uppercase(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
        )
    }
}

/** Pulsante 3D con una sola icona; [contentDescription] serve per l'accessibilità. */
@Composable
fun NeonIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(width = 46.dp, height = 50.dp),
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    Neon3D(modifier = modifier, onClick = onClick, enabled = enabled, filled = filled) { color ->
        Icon(icon, contentDescription, modifier = Modifier.size(24.dp), tint = color)
    }
}

/**
 * Selettore a due o più opzioni con cursore centrale: un blocco 3D rosso scorre
 * da un'opzione all'altra, sopra una guida incassata.
 */
@Composable
fun NeonToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val gap = 5.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0C0E), shape)
            .border(1.dp, Neon.Outline, shape)
            .padding(5.dp),
    ) {
        val count = options.size
        val itemWidth = (maxWidth - gap * (count - 1)) / count
        val thumbX by animateDpAsState(
            targetValue = (itemWidth + gap) * selectedIndex,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
            label = "thumb",
        )
        // Cursore
        Neon3D(
            modifier = Modifier
                .offset(x = thumbX)
                .width(itemWidth)
                .height(50.dp),
            filled = true,
        ) {}
        // Etichette, sopra il cursore
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    if (selected) Color(0xFF14151A) else Neon.TextDim,
                    label = "toggleText",
                )
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label.uppercase(),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Colori dei campi di testo in stile neon. */
@Composable
fun neonFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Neon.Accent,
    unfocusedBorderColor = Neon.Outline,
    errorBorderColor = Neon.Warn,
    focusedLabelColor = Neon.Accent,
    unfocusedLabelColor = Neon.TextDim,
    cursorColor = Neon.Accent,
    focusedSuffixColor = Neon.TextDim,
    unfocusedSuffixColor = Neon.TextDim,
)

/**
 * Casella 3D con alone luminoso pulsante, in stile display LCD a 7 segmenti con segmenti spenti sullo sfondo.
 * Il corpo del numero si adatta alla larghezza, così due caselle stanno affiancate.
 */
@Composable
fun LcdField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    ghost: String,
    suffix: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    maxLength: Int = 3,
    sizeChars: Int = ghost.length,
) {
    val pulse by rememberInfiniteTransition(label = "lcdGlow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val accent = if (isError) Neon.Warn else Neon.Accent
    val shape = RoundedCornerShape(22.dp)
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 14.dp)
            .graphicsLayer {
                rotationX = 9f
                cameraDistance = 16f * density
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .drawBehind {
                val radius = 22.dp.toPx()
                // Spessore della lastra, per l'effetto 3D
                drawRoundRect(
                    color = Color(0xFF1A1B1F),
                    topLeft = Offset(0f, 8.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(radius),
                )
                // Alone luminoso: strati sempre più larghi e trasparenti
                val layers = 8
                for (i in layers downTo 1) {
                    val spread = i * 2.dp.toPx()
                    drawRoundRect(
                        color = accent.copy(alpha = 0.05f * pulse * (layers - i + 1) / layers),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + 2 * spread, size.height + 2 * spread),
                        cornerRadius = CornerRadius(radius + spread),
                    )
                }
            }
            .background(Brush.verticalGradient(listOf(Color(0xFF24262C), Color(0xFF0F1013))), shape)
            .drawBehind {
                val radius = CornerRadius(22.dp.toPx())
                // Riflesso del vetro sulla metà superiore
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                        endY = size.height * 0.5f,
                    ),
                    size = Size(size.width, size.height * 0.5f),
                    cornerRadius = radius,
                )
                // Ombra interna in basso
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                        startY = size.height * 0.6f,
                        endY = size.height,
                    ),
                    cornerRadius = radius,
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { focusRequester.requestFocus() },
    ) {
        BoxWithConstraints(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Cifre larghe circa 0,85 volte il corpo: il corpo si calcola dallo spazio disponibile
            val sizeSp = ((maxWidth.value - 28f) / (sizeChars * 0.85f)).coerceIn(20f, 46f)
            val lcdStyle = TextStyle(
                fontFamily = LcdFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = sizeSp.sp,
                textAlign = TextAlign.End,
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    // Segmenti spenti
                    Text(
                        ghost,
                        modifier = Modifier.fillMaxWidth(),
                        style = lcdStyle,
                        color = accent.copy(alpha = 0.07f),
                        maxLines = 1,
                        softWrap = false,
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = { raw ->
                            val cleaned = if (decimal) {
                                val text = raw.replace(',', '.').filter { c -> c.isDigit() || c == '.' }
                                val firstDot = text.indexOf('.')
                                if (firstDot < 0) text else text.substring(0, firstDot + 1) + text.substring(firstDot + 1).replace(".", "")
                            } else {
                                raw.filter { c -> c.isDigit() }
                            }
                            onValueChange(cleaned.take(maxLength))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = lcdStyle.copy(
                            color = accent,
                            shadow = Shadow(accent.copy(alpha = 0.55f + 0.45f * pulse), Offset.Zero, 24f),
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                        ),
                        cursorBrush = SolidColor(accent),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                if (value.isEmpty()) {
                                    Text("0", style = lcdStyle, color = accent.copy(alpha = 0.25f))
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                Text(
                    suffix,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    color = accent.copy(alpha = 0.9f),
                    fontSize = (sizeSp * 0.42f).coerceAtLeast(12f).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Contatore a rulli come quello della pompa di benzina: ogni cifra è un rullo che gira
 * fino al valore. Zeri iniziali spenti, segmenti spenti dietro le cifre.
 */
@Composable
fun OdometerPrice(value: Double, modifier: Modifier = Modifier) {
    val target = (value * 100).roundToInt().coerceIn(0, 999_999).toFloat()
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.animateTo(target, tween(1500, easing = FastOutSlowInEasing))
    }
    val current = animated.value
    val shape = RoundedCornerShape(12.dp)
    val digitHeight = 38.dp
    val lcd = TextStyle(fontFamily = LcdFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp)

    // Posizione di ogni rullo (0..10 righe). Il primo gira in continuo; ogni rullo successivo
    // resta fermo sulla sua cifra e avanza solo mentre il rullo precedente passa da 9 a 0.
    // A riposo (valore intero) tutti i rulli sono quindi esattamente allineati sulla cifra.
    val positions = FloatArray(6)
    positions[0] = current % 10f
    for (place in 1..5) {
        val base = floor(current / 10f.pow(place)) % 10f
        positions[place] = base + (positions[place - 1] - 9f).coerceIn(0f, 1f)
    }

    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0C0E), Color(0xFF17181C))), shape)
            .border(1.dp, Brush.verticalGradient(listOf(Neon.Accent.copy(alpha = 0.6f), Neon.Accent.copy(alpha = 0.15f))), shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 6 rulli: migliaia, centinaia, decine, unità, decimi e centesimi di euro
        for (place in 5 downTo 0) {
            val position = positions[place]
            // Gli zeri iniziali restano spenti
            val lit = place <= 2 || current >= 10f.pow(place)
            Box(modifier = Modifier.height(digitHeight).clipToBounds()) {
                Text("8", style = lcd, color = Neon.Accent.copy(alpha = 0.07f), modifier = Modifier.align(Alignment.Center))
                Column(
                    modifier = Modifier
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .offset { IntOffset(0, -(position * digitHeight.toPx()).roundToInt()) },
                ) {
                    repeat(11) { i ->
                        Box(modifier = Modifier.height(digitHeight), contentAlignment = Alignment.Center) {
                            Text(
                                "${i % 10}",
                                style = lcd.copy(
                                    shadow = Shadow(Neon.Accent.copy(alpha = 0.6f), Offset.Zero, 18f),
                                ),
                                color = if (lit) Neon.Accent else Neon.Accent.copy(alpha = 0.12f),
                            )
                        }
                    }
                }
            }
            if (place == 2) {
                Text(",", style = lcd, color = Neon.Accent, modifier = Modifier.padding(horizontal = 1.dp))
            }
        }
        Text(
            "€",
            modifier = Modifier.padding(start = 6.dp),
            color = Neon.Accent.copy(alpha = 0.9f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
