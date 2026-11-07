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

mat2 mm2(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, s, -s, c);
}

float tri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

vec2 tri2(vec2 p) {
    return vec2(tri(p.x) + tri(p.y), tri(p.y + tri(p.x)));
}

float fbmAurora(vec2 p, float t) {
    float z = 1.8;
    float z2 = 2.5;
    float rz = 0.0;
    p = mm2(p.x * 0.06) * p;
    vec2 bp = p;
    float pulse = 0.88 + 0.12 * sin(t * 0.05) * cos(t * 0.01);
    float spdT = t * 0.06;

    for (int j = 0; j < 3; j++) {
        vec2 dg = tri2(bp * 1.85) * 0.75;
        dg = mm2(spdT) * dg;
        p -= dg / z2;

        bp *= 1.3;
        z2 *= 0.45;
        z *= 0.42;
        p *= 1.21 + (rz - 1.0) * 0.02;

        rz += tri(p.x + tri(p.y)) * z;
        p *= pulse;
    }
    float v = max(rz * 16.0, 0.001);
    return clamp(1.0 / (v * sqrt(v)), 0.0, 1.0);
}

vec4 aurora(vec3 rd, vec3 tintLow, vec3 tintHigh, float t) {
    vec4 col = vec4(0.0);
    vec4 avgCol = vec4(0.0);
    float rdY = max(rd.y, 0.035);
    float invRdY = 1.0 / (rdY * 2.0 + 0.4);

    const int STEPS = 10;
    const float STEP_F = 5.0;

    for (int j = 0; j < STEPS; j++) {
        float i = float(j);
        float fi = i * STEP_F;
        float pt = (0.8 + fi * fi * 0.00042) * invRdY;
        vec3 bpos = 5.5 + pt * rd;
        float rzt = fbmAurora(bpos.zx, t);
        float wave = 0.92 + 0.16 * (sin(fi * 0.043 + 1.0) * 0.5 + 0.5);
        vec3 tint = mix(tintLow, tintHigh, clamp(fi / (STEP_F * float(STEPS)), 0.0, 1.0));
        vec4 col2 = vec4(tint * wave, rzt);
        col2.rgb *= rzt;
        avgCol = mix(avgCol, col2, 0.5);
        float layerFade = 1.0 - clamp(fi / 50.0, 0.0, 1.0);
        col += avgCol * (layerFade * layerFade * layerFade * 0.28) * smoothstep(0.0, 5.0, fi);
    }
    col *= clamp(rd.y * 15.0 + 0.4, 0.0, 1.0);

    return smoothstep(0.0, 1.1, col * 2.15);
}

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = Resolution.x / max(Resolution.y, 1.0);
    float tanV = tan(radians(Fov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 dir = rotY(CameraDir.x) * rotX(CameraDir.y) * rayV;

    float t = Time * Speed + 23.0;

    vec3 c1 = Color1 * Color1;
    vec3 c2 = Color2 * Color2;

    vec3 themeA = c1 / max(max(c1.r, c1.g), max(c1.b, 0.001));
    vec3 themeB = c2 / max(max(c2.r, c2.g), max(c2.b, 0.001));
    vec3 theme  = mix(themeA, themeB, 0.5);

    float horizonMix = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 base = mix(vec3(0.007, 0.008, 0.020) + theme * 0.020,
                    vec3(0.004, 0.005, 0.014) + theme * 0.012,
                    horizonMix);

    vec3 dirS = vec3(dir.x * Scale, dir.y, dir.z * Scale);
    vec3 sky = base + aurora(dirS, themeA, themeB, t).rgb * 0.78;
    sky = sqrt(max(sky, vec3(0.0)));
    sky = smoothstep(vec3(0.0), vec3(1.0), sky);

    OutColor = vec4(sky, 1.0);
}
