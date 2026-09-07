package com.nothingness.bruhpatcher.ui.components.liquid

// Adapted from Kyant0/AndroidLiquidGlass (Apache 2.0),
// compose-miuix-ui (Apache 2.0),
// and SukiSU-Ultra (manager/app/src/main/java/com/sukisu/ultra/ui/component/liquid/)

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Interactive touch specular bloom highlight.
 * Follows the user's touch/drag with real-time radial falloff.
 * 
 * Uses hardware AGSL RuntimeShader on Android 13+ (API 33+) with BlendMode.Plus,
 * and falls back gracefully to a high-speed radial gradient on Android 8.0 - 12L (API 26-32).
 */
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val offset: Offset get() = positionAnimation.value - startPosition

    // Lazy initialization of AGSL shader on API 33+
    private val agslShader by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(
                """
                uniform float2 size;
                layout(color) uniform half4 color;
                uniform float radius;
                uniform float2 position;
                
                half4 main(float2 coord) {
                    float dist = distance(coord, position);
                    float intensity = smoothstep(radius, radius * 0.45, dist);
                    return color * intensity;
                }
                """.trimIndent()
            )
        } else {
            null
        }
    }

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            // Ambient base lift
            drawRect(
                color = Color.White.copy(alpha = 0.05f * progress),
                blendMode = BlendMode.Plus
            )

            val currentPos = position(size, positionAnimation.value)
            val clampedX = currentPos.x.fastCoerceIn(0f, size.width)
            val clampedY = currentPos.y.fastCoerceIn(0f, size.height)
            val highlightRadius = size.minDimension * 1.25f

            val shader = agslShader
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color", Color.White.copy(alpha = 0.16f * progress).toArgb())
                shader.setFloatUniform("radius", highlightRadius)
                shader.setFloatUniform("position", clampedX, clampedY)
                drawRect(
                    brush = ShaderBrush(shader),
                    blendMode = BlendMode.Plus
                )
            } else {
                // Fallback radial gradient for API < 33
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f * progress),
                            Color.White.copy(alpha = 0.06f * progress),
                            Color.Transparent
                        ),
                        center = Offset(clampedX, clampedY),
                        radius = highlightRadius
                    ),
                    center = Offset(clampedX, clampedY),
                    radius = highlightRadius,
                    blendMode = BlendMode.Plus
                )
            }
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            }
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }
}

/**
 * Tactile inner shadow specification for realistic glass edge depth
 */
@Immutable
data class InnerShadow(
    val radius: Dp = 12.dp,
    val color: Color = Color.Black.copy(alpha = 0.18f),
    val alpha: Float = 1.0f
)

/**
 * AGSL Shaders for SDF-based rounded rect refraction and chromatic dispersion
 * Origin: Kyant0/AndroidLiquidGlass
 */
object LiquidGlassShaders {
    const val ROUNDED_RECT_SDF = """
    float radiusAt(float2 coord, float4 radii) {
        if (coord.x >= 0.0) {
            if (coord.y <= 0.0) return radii.y;
            else return radii.z;
        } else {
            if (coord.y <= 0.0) return radii.x;
            else return radii.w;
        }
    }
    
    float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
        float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
        float outside = length(max(cornerCoord, 0.0)) - radius;
        float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
        return outside + inside;
    }
    
    float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
        float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
        if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
            return sign(coord) * normalize(max(cornerCoord, 0.0));
        } else {
            float gradX = step(cornerCoord.y, cornerCoord.x);
            return sign(coord) * float2(gradX, 1.0 - gradX);
        }
    }
    """

    const val ROUNDED_RECT_REFRACTION_WITH_DISPERSION = """
    uniform shader content;
    uniform float2 size;
    uniform float2 offset;
    uniform float4 cornerRadii;
    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform float depthEffect;
    uniform float chromaticAberration;
    
    $ROUNDED_RECT_SDF
    
    float circleMap(float x) {
        return 1.0 - sqrt(max(0.0, 1.0 - x * x));
    }
    
    half4 main(float2 coord) {
        float2 halfSize = size * 0.5;
        float2 centeredCoord = (coord + offset) - halfSize;
        float radius = radiusAt(coord, cornerRadii);
        
        float sd = sdRoundedRect(centeredCoord, halfSize, radius);
        if (-sd >= refractionHeight) {
            return content.eval(coord);
        }
        sd = min(sd, 0.0);
        
        float d = circleMap(1.0 - -sd / max(0.001, refractionHeight)) * refractionAmount;
        float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
        float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));
        
        float2 refractedCoord = coord + d * grad;
        float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / max(1.0, halfSize.x * halfSize.y));
        float2 dispersedCoord = d * grad * dispersionIntensity;
        
        half4 color = half4(0.0);
        half4 red = content.eval(refractedCoord + dispersedCoord);
        color.r += red.r / 3.5;
        color.a += red.a / 7.0;
        
        half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
        color.r += orange.r / 3.5;
        color.g += orange.g / 7.0;
        color.a += orange.a / 7.0;
        
        half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
        color.r += yellow.r / 3.5;
        color.g += yellow.g / 3.5;
        color.a += yellow.a / 7.0;
        
        half4 green = content.eval(refractedCoord);
        color.g += green.g / 3.5;
        color.a += green.a / 7.0;
        
        half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
        color.g += cyan.g / 3.5;
        color.b += cyan.b / 3.0;
        color.a += cyan.a / 7.0;
        
        half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
        color.b += blue.b / 3.0;
        color.a += blue.a / 7.0;
        
        half4 purple = content.eval(refractedCoord - dispersedCoord);
        color.r += purple.r / 7.0;
        color.b += purple.b / 3.0;
        color.a += purple.a / 7.0;
        
        return color;
    }
    """
}
