package com.focuslock.app.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.design.DesignTokens.Palette
import com.focuslock.app.ui.design.DesignTokens.Shape
import com.focuslock.app.ui.design.DesignTokens.Space
import com.focuslock.app.ui.design.DesignTokens.Type

/** Deep violet field with a fine measurement grid, inspired by award-winning immersive microsites. */
@Composable
fun CosmicBackground(modifier: Modifier = Modifier, ambient: Boolean = true) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Palette.Void, Palette.Surface, Palette.Void)
                )
            )
    ) {
        if (ambient) {
            Canvas(Modifier.fillMaxSize()) {
                val glow = size.minDimension * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Palette.AccentDeep.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.12f),
                        radius = glow
                    ),
                    radius = glow,
                    center = Offset(size.width * 0.78f, size.height * 0.12f)
                )
                val step = 48.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = Palette.Hairline.copy(alpha = 0.28f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = Palette.Hairline.copy(alpha = 0.28f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }
        }
    }
}

/** Hairline-bordered surface. Status is never carried by a one-sided border. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Shape.panel),
        colors = CardDefaults.cardColors(containerColor = Palette.SurfaceRaised.copy(alpha = 0.72f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Hairline)
    ) { content() }
}

@Composable
fun EyebrowText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(fontSize = Type.Label, fontWeight = FontWeight.Medium, color = Palette.AccentSoft)
    )
}

@Composable
fun DisplayHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = Type.displayStyle,
        color = Palette.TextPrimary
    )
}

@Composable
fun NumericDisplay(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = Type.numericStyle,
        color = Palette.TextPrimary
    )
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(Shape.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Accent,
            contentColor = Palette.Void,
            disabledContainerColor = Palette.Hairline,
            disabledContentColor = Palette.TextSecondary
        )
    ) { Text(text, style = TextStyle(fontSize = Type.Body, fontWeight = FontWeight.SemiBold)) }
}

@Composable
fun StatusPill(text: String, active: Boolean = true) {
    Text(
        text = text,
        modifier = androidx.compose.ui.Modifier
            .background(
                color = if (active) Palette.AccentDeep else Palette.SurfaceRaised,
                shape = RoundedCornerShape(Shape.pill)
            )
            .padding(horizontal = Space.md, vertical = Space.xs),
        style = TextStyle(fontSize = Type.Caption, color = if (active) Palette.TextPrimary else Palette.TextSecondary)
    )
}

@Composable
fun HairlineRule() {
    Spacer(
        Modifier
            .height(1.dp)
            .background(Palette.Hairline)
    )
}

@Composable
fun StatBlock(
    label: String,
    value: String,
    leading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = Space.sm)) {
        Text(
            value,
            style = if (leading) Type.displayStyle else headlineStyle(),
            color = if (leading) Palette.TextPrimary else Palette.TextSecondary
        )
        Text(label, style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary))
        Spacer(Modifier.height(Space.xs))
    }
}

private fun headlineStyle() = TextStyle(
    fontSize = Type.Headline,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum"
)

@Composable
fun RowsLabel(text: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = TextStyle(fontSize = Type.Body, color = Palette.TextPrimary))
        Spacer(Modifier.weight(1f))
        trailing?.let {
            Text(it, style = TextStyle(fontSize = Type.Caption, color = Palette.TextSecondary))
        }
    }
}
