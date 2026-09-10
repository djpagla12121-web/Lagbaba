package net.sath.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private class Snowflake(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speedY: Float,
    val speedX: Float,
    val alpha: Float,
    val swayAmp: Float,
    var swayOff: Float
)

@Composable
fun SnowfallBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 60
) {
    val tick = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                tick.floatValue += 0.035f
            }
        }
    }

    val flakes = remember {
        val list = mutableListOf<Snowflake>()
        val rng = Random(42)

        // Far layer (30 flakes)
        for (i in 0 until 30) {
            list.add(
                Snowflake(
                    x = rng.nextFloat() * 1000f,
                    y = rng.nextFloat() * 2000f,
                    radius = 1.5f + rng.nextFloat() * 2f,
                    speedY = 0.8f + rng.nextFloat() * 0.8f,
                    speedX = (rng.nextFloat() - 0.5f) * 0.4f,
                    alpha = 0.25f + rng.nextFloat() * 0.2f,
                    swayAmp = 0.5f + rng.nextFloat() * 1f,
                    swayOff = rng.nextFloat() * 6.28f
                )
            )
        }
        // Mid layer (20 flakes)
        for (i in 0 until 20) {
            list.add(
                Snowflake(
                    x = rng.nextFloat() * 1000f,
                    y = rng.nextFloat() * 2000f,
                    radius = 3.5f + rng.nextFloat() * 2.5f,
                    speedY = 1.6f + rng.nextFloat() * 1.2f,
                    speedX = (rng.nextFloat() - 0.5f) * 0.6f,
                    alpha = 0.45f + rng.nextFloat() * 0.25f,
                    swayAmp = 1f + rng.nextFloat() * 1.8f,
                    swayOff = rng.nextFloat() * 6.28f
                )
            )
        }
        // Near layer (10 flakes)
        for (i in 0 until 10) {
            list.add(
                Snowflake(
                    x = rng.nextFloat() * 1000f,
                    y = rng.nextFloat() * 2000f,
                    radius = 6.0f + rng.nextFloat() * 4.0f,
                    speedY = 2.4f + rng.nextFloat() * 1.6f,
                    speedX = (rng.nextFloat() - 0.5f) * 0.9f,
                    alpha = 0.7f + rng.nextFloat() * 0.25f,
                    swayAmp = 2.0f + rng.nextFloat() * 3f,
                    swayOff = rng.nextFloat() * 6.28f
                )
            )
        }
        list
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = tick.floatValue

        if (w <= 0f || h <= 0f) return@Canvas

        for (flake in flakes) {
            val sway = flake.swayAmp * sin(t + flake.swayOff)
            flake.x += flake.speedX + sway * 0.15f
            flake.y += flake.speedY

            if (flake.y > h + flake.radius * 2 || flake.x < -30f || flake.x > w + 30f) {
                flake.x = Random.nextFloat() * w
                flake.y = -flake.radius * 2
                flake.swayOff = Random.nextFloat() * 6.28f
            }

            val center = Offset(flake.x, flake.y)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flake.alpha),
                        Color(0xFF8A63FF).copy(alpha = flake.alpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = flake.radius.coerceAtLeast(1f)
                ),
                radius = flake.radius,
                center = center
            )
        }
    }
}
