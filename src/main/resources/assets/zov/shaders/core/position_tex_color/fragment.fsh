#version 150

in vec2 TexCoord;
in vec4 VertexColor;

uniform sampler2D Sampler0;

layout(std140) uniform Uniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

out vec4 OutColor;

void main() {
    vec4 texColor = texture(Sampler0, TexCoord);
    OutColor = texColor * VertexColor;
}
