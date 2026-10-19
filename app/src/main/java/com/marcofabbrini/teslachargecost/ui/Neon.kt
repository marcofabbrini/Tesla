package com.marcofabbrini.teslachargecost.ui

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
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
fun NeonTitle(text: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val probeSp = 100f
        val probe = TextStyle(
            fontFamily = TitleFontFamily,
            fontSize = probeSp.sp,
            letterSpacing = 0.06.em,
        )
        val measured = remember(text, probe) { measurer.measure(text, probe, maxLines = 1, softWrap = false).size.width }
        val available = with(density) { maxWidth.toPx() }
        // Il bagliore sporge un poco: si lascia un margine del 4%
        val fitSp = if (measured > 0) probeSp * (available * 0.96f) / measured else 24f
        val sizeSp = fitSp.coerceIn(14f, 72f)
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

/** Pennello a cometa rossa: una coda che sfuma e una testa netta, ruotata di [angle] gradi attorno al centro. */
private fun cometBrush(center: Offset, angle: Float, alpha: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val clear = Neon.Red.copy(alpha = 0f).toArgb()
        val head = Neon.Red.copy(alpha = alpha).toArgb()
        val shader = SweepGradient(
            center.x,
            center.y,
            intArrayOf(clear, clear, head, clear, clear),
            floatArrayOf(0f, 0.55f, 0.80f, 0.802f, 1f),
        )
        shader.setLocalMatrix(Matrix().apply { setRotate(angle, center.x, center.y) })
        return shader
    }
}

/** Sfondo scuro con griglia sottile e aloni di colore. */
@Composable
fun NeonBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val angle by rememberInfiniteTransition(label = "frame").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "frameAngle",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Neon.BgTop, Neon.BgBottom)))
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
                val line = Neon.Accent.copy(alpha = 0.04f)
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

                // Cornice dell'app: filo grigio chiaro sfumato, con una cometa rossa che la percorre
                val inset = 3.dp.toPx()
                val frameTopLeft = Offset(inset, inset)
                val frameSize = Size(size.width - 2 * inset, size.height - 2 * inset)
                val frameRadius = CornerRadius(24.dp.toPx())
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Neon.Accent.copy(alpha = 0.60f),
                            Neon.Accent.copy(alpha = 0.10f),
                            Neon.Accent.copy(alpha = 0.42f),
                            Neon.Accent.copy(alpha = 0.12f),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    topLeft = frameTopLeft,
                    size = frameSize,
                    cornerRadius = frameRadius,
                    style = Stroke(1.5.dp.toPx()),
                )
                val center = Offset(size.width / 2f, size.height / 2f)
                drawRoundRect(
                    brush = cometBrush(center, angle, 0.30f),
                    topLeft = frameTopLeft,
                    size = frameSize,
                    cornerRadius = frameRadius,
                    style = Stroke(7.dp.toPx()),
                )
                drawRoundRect(
                    brush = cometBrush(center, angle, 1f),
                    topLeft = frameTopLeft,
                    size = frameSize,
                    cornerRadius = frameRadius,
                    style = Stroke(2.5.dp.toPx()),
                )
            },
        content = content,
    )
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
 * Casella 3D con alone luminoso pulsante per i punti percentuale.
 * Display LCD a 7 segmenti con segmenti spenti sullo sfondo, come un vero quadrante.
 */
@Composable
fun LcdPercentField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "lcdGlow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val accent = if (isError) Neon.Warn else Neon.Accent
    val shape = RoundedCornerShape(26.dp)
    val lcdStyle = TextStyle(
        fontFamily = LcdFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 104.sp,
        textAlign = TextAlign.End,
    )
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .graphicsLayer {
                rotationX = 9f
                cameraDistance = 16f * density
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .drawBehind {
                val radius = 26.dp.toPx()
                // Spessore della lastra, per l'effetto 3D
                drawRoundRect(
                    color = Color(0xFF1A1B1F),
                    topLeft = Offset(0f, 9.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(radius),
                )
                // Alone luminoso: strati sempre più larghi e trasparenti
                val layers = 8
                for (i in layers downTo 1) {
                    val spread = i * 2.5.dp.toPx()
                    drawRoundRect(
                        color = accent.copy(alpha = 0.05f * pulse * (layers - i + 1) / layers),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + 2 * spread, size.height + 2 * spread),
                        cornerRadius = CornerRadius(radius + spread),
                    )
                }
            }
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2C33), Color(0xFF0F1013))), shape)
            .border(
                2.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.55f), accent.copy(alpha = 0.9f), accent.copy(alpha = 0.15f)),
                ),
                shape,
            )
            .drawBehind {
                val radius = CornerRadius(26.dp.toPx())
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
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                // Segmenti spenti
                Text(
                    "888",
                    modifier = Modifier.fillMaxWidth(),
                    style = lcdStyle,
                    color = accent.copy(alpha = 0.07f),
                )
                BasicTextField(
                    value = value,
                    onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(3)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = lcdStyle.copy(
                        color = accent,
                        shadow = Shadow(accent.copy(alpha = 0.55f + 0.45f * pulse), Offset.Zero, 30f),
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                "%",
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp),
                color = accent.copy(alpha = 0.9f),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
