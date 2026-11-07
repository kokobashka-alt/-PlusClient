#version 150

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 resolutionTime;
    vec4 color1;
    vec4 color2;
    vec4 parameters;
    vec4 transparency;
} data;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = p * 2.02 + vec2(8.4, 5.7);
        amplitude *= 0.5;
    }
    return value;
}

float ridged(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 4; i++) {
        float ridge = 1.0 - abs(noise(p) * 2.0 - 1.0);
        value += ridge * amplitude;
        p = p * 2.18 + vec2(3.1, 9.2);
        amplitude *= 0.52;
    }
    return value;
}

float getEdge(vec2 uv, float size) {
    float center = texture(MaskSampler, uv).a;
    float right = texture(MaskSampler, uv + vec2(data.resolutionTime.x * size, 0.0)).a;
    float left = texture(MaskSampler, uv - vec2(data.resolutionTime.x * size, 0.0)).a;
    float down = texture(MaskSampler, uv + vec2(0.0, data.resolutionTime.y * size)).a;
    float up = texture(MaskSampler, uv - vec2(0.0, data.resolutionTime.y * size)).a;
    float edge = abs(center - right) + abs(center - left) + abs(center - down) + abs(center - up);
    float diagonal1 = texture(MaskSampler, uv + vec2(data.resolutionTime.x * size, data.resolutionTime.y * size)).a;
    float diagonal2 = texture(MaskSampler, uv + vec2(-data.resolutionTime.x * size, data.resolutionTime.y * size)).a;
    float diagonal3 = texture(MaskSampler, uv + vec2(data.resolutionTime.x * size, -data.resolutionTime.y * size)).a;
    float diagonal4 = texture(MaskSampler, uv - vec2(data.resolutionTime.x * size, data.resolutionTime.y * size)).a;
    edge += 0.7 * (abs(center - diagonal1) + abs(center - diagonal2) + abs(center - diagonal3) + abs(center - diagonal4));
    return clamp(edge * 1.9, 0.0, 1.0);
}

void main() {
    vec4 maskSample = texture(MaskSampler, TexCoord);
    float mask = maskSample.a;
    if (mask <= 0.0) discard;
    vec3 itemColor = clamp(maskSample.rgb / max(mask, 0.001), 0.0, 1.0);

    float edge = getEdge(TexCoord, data.parameters.y);
    float edgeBand = smoothstep(0.025, 0.48, edge);
    float currentTime = data.resolutionTime.z;
    float speed = max(data.resolutionTime.w, 0.001);
    vec2 flow = TexCoord * mix(1.2, 3.2, clamp(data.parameters.x / 3.0, 0.0, 1.0));
    vec2 drift = vec2(currentTime * speed * 0.20, -currentTime * speed * 0.15);
    vec2 warp = vec2(
        fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
        fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8))
    );
    vec2 warpedFlow = flow + (warp - 0.5) * 1.8;
    float mist = fbm(warpedFlow * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
    float veins = ridged(warpedFlow * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55);
    veins = pow(clamp(veins, 0.0, 1.0), 2.4);
    float stripeA = 1.0 - abs(sin((warpedFlow.x * 1.08 + warpedFlow.y * 0.42) * 1.7 + currentTime * speed * 0.85 + mist * 4.3));
    float stripeB = 1.0 - abs(sin((warpedFlow.x * -0.58 + warpedFlow.y * 1.12) * 1.45 - currentTime * speed * 0.65 - mist * 2.9));
    stripeA = pow(clamp(stripeA, 0.0, 1.0), 4.8);
    stripeB = pow(clamp(stripeB, 0.0, 1.0), 5.4);
    float energy = clamp(mist * 0.22 + veins * 0.88 + stripeA * 0.55 + stripeB * 0.32, 0.0, 1.0);
    float core = smoothstep(0.18, 0.98, energy);
    float accent = pow(clamp(max(veins, stripeA), 0.0, 1.0), 1.25);
    vec3 outlineColor = mix(data.color1.rgb, data.color2.rgb, 0.35);
    vec3 shaderColor = mix(data.color1.rgb, data.color2.rgb, clamp(core * 0.75 + stripeB * 0.25, 0.0, 1.0));
    float itemColorMix = clamp(data.transparency.y, 0.0, 1.0);
    outlineColor = mix(outlineColor, itemColor, itemColorMix);
    shaderColor = mix(shaderColor, itemColor, itemColorMix);
    float creamyHighlight = clamp(0.045 + core * 0.08 + stripeA * 0.075, 0.0, 0.19)
            * mix(1.0, 0.22, itemColorMix);
    vec3 creamTint = vec3(1.0, 0.985, 0.975);
    outlineColor = mix(outlineColor, creamTint, 0.075 * mix(1.0, 0.22, itemColorMix));
    shaderColor = mix(shaderColor, creamTint, creamyHighlight);
    float innerMask = clamp(mask - edgeBand * 0.58, 0.0, 1.0);
    float fillStrength = data.parameters.w * innerMask * (0.32 + core * 0.72 + accent * 0.24);
    float edgeStrength = edgeBand * (0.32 + data.parameters.z * 0.11);
    float fillAlpha = clamp(fillStrength * 0.74, 0.0, 1.0);
    float edgeAlpha = clamp(edgeStrength * 0.52, 0.0, 1.0);
    float combined = max(fillAlpha + edgeAlpha, 0.001);
    vec3 rgb = (shaderColor * fillAlpha + outlineColor * edgeAlpha) / combined;
    float outputAlpha = clamp(data.transparency.x * combined * mask, 0.0, 1.0);
    if (outputAlpha <= 0.001) discard;
    OutColor = vec4(rgb, outputAlpha);
}
