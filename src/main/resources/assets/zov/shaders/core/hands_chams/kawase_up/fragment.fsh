#version 150

uniform sampler2D SourceSampler;

layout(std140) uniform ChamsData {
    vec4 offsetPixel;
    vec4 parameters;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 offset = data.offsetPixel.zw * data.offsetPixel.xy;
    vec4 color = texture(SourceSampler, TexCoord) * 4.0;
    color += texture(SourceSampler, TexCoord + vec2(offset.x * 2.0, 0.0));
    color += texture(SourceSampler, TexCoord - vec2(offset.x * 2.0, 0.0));
    color += texture(SourceSampler, TexCoord + vec2(0.0, offset.y * 2.0));
    color += texture(SourceSampler, TexCoord - vec2(0.0, offset.y * 2.0));
    color += texture(SourceSampler, TexCoord + offset) * 2.0;
    color += texture(SourceSampler, TexCoord - offset) * 2.0;
    color += texture(SourceSampler, TexCoord + vec2(offset.x, -offset.y)) * 2.0;
    color += texture(SourceSampler, TexCoord + vec2(-offset.x, offset.y)) * 2.0;
    OutColor = color / 16.0;
}
