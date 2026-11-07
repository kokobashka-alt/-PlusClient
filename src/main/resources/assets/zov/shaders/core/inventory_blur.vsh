#version 150

in vec3 Position;

layout(std140) uniform Uniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    vec2 InputResolution;
    float Quality;
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
    float time;
    int waveEnabled;
    float masterAlpha;
    int flowEnabled;
    float flowStrength;
    vec3 flowColor1;
    vec3 flowColor2;
    vec2 parentLocation;
    vec2 parentSize;
};

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
