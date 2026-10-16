package com.xiaomanjun.sleepdownschedule.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import org.json.JSONObject

enum class GlassStyle(val title: String) {
    LIQUID("液态玻璃"), GAUSSIAN("高斯模糊玻璃"), SOFT("柔光玻璃")
}

@Immutable
data class GlassParameters(
    val blur: Float = 8f, val opacity: Float = .28f, val tint: Float = .10f,
    val corner: Float = 28f, val highlight: Float = .35f, val stroke: Float = .7f,
    val shadow: Float = 12f, val lensHeight: Float = 12f, val lensAmount: Float = 20f,
    val dispersion: Boolean = true, val press: Float = 1f, val response: Float = 1f,
    val noise: Float = .008f, val brightness: Float = .03f,
    val contrast: Float = .9f, val saturation: Float = .9f, val blend: Float = .18f
) {
    fun normalized() = copy(blur = blur.coerceIn(0f, 64f), opacity = opacity.coerceIn(0f, 1f),
        tint = tint.coerceIn(0f, 1f), corner = corner.coerceIn(12f, 40f), highlight = highlight.coerceIn(0f, 1f),
        stroke = stroke.coerceIn(0f, 2f), shadow = shadow.coerceIn(0f, 24f), lensHeight = lensHeight.coerceIn(0f, 32f),
        lensAmount = lensAmount.coerceIn(0f, 48f), press = press.coerceIn(0f, 1f), response = response.coerceIn(.5f, 1.5f),
        noise = noise.coerceIn(0f, .04f), brightness = brightness.coerceIn(-.15f, .15f),
        contrast = contrast.coerceIn(.6f, 1.4f), saturation = saturation.coerceIn(0f, 1.5f), blend = blend.coerceIn(0f, .6f))

    companion object {
        fun defaults(style: GlassStyle) = when (style) {
            GlassStyle.LIQUID -> GlassParameters()
            GlassStyle.GAUSSIAN -> GlassParameters(blur = 24f, opacity = .32f, highlight = .16f,
                stroke = .5f, shadow = 10f, lensHeight = 0f, lensAmount = 0f, dispersion = false)
            GlassStyle.SOFT -> GlassParameters(blur = 32f, opacity = .42f, highlight = .12f,
                stroke = .4f, lensHeight = 0f, lensAmount = 0f, dispersion = false)
        }
    }
}

/** Device presentation only. Missing fields retain the previous renderer until explicitly applied. */
@Immutable
data class GlassAppearance(
    val style: GlassStyle = GlassStyle.LIQUID,
    val customized: Boolean = false,
    val presets: Map<GlassStyle, GlassParameters> = GlassStyle.entries.associateWith(GlassParameters::defaults)
) {
    val current get() = presets.getValue(style)
    fun update(value: GlassParameters) = copy(customized = true, presets = presets + (style to value.normalized()))

    fun encode(): String = JSONObject().apply {
        put("style", style.name); put("customized", customized)
        GlassStyle.entries.forEach { key ->
            val p = presets.getValue(key)
            put(key.name, JSONObject().apply {
                put("blur",p.blur);put("opacity",p.opacity);put("tint",p.tint);put("corner",p.corner)
                put("highlight",p.highlight);put("stroke",p.stroke);put("shadow",p.shadow)
                put("lensHeight",p.lensHeight);put("lensAmount",p.lensAmount);put("dispersion",p.dispersion)
                put("press",p.press);put("response",p.response);put("noise",p.noise)
                put("brightness",p.brightness);put("contrast",p.contrast);put("saturation",p.saturation);put("blend",p.blend)
            })
        }
    }.toString()

    companion object {
        fun decode(value: String?): GlassAppearance {
            if (value.isNullOrBlank()) return GlassAppearance()
            val json = try { JSONObject(value) } catch (_: org.json.JSONException) { return GlassAppearance() }
            return GlassAppearance(
                style = GlassStyle.entries.firstOrNull { it.name == json.optString("style") } ?: GlassStyle.LIQUID,
                customized = json.optBoolean("customized"),
                presets = GlassStyle.entries.associateWith { style ->
                    val d = GlassParameters.defaults(style)
                    val o = json.optJSONObject(style.name) ?: return@associateWith d
                    fun n(key: String, fallback: Float) = o.optDouble(key, fallback.toDouble()).toFloat().takeIf { it.isFinite() } ?: fallback
                    d.copy(blur=n("blur",d.blur),opacity=n("opacity",d.opacity),tint=n("tint",d.tint),corner=n("corner",d.corner),
                        highlight=n("highlight",d.highlight),stroke=n("stroke",d.stroke),shadow=n("shadow",d.shadow),
                        lensHeight=n("lensHeight",d.lensHeight),lensAmount=n("lensAmount",d.lensAmount),dispersion=o.optBoolean("dispersion",d.dispersion),
                        press=n("press",d.press),response=n("response",d.response),noise=n("noise",d.noise),brightness=n("brightness",d.brightness),
                        contrast=n("contrast",d.contrast),saturation=n("saturation",d.saturation),blend=n("blend",d.blend)).normalized()
                })
        }
    }
}

val LocalGlassAppearance = compositionLocalOf { GlassAppearance() }

internal fun BackdropEffectScope.appearanceEffects(appearance: GlassAppearance, quality: Float = 1f) {
    val p = appearance.current
    blur(p.blur * density * quality)
    when (appearance.style) {
        GlassStyle.LIQUID -> if (p.lensHeight > 0f && p.lensAmount > 0f) {
            lens(p.lensHeight * density * quality, p.lensAmount * density * quality,
                chromaticAberration = p.dispersion && quality >= .9f)
        }
        GlassStyle.GAUSSIAN -> Unit
        GlassStyle.SOFT -> {
            colorControls(p.brightness, p.contrast, p.saturation)
            if (p.noise > 0f) runtimeShaderEffect("gnomon-soft-grain", """
                uniform shader source;
                uniform float strength;
                half4 main(float2 xy) {
                    half4 c = source.eval(xy);
                    float n = fract(sin(dot(floor(xy), float2(12.9898,78.233))) * 43758.5453) - 0.5;
                    return half4(clamp(c.rgb + half(n * strength) * c.a, 0.0, c.a), c.a);
                }
            """.trimIndent(), "source") { setFloatUniform("strength", p.noise) }
        }
    }
}
