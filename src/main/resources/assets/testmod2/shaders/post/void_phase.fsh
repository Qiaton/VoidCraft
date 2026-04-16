#version 330
#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform VoidMotionConfig {
    vec2 FlowDir;
    float RadialFlow;
    float FlowSpeed;
};

out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec2 center = texCoord - vec2(0.5);
    float radius = dot(center, center);
    vec2 uv = texCoord + center * radius * 0.008;

    vec2 pixelSize = 1.0 / vec2(textureSize(InSampler, 0));
    uv = clamp(uv, pixelSize * 0.5, vec2(1.0) - pixelSize * 0.5);

    vec3 base = texture(InSampler, uv).rgb;
    float gray = luma(base);

    float gx = 0.0;
    float gy = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y)) * pixelSize;
            float s = luma(texture(InSampler, uv + offset).rgb);

            float kx = 0.0;
            float ky = 0.0;
            if (x == -1) kx = -1.0;
            if (x == 1)  kx = 1.0;
            if (y == -1) ky = -1.0;
            if (y == 1)  ky = 1.0;
            if (x != 0 && y == 0) kx *= 2.0;
            if (y != 0 && x == 0) ky *= 2.0;

            gx += s * kx;
            gy += s * ky;
        }
    }

    float edge = clamp(length(vec2(gx, gy)) * 0.54, 0.0, 1.0);
    float tone = smoothstep(0.04, 0.95, gray);

    vec3 darkColor = vec3(0.04, 0.07, 0.13);
    vec3 midColor = vec3(0.24, 0.34, 0.50);
    vec3 lightColor = vec3(0.64, 0.74, 0.86);

    vec3 phase = mix(darkColor, midColor, tone);
    phase = mix(phase, lightColor, smoothstep(0.72, 1.0, tone));

    vec3 keepDetail = base * vec3(0.32, 0.39, 0.52);
    phase = mix(keepDetail, phase, 0.30);

    phase = mix(phase, phase * 0.76, edge * 0.58);
    phase += edge * vec3(0.03, 0.05, 0.07);

    float vignette = smoothstep(0.90, 0.24, length(center));
    phase *= mix(0.86, 1.0, vignette);

    phase *= 2.00;

    fragColor = vec4(clamp(phase, 0.0, 1.0), 1.0);
}
