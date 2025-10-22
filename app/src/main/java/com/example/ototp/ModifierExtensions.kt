package com.example.ototp

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dimmingOutsideRoundedBox(
    boxSize: Dp = 300.dp,
    cornerRadius: Dp = 40.dp,
    dimColor: Color = Color(0x88000000)
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()

        val boxWidth = boxSize.toPx()
        val boxHeight = boxSize.toPx()
        val radius = cornerRadius.toPx()

        val canvasWidth = size.width
        val canvasHeight = size.height

        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2f

        drawRect(color = dimColor)

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Clear
        )
    }
)