package com.marcofabbrini.teslachargecost.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icone a linea (24x24), disegnate qui: nessuna dipendenza dalle librerie di icone. */
object NeonIcons {
    private fun line(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Back = line("back", "M19,12 L5,12", "M11,6 L5,12 L11,18")

    val History = line(
        "history",
        "M12,3 A9,9 0 1,1 12,21 A9,9 0 1,1 12,3",
        "M12,7 L12,12 L15.5,14.5",
    )

    val Settings = line(
        "settings",
        "M4,6 L10,6", "M14,6 L20,6", "M10,6 A2,2 0 1,0 14,6 A2,2 0 1,0 10,6",
        "M4,12 L5,12", "M9,12 L20,12", "M5,12 A2,2 0 1,0 9,12 A2,2 0 1,0 5,12",
        "M4,18 L14,18", "M18,18 L20,18", "M14,18 A2,2 0 1,0 18,18 A2,2 0 1,0 14,18",
    )

    val Delete = line(
        "delete",
        "M4,7 L20,7", "M9,7 L9,4 L15,4 L15,7",
        "M6,7 L7,20 L17,20 L18,7",
        "M10,11 L10,16", "M14,11 L14,16",
    )

    val Save = line(
        "save",
        "M3,3 L17,3 L21,7 L21,21 L3,21 Z",
        "M7,3 L7,8 L15,8 L15,3",
        "M7,21 L7,14 L17,14 L17,21",
    )

    val Check = line("check", "M4,12.5 L9.5,18 L20,6.5")

    val Photo = line(
        "photo",
        "M3,5 L21,5 L21,19 L3,19 Z",
        "M3,16 L9,10 L14,15 L17,12 L21,16",
        "M7.5,9 A1.5,1.5 0 1,0 10.5,9 A1.5,1.5 0 1,0 7.5,9",
    )

    val Restore = line(
        "restore",
        "M4,12 A8,8 0 1,0 6.4,6.4",
        "M4,4 L4,9 L9,9",
    )
}
