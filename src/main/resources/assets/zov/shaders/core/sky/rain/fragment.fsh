#version 150

uniform float Time;
uniform vec2  Resolution;
uniform vec2  CameraDir;
uniform float Fov;

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

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(Resolution.y, 1.0);
    float tanV = tan(radians(Fov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 D = rotY(CameraDir.x) * rotX(CameraDir.y) * rayV;

    vec4 o = vec4(0.0);
    float d = 0.0, s;
    vec3 p;
    float tOver40 = Time / 4e1;
    const vec3 K_COEFF = vec3(0.02, 0.06, 0.1);

    for (float i = 0.0; i < 80.0; i += 1.0) {
        p = D * d;
        p.z -= 6e1;
        p = p * 4e4 / dot(p, p);

        for (s = 0.01; s < 1.0; s += s) {
            p += cos(tOver40 + p.yzx / 1e1);
            float K = dot(p, K_COEFF) + Time;
            p -= abs(dot(sin(K + p / (s * 1e1)), vec3(s + s)));
        }

        s = 0.1 + 0.35 * abs(length(p) - 6e1);
        d += s;
        o += vec4(4.0, 2.0, 1.0, 0.0) / s * d
           + 4e3 * (1.0 + cos(i * 0.4 + vec4(2.0, 1.0, 0.0, 0.0))) / s;
    }

    vec3 anchor = normalize(vec3(0.6, 0.2, 1.0));
    float ad = length(D - anchor);
    o /= 4e6 * ad;
    o = mix(o, o.zyxw, smoothstep(0.2, 1.0, ad / 2.5));
    o = o / (o + 0.155) * 1.019;

    OutColor = vec4(o.rgb, 1.0);
}