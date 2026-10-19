package com.marcofabbrini.teslachargecost.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.marcofabbrini.teslachargecost.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Immagine di intestazione: quella scelta dall'utente, oppure l'illustrazione generica. */
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

    val shape = RoundedCornerShape(20.dp)
    val base = modifier
        .fillMaxWidth()
        .height(170.dp)
        .clip(shape)

    val bitmap = custom
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Immagine di intestazione",
            modifier = base,
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.header_car),
            contentDescription = "Illustrazione di una berlina elettrica",
            modifier = base,
            contentScale = ContentScale.Crop,
        )
    }
}

/** Converte testo con virgola o punto decimale in Double, oppure null se non valido. */
fun String.toDoubleOrNullIt(): Double? = trim().replace(',', '.').toDoubleOrNull()
