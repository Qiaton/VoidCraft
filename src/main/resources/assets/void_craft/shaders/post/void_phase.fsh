#version 330
#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;
uniform sampler2D SceneDepthSampler;
uniform sampler2D PhaseTearDataSampler;
uniform sampler2D PhaseTearMaskSampler;

in vec2 texCoord;

out vec4 fragColor;

const int MAX_EFFECTS = 16;

float decodeU16(vec2 packedBytes) {
    vec2 bytes = round(clamp(packedBytes, 0.0, 1.0) * 255.0);
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

void accumulateBlackHoleEffect(
    vec2 normalized,
    float maskAlpha,
    float progress,
    float amplitude,
    float thickness,
    float alpha,
    vec2 detail,
    float swirlStrength,
    float suctionStrength,
    float centerShadowScale,
    float time,
    inout vec2 totalOffset,
    inout float strongestEdge,
    inout float membranePresence,
    inout float blackHoleShadow,
    inout float accretionPresence
) {
    float dist = length(normalized);
    float collapse = smoothstep(0.20, 1.0, progress);
    float expand = smoothstep(0.0, 0.18, progress);
    float fade = (1.0 - smoothstep(0.78, 1.0, progress)) * alpha;
    if (fade <= 0.0001) {
        return;
    }

    float noiseFrequency = mix(3.0, 18.0, detail.x);
    float noiseSpeed = mix(0.16, 3.20, detail.y);
    float angle = atan(normalized.y, normalized.x);
    float flowNoise = noise(normalized * noiseFrequency + vec2(time * 0.48 * noiseSpeed, -time * 0.30 * noiseSpeed));
    float foldNoise = noise((normalized + vec2(4.1, -2.7)) * (noiseFrequency * 0.62 + thickness * 4.2)
            + vec2(-time * 0.34 * noiseSpeed, time * 0.42 * noiseSpeed));
    float spiralFlow = fract(angle * 0.15915494 + dist * 0.44 - time * (0.10 + noiseSpeed * 0.035));
    float spiralBand = smoothstep(0.0, 0.22, spiralFlow) * (1.0 - smoothstep(0.52, 1.0, spiralFlow));

    vec2 radial = dist > 0.0001 ? normalize(normalized) : vec2(0.0, 1.0);
    vec2 tangent = vec2(-radial.y, radial.x);
    float diskWave = (flowNoise - 0.5) * 0.018 + (spiralBand - 0.5) * 0.010;
    float diskY = abs(normalized.y + diskWave);
    float diskX = abs(normalized.x);

    float horizonMask = (1.0 - smoothstep(0.18, 0.56, dist)) * maskAlpha;
    float horizonEdge = smoothstep(0.34, 0.58, dist) * (1.0 - smoothstep(0.58, 0.76, dist)) * maskAlpha;
    float lensRing = smoothstep(0.38, 0.74, dist) * (1.0 - smoothstep(0.92, 1.14, dist)) * maskAlpha;
    float outerSwirlMask = smoothstep(0.46, 0.90, dist) * (1.0 - smoothstep(0.98, 1.16, dist)) * maskAlpha;
    float diskMask = (1.0 - smoothstep(0.045, 0.24, diskY))
            * smoothstep(0.22, 0.42, diskX)
            * (1.0 - smoothstep(0.86, 1.18, diskX))
            * (0.70 + 0.22 * flowNoise + 0.08 * spiralBand)
            * maskAlpha;

    float baseStrength = amplitude
            * fade
            * (0.60 + 0.40 * expand)
            * (1.0 - collapse * 0.22);
    float lensStrength = baseStrength
            * lensRing
            * (0.026 + 0.018 * thickness)
            * (0.72 + 0.28 * foldNoise);
    float horizonPullStrength = baseStrength
            * suctionStrength
            * (horizonEdge * 0.72 + lensRing * 0.62 + outerSwirlMask * 0.44)
            * (0.016 + 0.018 * horizonMask);
    float swirlPullStrength = baseStrength
            * swirlStrength
            * (outerSwirlMask + diskMask * 0.42)
            * (0.012 + 0.018 * thickness)
            * (0.84 + 0.16 * spiralBand);

    totalOffset += radial * (lensStrength + horizonPullStrength);
    totalOffset += tangent * swirlPullStrength;
    totalOffset += normalize(tangent * 0.68 + radial * 0.46) * diskMask * baseStrength * 0.006 * (0.70 + 0.30 * flowNoise);

    strongestEdge = max(strongestEdge, max(horizonEdge, lensRing * 0.48) * fade);
    membranePresence = max(membranePresence, lensRing * fade * 0.32);
    blackHoleShadow = max(blackHoleShadow, horizonMask * fade * (0.86 + 0.14 * expand) * centerShadowScale);
    accretionPresence = max(accretionPresence, (diskMask + horizonEdge * 0.28) * fade);
}

void accumulatePhaseEffect(
    int row,
    vec2 normalized,
    float maskAlpha,
    float time,
    inout vec2 totalOffset,
    inout float strongestEdge,
    inout float membranePresence,
    inout float blackHoleShadow,
    inout float accretionPresence
) {
    vec2 state = decodePair(ivec2(0, row));
    vec2 style = decodePair(ivec2(1, row));
    vec2 detail = decodePair(ivec2(2, row));
    vec2 pull = decodePair(ivec2(5, row));
    vec2 occlusion = decodePair(ivec2(6, row));
    vec2 shape = decodePair(ivec2(7, row));

    float progress = state.x;
    float amplitude = state.y;
    float thickness = style.x;
    float alpha = style.y;
    float swirlStrength = pull.x;
    float suctionStrength = pull.y;
    float occlusionEnabled = occlusion.x;
    float effectDepth = occlusion.y;
    float centerShadowScale = clamp(shape.y, 0.0, 1.0);
    float dist = length(normalized);

    if (progress <= 0.0001 || alpha <= 0.0001 || maskAlpha <= 0.0001) {
        return;
    }

    if (occlusionEnabled > 0.5) {
        float sceneDepth = texture(SceneDepthSampler, texCoord).r;
        float depthVisibility = smoothstep(effectDepth - 0.0015, effectDepth + 0.0005, sceneDepth);
        maskAlpha *= depthVisibility;
        if (maskAlpha <= 0.0001) {
            return;
        }
    }

    if (shape.x > 0.5) {
        accumulateBlackHoleEffect(
            normalized,
            maskAlpha,
            progress,
            amplitude,
            thickness,
            alpha,
            detail,
            swirlStrength,
            suctionStrength,
            centerShadowScale,
            time,
            totalOffset,
            strongestEdge,
            membranePresence,
            blackHoleShadow,
            accretionPresence
        );
        return;
    }

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
    float swirlMask = smoothstep(0.08, 0.86, dist) * (1.0 - smoothstep(1.0, 1.12, dist)) * maskAlpha;
    float suctionMask = (1.0 - smoothstep(0.0, 1.04, dist)) * maskAlpha;
    float swirlPulse = 0.72 + 0.28 * sin(time * (1.2 + noiseSpeed * 0.35) + dist * 4.5 + breakupNoise * 2.8);
    float swirlPullStrength = baseStrength
            * swirlStrength
            * swirlMask
            * (0.010 + 0.018 * thickness)
            * swirlPulse;
    float suctionPullStrength = baseStrength
            * suctionStrength
            * suctionMask
            * (0.010 + 0.018 * centerMask + 0.012 * rimMask)
            * (1.0 - collapse * 0.18);

    totalOffset += membraneDirection * bulkStrength;
    totalOffset += tangent * (((flowNoise - 0.5) * 1.32) + ripple * 0.24) * sheetStrength;
    totalOffset += radial * ((breakupNoise - 0.38) * 1.16) * rimStrength;
    totalOffset += radial * lensStrength;
    totalOffset += tangent * swirlPullStrength;
    totalOffset += radial * suctionPullStrength;
    strongestEdge = max(strongestEdge, rimMask * fade);
    membranePresence = max(membranePresence, maskAlpha * fade);
}

void accumulateAnalyticPhaseEffects(
    float time,
    inout vec2 totalOffset,
    inout float strongestEdge,
    inout float membranePresence,
    inout float blackHoleShadow,
    inout float accretionPresence
) {
    for (int row = 1; row <= MAX_EFFECTS; row++) {
        vec2 screenCenter = decodePair(ivec2(3, row));
        vec2 screenExtents = decodePair(ivec2(4, row));
        if (screenExtents.x <= 0.0001 || screenExtents.y <= 0.0001) {
            continue;
        }

        vec2 normalized = (texCoord - screenCenter) / max(screenExtents, vec2(0.0001));
        float dist = length(normalized);
        float maskAlpha = 1.0 - smoothstep(0.62, 1.0, dist);
        if (maskAlpha <= 0.0001) {
            continue;
        }

        accumulatePhaseEffect(
            row,
            normalized,
            maskAlpha,
            time,
            totalOffset,
            strongestEdge,
            membranePresence,
            blackHoleShadow,
            accretionPresence
        );
    }
}

void main() {
    vec2 headerFlags = decodePair(ivec2(0, 0));
    float localInVoid = headerFlags.x;
    float time = headerFlags.y * 128.0;

    vec2 pixelSize = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 totalOffset = vec2(0.0);
    float strongestEdge = 0.0;
    float membranePresence = 0.0;
    float blackHoleShadow = 0.0;
    float accretionPresence = 0.0;

    vec4 maskSample = texture(PhaseTearMaskSampler, texCoord);
    int effectIndex = int(round(maskSample.b * 255.0)) - 1;
    bool usedRasterMask = false;
    if (maskSample.a > 0.001 && effectIndex >= 0 && effectIndex < MAX_EFFECTS) {
        vec2 normalized = maskSample.rg * 2.0 - 1.0;
        float dist = length(normalized);
        float maskAlpha = 1.0 - smoothstep(0.62, 1.0, dist);
        if (maskAlpha > 0.0001) {
            usedRasterMask = true;
            accumulatePhaseEffect(
                effectIndex + 1,
                normalized,
                maskAlpha,
                time,
                totalOffset,
                strongestEdge,
                membranePresence,
                blackHoleShadow,
                accretionPresence
            );
        }
    }

    if (!usedRasterMask) {
        accumulateAnalyticPhaseEffects(
            time,
            totalOffset,
            strongestEdge,
            membranePresence,
            blackHoleShadow,
            accretionPresence
        );
    }

    vec2 uv = clamp(texCoord + totalOffset, pixelSize * 0.5, vec2(1.0) - pixelSize * 0.5);
    vec3 color = texture(InSampler, uv).rgb;

    if (localInVoid > 0.5) {
        color = mix(color, applyFullScreenPhase(color), 0.84);
    }

    color = mix(color, color * vec3(0.024, 0.028, 0.060), clamp(blackHoleShadow, 0.0, 1.0));
    color += clamp(accretionPresence, 0.0, 1.0) * vec3(0.026, 0.034, 0.080);
    color += strongestEdge * vec3(0.020, 0.028, 0.040);
    color = mix(color, color + vec3(0.032, 0.046, 0.062), membranePresence * 0.12);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
