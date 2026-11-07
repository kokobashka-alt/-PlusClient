#version 150

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

out vec4 fragColor;

const float PI = 3.141592653589793;
const float RAD = 0.017453292519943295;

void main() {
    float startAngle = start * RAD;
    float endAngle = startAngle + min(end * RAD, PI * 2.0);
    float smoothThreshold = 6.0 / length(size);
    vec2 center = ((gl_FragCoord.xy - location) / size) * 2.0 - 1.0;
    float distance = length(center);
    float bandAlpha = smoothstep(radius, radius + smoothThreshold, distance)
        * smoothstep(radius + thickness, radius + thickness - smoothThreshold, distance);
    float angle = atan(center.y, center.x) + PI;
    float angleAlpha = smoothstep(angle, angle - smoothThreshold, startAngle - 0.1)
        * smoothstep(angle, angle + smoothThreshold, endAngle + 0.1);
    float degrees = mod(angle / PI * 180.0, 360.0);
    degrees = degrees >= 180.0 ? (360.0 - degrees) * 2.0 : degrees * 2.0;
    fragColor = mix(color1, color2, degrees / 360.0) * bandAlpha * angleAlpha;
}
