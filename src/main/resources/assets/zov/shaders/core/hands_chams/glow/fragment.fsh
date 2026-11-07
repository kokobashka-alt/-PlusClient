#version 150

uniform sampler2D BloomSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 color1;
    vec4 color2;
    vec4 parameters;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 bloomSample = texture(BloomSampler, TexCoord);
    float bloom = bloomSample.a;
    vec3 itemColor = clamp(bloomSample.rgb / max(bloom, 0.001), 0.0, 1.0);
    float mask = texture(MaskSampler, TexCoord).a;
    float outer = max(bloom - mask * 0.72, 0.0);
    float signal = clamp(outer * data.parameters.x, 0.0, 1.0);
    float halo = smoothstep(0.002, 0.20, signal);
    float body = smoothstep(0.055, 0.48, signal);
    float core = smoothstep(0.30, 0.88, signal);
    float alpha = halo * 0.14 + body * 0.24 + core * 0.12;
    if (alpha <= 0.001) discard;
    float gradientFactor = clamp(TexCoord.y * 0.72 + bloom * 0.24, 0.0, 1.0);
    vec3 gradient = mix(data.color1.rgb, data.color2.rgb, gradientFactor);
    float itemColorMix = clamp(data.parameters.y, 0.0, 1.0);
    gradient = mix(gradient, itemColor, itemColorMix);
    float creamMix = (0.055 + body * 0.045 + core * 0.15) * mix(1.0, 0.20, itemColorMix);
    vec3 creamyColor = mix(gradient, vec3(1.0, 0.985, 0.975), creamMix);
    OutColor = vec4(creamyColor, alpha);
}
