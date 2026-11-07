#version 150

uniform sampler2D TrailSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 resolutionTime;
    vec4 color1;
    vec4 color2;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 pixel = data.resolutionTime.xy;
    float current = texture(MaskSampler, TexCoord).a;
    current = max(current, texture(MaskSampler, TexCoord + vec2(pixel.x, 0.0)).a);
    current = max(current, texture(MaskSampler, TexCoord - vec2(pixel.x, 0.0)).a);
    current = max(current, texture(MaskSampler, TexCoord + vec2(0.0, pixel.y)).a);
    current = max(current, texture(MaskSampler, TexCoord - vec2(0.0, pixel.y)).a);
    current = smoothstep(0.025, 0.72, current);
    vec4 historySample = texture(TrailSampler, TexCoord);
    vec4 neighborSamples = texture(TrailSampler, TexCoord + vec2(pixel.x, 0.0));
    neighborSamples += texture(TrailSampler, TexCoord - vec2(pixel.x, 0.0));
    neighborSamples += texture(TrailSampler, TexCoord + vec2(0.0, pixel.y));
    neighborSamples += texture(TrailSampler, TexCoord - vec2(0.0, pixel.y));
    vec4 softHistorySample = neighborSamples * 0.25;
    float history = historySample.a;
    float softHistory = softHistorySample.a;
    float currentSuppression = 1.0 - current;
    float coreGhost = history * currentSuppression;
    float softGhost = softHistory * currentSuppression;
    float ghost = clamp(coreGhost * 0.76 + softGhost * 0.28, 0.0, 1.0);
    float energy = pow(clamp(data.color2.a, 0.0, 1.0), 1.12);
    float ageBand = pow(max(sin(clamp(history, 0.0, 1.0) * 9.42477796), 0.0), 5.0);
    float alpha = pow(ghost, 1.16) * data.resolutionTime.w * energy;
    alpha *= 0.54 + ageBand * 0.16;
    if (alpha <= 0.002) discard;
    float flowA = 0.5 + 0.5 * sin(TexCoord.x * 8.0 + TexCoord.y * 11.0 - data.resolutionTime.z * 1.35);
    float flowB = 0.5 + 0.5 * sin(TexCoord.x * -5.0 + TexCoord.y * 7.0 + data.resolutionTime.z * 0.92);
    float flow = flowA * 0.62 + flowB * 0.38;
    vec3 gradient = mix(data.color2.rgb, data.color1.rgb, clamp(history * 0.64 + flow * 0.36, 0.0, 1.0));
    vec3 historyPremultiplied = (historySample.rgb * 0.76 + softHistorySample.rgb * 0.28) * currentSuppression;
    vec3 itemColor = clamp(historyPremultiplied / max(coreGhost * 0.76 + softGhost * 0.28, 0.001), 0.0, 1.0);
    float itemColorMix = clamp(data.color1.a, 0.0, 1.0);
    gradient = mix(gradient, itemColor, itemColorMix);
    vec3 creamTint = vec3(1.0, 0.985, 0.975);
    float creamMix = (0.08 + coreGhost * 0.18 + ageBand * 0.05) * mix(1.0, 0.20, itemColorMix);
    vec3 creamy = mix(gradient, creamTint, creamMix);
    OutColor = vec4(creamy, clamp(alpha, 0.0, 0.72));
}
