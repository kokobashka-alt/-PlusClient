#version 150

layout(std140) uniform Uniforms {
    float Time;
    vec2 Resolution;
    vec2 CameraDir;
    float Fov;
};

out vec4 OutColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0, c, s,
                0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s,
                0.0, 1.0, 0.0,
                -s, 0.0, c);
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(Resolution.y, 1.0);
    float tanV = tan(radians(Fov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 dir = rotY(CameraDir.x) * rotX(CameraDir.y) * rayV;

    vec4 color = vec4(0.0);
    float distanceValue = 0.0;
    float stepValue;
    vec3 point;
    float timePart = Time / 40.0;
    const vec3 coefficients = vec3(0.02, 0.06, 0.1);

    for (float i = 0.0; i < 80.0; i += 1.0) {
        point = dir * distanceValue;
        point.z -= 60.0;
        point = point * 40000.0 / max(dot(point, point), 0.0001);

        for (stepValue = 0.01; stepValue < 1.0; stepValue += stepValue) {
            point += cos(timePart + point.yzx / 10.0);
            float phase = dot(point, coefficients) + Time;
            point -= abs(dot(sin(phase + point / (stepValue * 10.0)), vec3(stepValue + stepValue)));
        }

        stepValue = 0.1 + 0.35 * abs(length(point) - 60.0);
        distanceValue += stepValue;
        color += vec4(4.0, 2.0, 1.0, 0.0) / stepValue * distanceValue
               + 4000.0 * (1.0 + cos(i * 0.4 + vec4(2.0, 1.0, 0.0, 0.0))) / stepValue;
    }

    vec3 anchor = normalize(vec3(0.6, 0.2, 1.0));
    float anchorDistance = max(length(dir - anchor), 0.0001);
    color /= 4000000.0 * anchorDistance;
    color = mix(color, color.zyxw, smoothstep(0.2, 1.0, anchorDistance / 2.5));
    color = color / (color + 0.155) * 1.019;

    OutColor = vec4(color.rgb, 1.0);
}
