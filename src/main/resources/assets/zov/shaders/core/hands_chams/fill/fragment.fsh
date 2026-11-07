#version 150

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 color1Fill;
    vec4 color2;
    vec4 parameters;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 maskSample = texture(MaskSampler, TexCoord);
    float mask = maskSample.a;
    vec3 itemColor = clamp(maskSample.rgb / max(mask, 0.001), 0.0, 1.0);
    float smoothMask = smoothstep(0.015, 0.96, mask);
    float vertical = smoothstep(0.05, 0.95, TexCoord.y);
    float flowA = 0.5 + 0.5 * sin((TexCoord.x * 1.25 + TexCoord.y * 0.82) * 5.2 + data.parameters.y * 0.42);
    float flowB = 0.5 + 0.5 * sin((TexCoord.x * -0.76 + TexCoord.y * 1.18) * 4.1 - data.parameters.y * 0.31 + 1.7);
    float flow = flowA * 0.64 + flowB * 0.36;
    float sheen = smoothstep(0.48, 0.96, flow);
    vec3 gradient = mix(data.color1Fill.rgb, data.color2.rgb, clamp(vertical * 0.58 + flow * 0.24, 0.0, 1.0));
    float itemColorMix = clamp(data.parameters.z, 0.0, 1.0);
    gradient = mix(gradient, itemColor, itemColorMix);
    float creamMix = (0.045 + sheen * 0.16) * mix(1.0, 0.22, itemColorMix);
    vec3 creamyColor = mix(gradient, vec3(1.0, 0.985, 0.975), creamMix);
    float opacity = clamp(smoothMask * data.color1Fill.a * data.parameters.x * (0.90 + sheen * 0.10), 0.0, 1.0);
    if (opacity <= 0.001) discard;
    OutColor = vec4(creamyColor, opacity);
}
