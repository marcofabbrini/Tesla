package com.marcofabbrini.teslachargecost.ui

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
    val BgTop = Color(0xFF060B16)
    val BgBottom = Color(0xFF02040A)
    val Panel = Color(0xFF0E1830)
    val Cyan = Color(0xFF00E5FF)
    val Violet = Color(0xFF7C4DFF)
    val Red = Color(0xFFFF5370)
    val Outline = Color(0xFF1C2E4A)
    val TextDim = Color(0xFF8FA3BF)
}

/** Font LCD a 7 segmenti (DSEG, licenza SIL OFL) usato solo per il numero della percentuale. */
val LcdFontFamily = FontFamily(Font(R.font.dseg7_classic_bold, FontWeight.Bold))

/** Sfondo scuro con griglia sottile e aloni di colore. */
@Composable
fun NeonBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Neon.BgTop, Neon.BgBottom)))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Neon.Cyan.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.1f, 0f),
                        radius = size.width,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Neon.Violet.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.95f),
                        radius = size.width * 1.1f,
                    ),
                )
                val step = 32.dp.toPx()
                val line = Neon.Cyan.copy(alpha = 0.04f)
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
        content = content,
    )
}

/** Pannello in vetro scuro con bordo sfumato al neon. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Color = Neon.Cyan,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111B30).copy(alpha = 0.92f), Color(0xFF0A1222).copy(alpha = 0.92f)),
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(glow.copy(alpha = 0.7f), Neon.Violet.copy(alpha = 0.3f))),
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

/** Pulsante testuale con contorno al neon. */
@Composable
fun NeonTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        label.uppercase(),
        modifier = modifier
            .clip(shape)
            .border(1.dp, Neon.Cyan.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Neon.Cyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
    )
}

/** Selettore a due o più opzioni, stile "pillola" luminosa. */
@Composable
fun NeonToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1222), shape)
            .border(1.dp, Neon.Outline, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val itemShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .then(
                        if (selected) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Neon.Cyan.copy(alpha = 0.28f), Neon.Violet.copy(alpha = 0.28f)),
                                    ),
                                    itemShape,
                                )
                                .border(1.dp, Neon.Cyan.copy(alpha = 0.8f), itemShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label.uppercase(),
                    color = if (selected) Neon.Cyan else Neon.TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

/** Colori dei campi di testo in stile neon. */
@Composable
fun neonFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Neon.Cyan,
    unfocusedBorderColor = Neon.Outline,
    errorBorderColor = Neon.Red,
    focusedLabelColor = Neon.Cyan,
    unfocusedLabelColor = Neon.TextDim,
    cursorColor = Neon.Cyan,
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
    val accent = if (isError) Neon.Red else Neon.Cyan
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
                    color = Color(0xFF031A22),
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
            .background(Brush.verticalGradient(listOf(Color(0xFF10203D), Color(0xFF040A16))), shape)
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
