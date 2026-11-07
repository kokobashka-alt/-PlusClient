#version 150

in vec3 Position;

layout(std140) uniform Uniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    vec4 color1;
    vec4 color2;
    vec4 color3;
    vec4 color4;
    vec4 outlineColor;
    vec2 size;
    vec2 location;
    vec4 radius;
    float thickness;
    float softness;
};

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
