#version 330
#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;
uniform sampler2D SceneDepthSampler;
uniform sampler2D PhaseTearDataSampler;
uniform sampler2D PhaseTearMaskSampler;

in vec2 texCoord;

out vec4 fragColor;

const int MAX_EFFECTS = 8;

float decodeU16(vec2 packed) {
    vec2 bytes = round(clamp(packed, 0.0, 1.0) * 255.0);
    return (bytes.x * 256.0 + bytes.y) / 65535.0;
}

vec2 decodePair(ivec2 texelCoord) {
    vec4 encoded = texelFetch(PhaseTearDataSampler, texelCoord, 0);
    return vec2(
        decodeU16(encoded.rg),
        decodeU16(encoded.ba)
    );
}

float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

vec3 applyFullScreenPhase(vec3 baseColor) {
    float gray = luma(baseColor);
    vec2 center = texCoord - vec2(0.5);
    float vignette = smoothstep(0.92, 0.20, length(center));

    vec3 darkColor = vec3(0.04, 0.07, 0.13);
    vec3 midColor = vec3(0.24, 0.34, 0.50);
    vec3 lightColor = vec3(0.64, 0.74, 0.86);

    float tone = smoothstep(0.05, 0.95, gray);
    vec3 phase = mix(darkColor, midColor, tone);
    phase = mix(phase, lightColor, smoothstep(0.72, 1.0, tone));
    phase = mix(baseColor * vec3(0.32, 0.39, 0.52), phase, 0.30);
    phase *= mix(0.86, 1.0, vignette);
    return clamp(phase * 2.0, 0.0, 1.0);
}

void main() {
    vec2 headerFlags = decodePair(ivec2(0, 0));
    float localInVoid = headerFlags.x;
    float time = headerFlags.y * 128.0;

    vec2 pixelSize = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 totalOffset = vec2(0.0);
    float strongestEdge = 0.0;
    float membranePresence = 0.0;

    vec4 maskSample = texture(PhaseTearMaskSampler, texCoord);
    int effectIndex = int(round(maskSample.b * 255.0)) - 1;
    if (maskSample.a > 0.001 && effectIndex >= 0 && effectIndex < MAX_EFFECTS) {
        int row = effectIndex + 1;
        vec2 state = decodePair(ivec2(0, row));
        vec2 style = decodePair(ivec2(1, row));
        vec2 detail = decodePair(ivec2(2, row));

        float progress = state.x;
        float amplitude = state.y;
        float thickness = style.x;
        float alpha = style.y;

        vec2 normalized = maskSample.rg * 2.0 - 1.0;
        float dist = length(normalized);
        float maskAlpha = 1.0 - smoothstep(0.62, 1.0, dist);

        if (progress > 0.0001 && alpha > 0.0001 && maskAlpha > 0.0001) {
            float collapse = smoothstep(0.20, 1.0, progress);
            float expand = smoothstep(0.0, 0.18, progress);
            float fade = (1.0 - smoothstep(0.78, 1.0, progress)) * alpha;

            float centerMask = (1.0 - smoothstep(0.0, 0.58, dist)) * maskAlpha;
            float midMask = smoothstep(0.12, 0.72, dist) * (1.0 - smoothstep(0.76, 1.02, dist)) * maskAlpha;
            float rimMask = smoothstep(0.44, 0.98, dist) * (1.0 - smoothstep(1.0, 1.08, dist));
            rimMask *= maskAlpha;
            float membraneMask = maskAlpha * (0.62 + 0.38 * midMask);

            float noiseFrequency = mix(2.6, 15.0, detail.x);
            float noiseSpeed = mix(0.12, 2.80, detail.y);
            float angle = atan(normalized.y, normalized.x);
            float flowNoise = noise(normalized * noiseFrequency + vec2(time * 0.34 * noiseSpeed, -time * 0.22 * noiseSpeed));
            float foldNoise = noise((normalized + vec2(1.8, -3.2)) * (noiseFrequency * 0.74 + thickness * 3.4)
                    + vec2(-time * 0.24 * noiseSpeed, time * 0.37 * noiseSpeed));
            float breakupNoise = noise((normalized * vec2(0.72, 1.08) + vec2(flowNoise, foldNoise))
                    * (noiseFrequency * 0.52 + 1.2)
                    + vec2(6.8, 13.7));

            vec2 radial = dist > 0.0001 ? normalize(normalized) : vec2(0.0, 1.0);
            vec2 tangent = vec2(-radial.y, radial.x);
            vec2 warpNoise = vec2(flowNoise - 0.5, foldNoise - 0.5);
            float ripple = sin(angle * 3.0 + time * (0.52 + noiseSpeed * 0.46) + breakupNoise * 6.2);
            vec2 membraneDirection = normalize(
                    warpNoise * 1.18
                    + radial * (0.42 + 0.58 * centerMask + (breakupNoise - 0.5) * 0.55)
                    + tangent * ((flowNoise - 0.5) * 1.18 + ripple * 0.30 + rimMask * 0.18)
            );

            float baseStrength = amplitude
                    * fade
                    * (0.52 + 0.48 * expand)
                    * (1.0 - collapse * 0.26);
            float bulkStrength = baseStrength
                    * membraneMask
                    * (0.014 + 0.020 * breakupNoise)
                    * (0.66 + 0.34 * centerMask);
            float sheetStrength = baseStrength
                    * midMask
                    * (0.012 + 0.016 * flowNoise)
                    * (0.58 + 0.42 * breakupNoise);
            float rimStrength = baseStrength
                    * rimMask
                    * (0.010 + 0.014 * foldNoise)
                    * (0.58 + 0.42 * thickness);
            float lensStrength = baseStrength
                    * centerMask
                    * (0.008 + 0.006 * breakupNoise);

            totalOffset += membraneDirection * bulkStrength;
            totalOffset += tangent * (((flowNoise - 0.5) * 1.32) + ripple * 0.24) * sheetStrength;
            totalOffset += radial * ((breakupNoise - 0.38) * 1.16) * rimStrength;
            totalOffset += radial * lensStrength;
            strongestEdge = max(strongestEdge, rimMask * fade);
            membranePresence = max(membranePresence, maskAlpha * fade);
        }
    }

    vec2 uv = clamp(texCoord + totalOffset, pixelSize * 0.5, vec2(1.0) - pixelSize * 0.5);
    vec3 color = texture(InSampler, uv).rgb;

    if (localInVoid > 0.5) {
        color = mix(color, applyFullScreenPhase(color), 0.84);
    }

    color += strongestEdge * vec3(0.020, 0.028, 0.040);
    color = mix(color, color + vec3(0.032, 0.046, 0.062), membranePresence * 0.12);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
