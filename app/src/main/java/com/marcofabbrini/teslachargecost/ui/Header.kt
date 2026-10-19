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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.marcofabbrini.teslachargecost.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carica l'immagine scelta dall'utente (null se non c'è o non si legge). */
@Composable
private fun rememberCustomImage(imageUri: String?): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, imageUri) {
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
}

/**
 * Immagine di sfondo della schermata principale: l'auto fornita con l'app (senza sfondo),
 * oppure l'immagine scelta dall'utente, sfumata verso il basso.
 */
@Composable
fun HomeBackdrop(imageUri: String?, modifier: Modifier = Modifier) {
    val custom by rememberCustomImage(imageUri)
    val bitmap = custom
    Box(modifier = modifier.fillMaxWidth().height(210.dp)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.6f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(listOf(Color.Black, Color.Black, Color.Transparent)),
                            blendMode = BlendMode.DstIn,
                        )
                    },
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.header_tesla),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopCenter,
            )
        }
    }
}

/** Anteprima dell'immagine di sfondo nelle impostazioni. */
@Composable
fun HeaderImage(imageUri: String?, modifier: Modifier = Modifier) {
    val custom by rememberCustomImage(imageUri)
    val shape = RoundedCornerShape(22.dp)
    val bitmap = custom
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1C21), Color(0xFF0A0B0D))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Neon.Accent.copy(alpha = 0.65f), Neon.Accent2.copy(alpha = 0.25f))),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Immagine di sfondo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.header_tesla),
                contentDescription = "Auto elettrica",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Converte testo con virgola o punto decimale in Double, oppure null se non valido. */
fun String.toDoubleOrNullIt(): Double? = trim().replace(',', '.').toDoubleOrNull()
