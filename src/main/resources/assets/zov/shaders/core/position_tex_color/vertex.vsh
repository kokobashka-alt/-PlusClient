#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

layout(std140) uniform Uniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

out vec2 TexCoord;
out vec4 VertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    TexCoord = UV0;
    VertexColor = Color;
}
