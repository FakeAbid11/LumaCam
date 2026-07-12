package com.lumacam.core.camera.film

/**
 * GLSL ES 2.0 shader sources for the Film Camera Engine (LumaCam). All effects run
 * in a single fragment pass: color-matrix grade, temperature, procedural grain,
 * approximate bloom/halation glow, vignette, chromatic aberration, scanlines and
 * softness. Effect strengths are supplied as uniforms (see
 * [com.lumacam.core.common.film.FilmPreset]) so switching presets never recompiles.
 *
 * Two fragment variants share one body: [FRAGMENT] samples the camera's external-OES
 * texture (live preview / video); [FRAGMENT_2D] samples a normal 2D texture (offscreen
 * photo baking). Shader-only — no texture assets — to keep the APK small (PRD).
 */
internal object FilmShaders {

    const val VERTEX = """
        uniform mat4 uTexMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = (uTexMatrix * aTextureCoord).xy;
        }
    """

    private const val OES_HEADER = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        uniform samplerExternalOES sTexture;
    """

    private const val TWO_D_HEADER = """
        precision mediump float;
        uniform sampler2D sTexture;
    """

    private const val BODY = """
        varying vec2 vTexCoord;
        uniform vec2 uResolution;
        uniform float uTime;
        uniform mat4 uColorMatrix;
        uniform vec4 uColorOffset;
        uniform float uGrain;
        uniform float uHalation;
        uniform float uBloom;
        uniform float uVignette;
        uniform float uChroma;
        uniform float uScanline;
        uniform float uSoftness;
        uniform float uTemperature;

        float hash(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        vec3 tex(vec2 uv) { return texture2D(sTexture, uv).rgb; }

        void main() {
            vec2 uv = vTexCoord;
            vec2 texel = 1.0 / uResolution;
            vec2 center = vec2(0.5, 0.5);
            vec2 dir = uv - center;

            vec3 color;
            if (uChroma > 0.001) {
                float ca = uChroma * 0.004;
                color.r = tex(uv + dir * ca).r;
                color.g = tex(uv).g;
                color.b = tex(uv - dir * ca).b;
            } else {
                color = tex(uv);
            }

            if (uSoftness > 0.001) {
                vec3 blur = tex(uv + vec2(texel.x, 0.0) * 1.5)
                          + tex(uv - vec2(texel.x, 0.0) * 1.5)
                          + tex(uv + vec2(0.0, texel.y) * 1.5)
                          + tex(uv - vec2(0.0, texel.y) * 1.5);
                blur *= 0.25;
                color = mix(color, blur, uSoftness);
            }

            if (uBloom > 0.001 || uHalation > 0.001) {
                float r = 3.0;
                vec3 glow = tex(uv + vec2(texel.x, texel.y) * r)
                          + tex(uv + vec2(-texel.x, texel.y) * r)
                          + tex(uv + vec2(texel.x, -texel.y) * r)
                          + tex(uv + vec2(-texel.x, -texel.y) * r)
                          + tex(uv + vec2(texel.x, 0.0) * r * 2.0)
                          + tex(uv + vec2(-texel.x, 0.0) * r * 2.0);
                glow *= (1.0 / 6.0);
                float lum = dot(glow, vec3(0.299, 0.587, 0.114));
                float bright = smoothstep(0.6, 1.0, lum);
                vec3 bloomColor = glow * bright;
                color += bloomColor * uBloom * 0.8;
                color += bloomColor * vec3(1.0, 0.5, 0.3) * uHalation;
            }

            vec4 graded = uColorMatrix * vec4(color, 1.0) + uColorOffset;
            color = clamp(graded.rgb, 0.0, 1.0);

            float t = (uTemperature - 0.5) * 2.0;
            color.r += t * 0.06;
            color.b -= t * 0.06;
            color = clamp(color, 0.0, 1.0);

            if (uScanline > 0.001) {
                float sl = sin(uv.y * uResolution.y * 1.5) * 0.5 + 0.5;
                color *= 1.0 - uScanline * 0.25 * sl;
            }

            if (uVignette > 0.001) {
                float d = distance(uv, center);
                float vig = smoothstep(0.8, 0.35, d);
                color *= mix(1.0, vig, uVignette);
            }

            if (uGrain > 0.001) {
                float n = hash(uv * uResolution + fract(uTime)) - 0.5;
                color += n * uGrain * 0.15;
            }

            gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
        }
    """

    val FRAGMENT: String = OES_HEADER + BODY
    val FRAGMENT_2D: String = TWO_D_HEADER + BODY
}
