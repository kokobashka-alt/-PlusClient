#version 150

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

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 boxSize, vec4 cornerRadius) {
    cornerRadius.xy = center.x > 0.0 ? cornerRadius.xy : cornerRadius.zw;
    cornerRadius.x = center.y > 0.0 ? cornerRadius.x : cornerRadius.y;
    vec2 distance = abs(center) - boxSize + cornerRadius.x;
    return min(max(distance.x, distance.y), 0.0)
        + length(max(distance, 0.0)) - cornerRadius.x;
}

vec4 createGradient(vec2 coords) {
    vec4 color = mix(mix(color1, color2, coords.y),
                     mix(color3, color4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843,
                 fract(sin(dot(coords, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

void main() {
    float distance = roundedBoxSDF(
        gl_FragCoord.xy - location - size * 0.5,
        size * 0.5,
        radius
    );
    float edge = thickness > 0.0 ? 1.0 : softness + 1.0;
    float bodyAlpha = 1.0 - smoothstep(-1.0, edge, distance);

    if (bodyAlpha < 0.49 && thickness > 0.0) {
        float outlineAlpha = 1.0 - smoothstep(-softness, softness, distance);
        fragColor = vec4(outlineColor.rgb, outlineColor.a * outlineAlpha);
    } else {
        float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(distance));
        vec4 gradient = createGradient((gl_FragCoord.xy - location) / size);
        vec4 body = vec4(gradient.rgb, gradient.a * bodyAlpha);
        fragColor = mix(vec4(gradient.rgb, 0.0),
                        mix(body, thickness > 0.0 ? outlineColor : body, borderAlpha),
                        bodyAlpha);
    }
}
