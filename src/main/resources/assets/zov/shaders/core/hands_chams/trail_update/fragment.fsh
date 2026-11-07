#version 150

uniform sampler2D TrailSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 motion;
    vec4 parameters;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    float retention = exp(-data.motion.z * data.motion.w);
    vec2 flicker = vec2(
        sin(data.parameters.z * 1.7 + TexCoord.y * 9.0),
        cos(data.parameters.z * 1.3 + TexCoord.x * 7.0)
    ) * data.parameters.xy * 0.10;
    vec2 historyUv = TexCoord + data.motion.xy + flicker;
    vec4 historyCenter = texture(TrailSampler, historyUv);
    vec4 historyNeighbors = texture(TrailSampler, historyUv + vec2(data.parameters.x, 0.0));
    historyNeighbors += texture(TrailSampler, historyUv - vec2(data.parameters.x, 0.0));
    historyNeighbors += texture(TrailSampler, historyUv + vec2(0.0, data.parameters.y));
    historyNeighbors += texture(TrailSampler, historyUv - vec2(0.0, data.parameters.y));
    vec4 history = mix(historyCenter, historyNeighbors * 0.25, 0.18) * retention;
    vec4 maskSample = texture(MaskSampler, TexCoord);
    float currentAlpha = smoothstep(0.025, 0.82, maskSample.a) * 0.72;
    vec3 currentColor = clamp(maskSample.rgb / max(maskSample.a, 0.001), 0.0, 1.0);
    vec4 current = vec4(currentColor * currentAlpha, currentAlpha);
    OutColor = current.a >= history.a ? current : history;
}
