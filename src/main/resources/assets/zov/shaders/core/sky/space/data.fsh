#version 150

layout(std140) uniform Uniforms {
    float Time;
    vec2  Resolution;
    vec2  CameraDir;
    float Fov;
    vec3  Color1;
    vec3  Color2;
    float Speed;
    float Scale;
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

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z + p.y * p.x);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);

    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);

    return mix(nxy0, nxy1, f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += amp * valueNoise(p);
        p *= 2.02;
        amp *= 0.5;
    }
    return sum;
}

float starLayer(vec3 dir, float density, float t) {
    vec3 p = dir * density;
    vec3 cell = floor(p);
    vec3 f = fract(p) - 0.5;

    float best = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 off = vec3(float(x), float(y), float(z));
                vec3 id = cell + off;
                float h = hash33(id);
                if (h < 0.55) continue;
                vec3 sp = off + (vec3(hash13(id + 1.7), hash13(id + 4.3), hash13(id + 9.1)) - 0.5) * 0.8;
                float d = length(f - sp);
                float bright = smoothstep(0.08, 0.0, d) * h;
                best = max(best, bright);
            }
        }
    }
    return best;
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(Resolution.y, 1.0);
    float tanV = tan(radians(Fov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 dir = rotY(CameraDir.x) * rotX(CameraDir.y) * rayV;

    float t = Time * Speed;

    vec3 c1 = Color1 / max(max(Color1.r, Color1.g), max(Color1.b, 0.001));
    vec3 c2 = Color2 / max(max(Color2.r, Color2.g), max(Color2.b, 0.001));

    float zen = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = mix(vec3(0.006, 0.007, 0.018), vec3(0.012, 0.010, 0.030), zen);

    vec3 np = dir * (2.2 * Scale) + vec3(0.0, 0.0, t * 0.03);
    float n1 = fbm(np);
    float n2 = fbm(np * 1.9 + 11.0 + vec3(t * 0.02, 0.0, 0.0));
    float nebula = pow(clamp(n1 * 1.3 - 0.25, 0.0, 1.0), 1.8);
    float nebula2 = pow(clamp(n2 * 1.3 - 0.3, 0.0, 1.0), 2.2);
    vec3 nebColor = mix(c1, c2, clamp(n1 * 0.7 + 0.15, 0.0, 1.0));
    vec3 sky = base + nebColor * nebula * 0.45 + c2 * nebula2 * 0.22;

    float stars  = starLayer(dir, 22.0, t);
    stars += starLayer(dir, 42.0, t * 1.3) * 0.7;
    stars += starLayer(dir, 80.0, t * 0.8) * 0.45;
    vec3 starTint = mix(vec3(1.0), mix(c1, c2, 0.5), 0.35);
    sky += starTint * stars;

    sky *= clamp(dir.y * 4.0 + 0.85, 0.3, 1.0);

    sky = sqrt(max(sky, vec3(0.0)));
    sky = smoothstep(vec3(0.0), vec3(1.05), sky);

    OutColor = vec4(sky, 1.0);
}
