#version 150

layout(std140) uniform Uniforms {
    float Time;
    vec2  Resolution;
    vec2  CameraDir;
    float Fov;
};

out vec4 OutColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}
mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

float orb(vec3 p) {
    float t = Time * 4.0;
    return length(p - vec3(
            sin(sin(t * 0.2) + t * 0.4) * 6.0,
            1.0 + sin(sin(t * 0.5) + t * 0.2) * 4.0,
            12.0 + Time + cos(t * 0.3) * 8.0));
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(Resolution.y, 1.0);
    float tanV = tan(radians(Fov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 dir = rotY(CameraDir.x) * rotX(CameraDir.y) * rayV;

    vec4 o = vec4(0.0);
    float d = 0.0, e = 0.0, s = 0.0;
    float t = Time;
    float t07 = 0.7 * t;
    float t01 = 0.1 * t;
    vec3 timeOffset = vec3(0.0, 0.0, t);

    for (float i = 0.0; i < 48.0; i += 1.0) {
        vec3 p = dir * d + timeOffset;
        e = orb(p) - 0.1;

        float ang = t01 + p.z / 8.0;
        vec4 rot = cos(ang + vec4(0.0, 33.0, 11.0, 0.0));
        p.xy = mat2(rot.x, rot.y, rot.z, rot.w) * p.xy;

        s = 4.0 - abs(p.y);

        for (float a = 0.8; a < 16.0; a += a) {
            p += cos(t07 + p.yzx) * 0.2;
            s -= abs(dot(sin(t01 + p * a), vec3(0.6))) / a;
        }

        e = max(0.5 * e, 0.01);
        s = min(0.03 + 0.2 * abs(s), e);
        d += s;
        o += vec4(1.0 / (s + e * 3.0));
    }

    o = tanh(o / 10.0);
    OutColor = vec4(o.rgb, 1.0);
}