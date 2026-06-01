#version 330
#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;
uniform sampler2D SceneDepthSampler;
uniform sampler2D PhaseTearDataSampler;
uniform sampler2D PhaseTearMaskSampler;

in vec2 texCoord;

out vec4 fragColor;

const int MAX_EFFECTS = 16;
const float SCREEN_AXIS_PACK_RANGE = 2.0;

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

vec2 decodeSignedPair(ivec2 texelCoord) {
    return (decodePair(texelCoord) * 2.0 - 1.0) * SCREEN_AXIS_PACK_RANGE;
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

vec3 applyPhasePreLight(vec3 baseColor, float strength) {
    float gray = luma(baseColor);
    float shadow = 1.0 - smoothstep(0.10, 0.60, gray);
    vec3 lightColor = baseColor + vec3(0.030, 0.038, 0.050);

    lightColor = mix(lightColor, sqrt(clamp(lightColor, 0.0, 1.0)), shadow * 0.18);
    return mix(baseColor, clamp(lightColor, 0.0, 1.0), strength);
}

vec3 applyPhaseContrast(vec3 baseColor, float strength) {
    float gray = luma(baseColor);
    vec3 contrastColor = (baseColor - vec3(0.46)) * 1.20 + vec3(0.46);
    float shadow = 1.0 - smoothstep(0.10, 0.48, gray);
    float light = smoothstep(0.38, 0.84, gray);
    float highLight = smoothstep(0.66, 1.0, gray);

    contrastColor *= 1.0 - shadow * 0.30;
    contrastColor = mix(contrastColor, contrastColor * vec3(0.94, 0.98, 1.06), shadow * 0.16);
    contrastColor += (vec3(1.0) - contrastColor) * (light * 0.26 + highLight * 0.24) * vec3(0.88, 0.98, 1.12);
    contrastColor += vec3(0.018, 0.028, 0.044) * light;
    return mix(baseColor, clamp(contrastColor, 0.0, 1.0), strength);
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
    vec2 coreMaskScale,
    float flatGate,
    float centerShadowScale,
    float time,
    inout vec2 totalOffset,
    inout float strongestEdge,
    inout float membranePresence,
    inout float blackHoleShadow
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
    float tearNoise = noise(vec2(cos(angle) * 2.4 + time * 0.10 * noiseSpeed, sin(angle) * 2.4 - time * 0.08 * noiseSpeed));
    float thickEdgeWave = 1.0
            + (tearNoise - 0.5) * 0.12
            + sin(angle * 5.0 + time * 0.20 * noiseSpeed) * 0.025
            + sin(angle * 11.0 - time * 0.14 * noiseSpeed) * 0.018;
    float flatEdgeWave = 1.0
            + (tearNoise - 0.5) * 0.035
            + sin(angle * 7.0 + time * 0.12 * noiseSpeed) * 0.006;
    float edgeWave = mix(thickEdgeWave, flatEdgeWave, flatGate);

    vec2 radial = dist > 0.0001 ? normalize(normalized) : vec2(0.0, 1.0);
    vec2 tangent = vec2(-radial.y, radial.x);

    vec2 coreNormalized = normalized / max(coreMaskScale, vec2(0.001));
    float coreDist = length(coreNormalized);
    float gateDist = coreDist / max(0.001, edgeWave);
    float holeStart = mix(0.78, 0.92, flatGate);
    float holeEnd = mix(0.96, 0.985, flatGate);
    float edgeStart = mix(0.78, 0.93, flatGate);
    float edgePeak = mix(0.99, 0.985, flatGate);
    float edgeLeave = mix(1.00, 0.995, flatGate);
    float edgeEnd = mix(1.24, 1.065, flatGate);
    float gateHole = (1.0 - smoothstep(holeStart, holeEnd, gateDist)) * maskAlpha;
    float gateEdge = smoothstep(edgeStart, edgePeak, gateDist)
            * (1.0 - smoothstep(edgeLeave, edgeEnd, gateDist))
            * maskAlpha
            * mix(0.72 + 0.28 * tearNoise, 0.86 + 0.14 * tearNoise, flatGate);
    float gripStart = mix(0.86, 0.92, flatGate);
    float gripEnd = mix(1.22, 1.04, flatGate);
    float gripFadeStart = mix(0.86, 0.95, flatGate);
    float gripFadeEnd = mix(1.0, 1.08, flatGate);
    float gateGrip = smoothstep(gripStart, gripEnd, gateDist)
            * (1.0 - smoothstep(gripFadeStart, gripFadeEnd, dist))
            * maskAlpha;
    float gateLens = smoothstep(0.30, 1.08, gateDist)
            * (1.0 - smoothstep(0.78, 1.0, dist))
            * maskAlpha;

    float baseStrength = amplitude
            * fade
            * (0.70 + 0.30 * expand)
            * (1.0 - collapse * 0.18);
    float edgeBendStrength = baseStrength
            * gateEdge
            * (0.022 + 0.026 * thickness)
            * (0.72 + 0.28 * foldNoise);
    float gatePullStrength = baseStrength
            * suctionStrength
            * gateGrip
            * (0.038 + 0.048 * thickness)
            * (0.76 + 0.24 * foldNoise);
    float gateLensStrength = baseStrength
            * gateLens
            * (0.012 + 0.014 * thickness)
            * (0.70 + 0.30 * flowNoise);
    float gateTurnStrength = baseStrength
            * swirlStrength
            * gateGrip
            * (0.012 + 0.020 * thickness)
            * (0.62 + 0.38 * tearNoise);

    totalOffset += radial * (edgeBendStrength + gatePullStrength + gateLensStrength);
    totalOffset += tangent * gateTurnStrength;

    float gateVisible = centerShadowScale;
    strongestEdge = max(strongestEdge, gateEdge * fade * 1.35 * gateVisible);
    membranePresence = max(membranePresence, (gateEdge * 0.55 + gateGrip * 0.18) * fade * gateVisible);
    blackHoleShadow = max(blackHoleShadow, (gateHole * 1.18 + gateEdge * 0.42) * fade * centerShadowScale);
}

void accumulatePhaseEffect(
    int row,
    vec2 normalized,
    float maskAlpha,
    float time,
    inout vec2 totalOffset,
    inout float strongestEdge,
    inout float membranePresence,
    inout float blackHoleShadow
) {
    vec2 state = decodePair(ivec2(0, row));
    vec2 style = decodePair(ivec2(1, row));
    vec2 detail = decodePair(ivec2(2, row));
    vec2 pull = decodePair(ivec2(5, row));
    vec2 occlusion = decodePair(ivec2(6, row));
    vec2 shape = decodePair(ivec2(7, row));
    vec2 coreMaskScale = decodePair(ivec2(8, row));
    vec2 gateMode = decodePair(ivec2(9, row));

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
            coreMaskScale,
            step(0.5, gateMode.x),
            centerShadowScale,
            time,
            totalOffset,
            strongestEdge,
            membranePresence,
            blackHoleShadow
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
    inout float blackHoleShadow
) {
    for (int row = 1; row <= MAX_EFFECTS; row++) {
        vec2 screenCenter = decodePair(ivec2(3, row));
        vec2 screenExtents = decodePair(ivec2(4, row));
        if (screenExtents.x <= 0.0001 || screenExtents.y <= 0.0001) {
            continue;
        }

        vec2 rightAxis = decodeSignedPair(ivec2(10, row));
        vec2 upAxis = decodeSignedPair(ivec2(11, row));
        vec2 delta = texCoord - screenCenter;
        float determinant = rightAxis.x * upAxis.y - rightAxis.y * upAxis.x;
        vec2 normalized = (texCoord - screenCenter) / max(screenExtents, vec2(0.0001));
        if (abs(determinant) > 0.000001 && length(rightAxis) > 0.0001 && length(upAxis) > 0.0001) {
            normalized = vec2(
                (delta.x * upAxis.y - delta.y * upAxis.x) / determinant,
                (rightAxis.x * delta.y - rightAxis.y * delta.x) / determinant
            );
        }
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
            blackHoleShadow
        );
    }
}

float easeInOut(float value) {
    float t = clamp(value, 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

vec2 transitionAspectPoint(vec2 uv) {
    vec2 texSize = vec2(textureSize(InSampler, 0));
    float aspect = texSize.x / max(texSize.y, 1.0);
    vec2 point = uv - vec2(0.5);
    point.x *= aspect;
    return point;
}

float transitionCoverRadius() {
    vec2 corner = abs(transitionAspectPoint(vec2(1.0, 1.0)));
    return length(corner) + 0.08;
}

float transitionInsideEllipse(float dist, float radius, float edgeWidth) {
    float outerRadius = max(radius, 0.0001);
    return 1.0 - smoothstep(max(0.0, outerRadius - edgeWidth), outerRadius, dist);
}

vec2 phaseWorldTransitionOffset(float enterProgress, float exitProgress, float holdWhite, float transitionActive, float time) {
    if (transitionActive < 0.5) {
        return vec2(0.0);
    }

    vec2 aspectPoint = transitionAspectPoint(texCoord);
    float dist = length(aspectPoint);
    float coverRadius = transitionCoverRadius();
    vec2 dir = dist > 0.0001 ? normalize(texCoord - vec2(0.5)) : vec2(0.0);
    float flowNoise = noise(texCoord * 18.0 + vec2(time * 0.42, -time * 0.31));
    float foldNoise = noise(texCoord * 42.0 + vec2(-time * 0.28, time * 0.36));
    float wave = sin(dist * 38.0 - time * 5.8 + flowNoise * 6.28318);
    float shimmer = mix(wave, foldNoise * 2.0 - 1.0, 0.34);

    float enterMask = 0.0;
    if (enterProgress > 0.0) {
        float radius = coverRadius * easeInOut(enterProgress);
        enterMask = transitionInsideEllipse(dist, radius, 0.18);
        enterMask *= 1.0 - smoothstep(0.74, 1.0, enterProgress) * 0.72;
    }

    float exitMask = 0.0;
    if (exitProgress > 0.5) {
        float collapse = easeInOut((exitProgress - 0.5) * 2.0);
        float radius = coverRadius * (1.0 - collapse);
        exitMask = transitionInsideEllipse(dist, radius, 0.20) * (1.0 - collapse * 0.55);
    }

    float mask = max(enterMask, exitMask) * (1.0 - holdWhite);
    vec2 tangent = vec2(-dir.y, dir.x);
    return (dir * shimmer * 0.020 + tangent * wave * 0.010) * mask;
}

float voidInOutEllipseMask(float progress, float edgeWidth) {
    if (progress <= 0.0) {
        return 0.0;
    }

    float dist = length(transitionAspectPoint(texCoord));
    float radius = transitionCoverRadius() * easeInOut(progress);
    return transitionInsideEllipse(dist, radius, edgeWidth);
}

vec2 voidInOutOffset(float warpProgress, float maskProgress, float effectOn, float time) {
    if (effectOn < 0.5 || warpProgress <= 0.0) {
        return vec2(0.0);
    }

    vec2 aspectPoint = transitionAspectPoint(texCoord);
    float dist = length(aspectPoint);
    float radius = transitionCoverRadius() * easeInOut(warpProgress);
    vec2 dir = dist > 0.0001 ? normalize(texCoord - vec2(0.5)) : vec2(0.0);
    vec2 tangent = vec2(-dir.y, dir.x);
    float flowNoise = noise(texCoord * 22.0 + vec2(time * 0.36, -time * 0.29));
    float foldNoise = noise(texCoord * 48.0 + vec2(-time * 0.22, time * 0.41));
    float wave = sin(dist * 44.0 - time * 6.4 + flowNoise * 6.28318);
    float innerEdge = max(0.0, radius - 0.20);
    float outerEdge = max(radius, innerEdge + 0.0001);
    float edgeMask = smoothstep(innerEdge, outerEdge, dist)
            * (1.0 - smoothstep(radius, radius + 0.16, dist));
    float insideMask = transitionInsideEllipse(dist, radius, 0.18);
    float strength = max(edgeMask, insideMask * 0.42) * (1.0 - maskProgress * 0.65);
    float shimmer = mix(wave, foldNoise * 2.0 - 1.0, 0.38);
    return (dir * shimmer * 0.022 + tangent * wave * 0.012) * strength;
}

float voidInOutWhite(float warpProgress, float maskProgress, float effectOn) {
    if (effectOn < 0.5) {
        return 0.0;
    }

    float dist = length(transitionAspectPoint(texCoord));
    float coverRadius = transitionCoverRadius();
    float warpRadius = coverRadius * easeInOut(warpProgress);
    float warpInnerEdge = max(0.0, warpRadius - 0.18);
    float warpOuterEdge = max(warpRadius, warpInnerEdge + 0.0001);
    float warpEdge = smoothstep(warpInnerEdge, warpOuterEdge, dist)
            * (1.0 - smoothstep(warpRadius, warpRadius + 0.20, dist));
    float warpFill = transitionInsideEllipse(dist, warpRadius, 0.26);
    float warpLight = (warpEdge * 0.46 + warpFill * 0.12) * (1.0 - maskProgress * 0.70);

    float maskRadius = coverRadius * easeInOut(maskProgress);
    float maskInnerEdge = max(0.0, maskRadius - 0.12);
    float maskOuterEdge = max(maskRadius, maskInnerEdge + 0.0001);
    float maskEdge = smoothstep(maskInnerEdge, maskOuterEdge, dist)
            * (1.0 - smoothstep(maskRadius, maskRadius + 0.14, dist))
            * smoothstep(0.0, 0.08, maskProgress);

    return clamp(warpLight + maskEdge * 0.30, 0.0, 0.62);
}

void main() {
    vec2 headerFlags = decodePair(ivec2(0, 0));
    vec2 transitionProgress = decodePair(ivec2(1, 0));
    vec2 transitionFlags = decodePair(ivec2(2, 0));
    vec2 voidInOutProgress = decodePair(ivec2(3, 0));
    vec2 voidInOutFlags = decodePair(ivec2(4, 0));
    float fullScreenPhaseStrength = clamp(headerFlags.x, 0.0, 1.0);
    float time = headerFlags.y * 128.0;
    float enterProgress = clamp(transitionProgress.x, 0.0, 1.0);
    float exitProgress = clamp(transitionProgress.y, 0.0, 1.0);
    float holdWhite = transitionFlags.x;
    float transitionStage = transitionFlags.y;
    float transitionActive = transitionStage > 0.001 ? 1.0 : 0.0;
    float voidInOutWarpProgress = clamp(voidInOutProgress.x, 0.0, 1.0);
    float voidInOutMaskProgress = clamp(voidInOutProgress.y, 0.0, 1.0);
    float voidInOutActive = voidInOutFlags.x;

    vec2 pixelSize = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 totalOffset = vec2(0.0);
    float strongestEdge = 0.0;
    float membranePresence = 0.0;
    float blackHoleShadow = 0.0;

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
                blackHoleShadow
            );
        }
    }

    if (!usedRasterMask) {
        accumulateAnalyticPhaseEffects(
            time,
            totalOffset,
            strongestEdge,
            membranePresence,
            blackHoleShadow
        );
    }

    totalOffset += phaseWorldTransitionOffset(enterProgress, exitProgress, holdWhite, transitionActive, time);
    totalOffset += voidInOutOffset(voidInOutWarpProgress, voidInOutMaskProgress, voidInOutActive, time);

    vec2 uv = clamp(texCoord + totalOffset, pixelSize * 0.5, vec2(1.0) - pixelSize * 0.5);
    vec3 color = texture(InSampler, uv).rgb;
    color = mix(color, texture(InSampler, texCoord).rgb, voidInOutEllipseMask(voidInOutMaskProgress, 0.18) * step(0.5, voidInOutActive));

    if (fullScreenPhaseStrength > 0.001) {
        color = applyPhasePreLight(color, fullScreenPhaseStrength);
        color = mix(color, applyFullScreenPhase(color), 0.84 * fullScreenPhaseStrength);
        color = applyPhaseContrast(color, fullScreenPhaseStrength);
    }

    color = mix(color, color * vec3(0.024, 0.028, 0.060), clamp(blackHoleShadow, 0.0, 1.0));
    color += strongestEdge * vec3(0.020, 0.028, 0.040);
    color = mix(color, color + vec3(0.032, 0.046, 0.062), membranePresence * 0.12);
    color = mix(color, vec3(1.0), voidInOutWhite(voidInOutWarpProgress, voidInOutMaskProgress, voidInOutActive));
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
