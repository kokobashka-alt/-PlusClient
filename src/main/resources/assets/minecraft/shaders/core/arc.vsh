#version 150

in vec3 Position;

layout(std140) uniform Uniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    vec4 color1;
    vec4 color2;
    vec2 size;
    vec2 location;
    float radius;
    float thickness;
    float start;
    float end;
};

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
