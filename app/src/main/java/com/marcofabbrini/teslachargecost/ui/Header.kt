package com.marcofabbrini.teslachargecost.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intestazione: l'immagine scelta dall'utente, oppure quella fornita con l'app,
 * su sfondo scuro con una luce al neon sotto l'auto.
 */
@Composable
fun HeaderImage(imageUri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val custom: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = if (imageUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0C1630), Color(0xFF040813))))
            .drawBehind {
                // Luce a terra sotto l'auto
                drawRect(
                    Brush.radialGradient(
                        listOf(Neon.Cyan.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.92f),
                        radius = size.width * 0.55f,
                    ),
                )
            }
            .border(
                1.dp,
                Brush.linearGradient(listOf(Neon.Cyan.copy(alpha = 0.6f), Neon.Violet.copy(alpha = 0.25f))),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = custom
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Immagine di intestazione",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Sfumatura per tenere leggibile l'etichetta
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.header_tesla),
                contentDescription = "Auto elettrica",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Text(
            "CHARGE COST",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Neon.Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
        )
    }
}

/** Converte testo con virgola o punto decimale in Double, oppure null se non valido. */
fun String.toDoubleOrNullIt(): Double? = trim().replace(',', '.').toDoubleOrNull()
