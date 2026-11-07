#version 150

uniform sampler2D FireSampler;

layout(std140) uniform ChamsData {
    vec4 parameters;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 pixel = data.parameters.xy;
    vec4 center = texture(FireSampler, TexCoord);
    vec4 right = texture(FireSampler, TexCoord + vec2(pixel.x, 0.0));
    vec4 left = texture(FireSampler, TexCoord - vec2(pixel.x, 0.0));
    vec4 up = texture(FireSampler, TexCoord + vec2(0.0, pixel.y));
    vec4 down = texture(FireSampler, TexCoord - vec2(0.0, pixel.y));
    float alpha = center.a * 0.72 + (right.a + left.a + up.a + down.a) * 0.07;
    if (alpha <= 0.001) discard;
    vec3 weighted = center.rgb * center.a * 0.72;
    weighted += right.rgb * right.a * 0.07;
    weighted += left.rgb * left.a * 0.07;
    weighted += up.rgb * up.a * 0.07;
    weighted += down.rgb * down.a * 0.07;
    vec3 color = weighted / max(alpha, 0.001);
    color = mix(color, vec3(1.0, 0.985, 0.975), smoothstep(0.52, 0.96, center.a) * 0.07);
    OutColor = vec4(color, clamp(alpha * data.parameters.z * 0.82, 0.0, 1.0));
}
