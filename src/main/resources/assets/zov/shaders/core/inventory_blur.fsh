#version 150

uniform sampler2D Sampler0;

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

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 boxSize, vec4 cornerRadius) {
    cornerRadius.xy = center.x > 0.0 ? cornerRadius.xy : cornerRadius.zw;
    cornerRadius.x = center.y > 0.0 ? cornerRadius.x : cornerRadius.y;
    vec2 distance = abs(center) - boxSize + cornerRadius.x;
    return min(max(distance.x, distance.y), 0.0) + length(max(distance, 0.0)) - cornerRadius.x;
}

void main() {
    float distance = roundedBoxSDF(gl_FragCoord.xy - location - size / 2.0, size / 2.0, radius);
    float bodyAlpha = 1.0 - smoothstep(-1.0, 1.0, distance);
    float opacity = clamp(color1.a, 0.0, 1.0) * bodyAlpha;
    if (opacity <= 0.0) {
        discard;
    }

    vec3 blurredColor = texture(Sampler0, gl_FragCoord.xy / InputResolution).rgb;
    fragColor = vec4(blurredColor, opacity);
}
