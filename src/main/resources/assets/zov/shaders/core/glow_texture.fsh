#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 sampleColor = texture(Sampler0, texCoord0);
    float intensity = max(max(sampleColor.r, sampleColor.g), sampleColor.b);
    float alpha = intensity * vertexColor.a * ColorModulator.a;
    if (alpha <= 0.001) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb * ColorModulator.rgb, alpha);
}
