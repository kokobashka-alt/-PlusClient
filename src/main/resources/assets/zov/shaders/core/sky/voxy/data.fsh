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

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 x) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(p + vec2(0, 0));
    float b = hash(p + vec2(1, 0));
    float c = hash(p + vec2(0, 1));
    float d = hash(p + vec2(1, 1));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p); p *= 2.02;
    f += 0.2500 * noise(p); p *= 2.03;
    f += 0.1250 * noise(p); p *= 2.01;
    f += 0.0625 * noise(p);
    return f / 0.9375;
}

float getAuroraSample(vec2 coord, float t) {
    vec2 distortedCoord = coord;

    float baseAngle = (t * 0.0004) * 0.3;
    float cA = cos(baseAngle), sA = sin(baseAngle);
    mat2 baseRot = mat2(cA, -sA, sA, cA);

    distortedCoord = baseRot * distortedCoord;

    vec2 flowUV = distortedCoord * 0.35;
    flowUV += vec2(sin(t * 0.0012), cos(t * 0.0010));

    float f = fbm(flowUV * 8.0) * 2.0 - 1.0;

    vec2 curlDir = normalize(vec2(
        cos(f * 6.283 + t * 0.2),
        sin(f * 6.283 + t * 0.1)
    ));

    vec2 warping = curlDir * f;
    distortedCoord += warping * 0.125;

    distortedCoord.y *= 0.75;
    distortedCoord.x *= 1.5;

    float sheet = fbm(vec2(distortedCoord.x * 1.25, distortedCoord.y * 0.5 + t * 0.0025) * 4.0);
    sheet *= sheet * sheet;

    float rays = fbm(vec2(distortedCoord.x * 5.0, distortedCoord.y * 2.0) * 1.5 + vec2(-t * 0.0015, t * 0.0025) * 8.0);
    float aurora = sheet * (0.8 + pow(rays, 8.0) * 4.0);
    return max(aurora, 0.0);
}

vec4 auroraVolumetric(vec3 rd, vec3 tintLow, vec3 tintHigh, float t) {
    vec4 col = vec4(0.0);
    float rdY = max(rd.y, 0.035);
    float invRdY = 1.0 / (rdY * 2.0 + 0.4);

    const int STEPS = 8;
    const float STEP_F = 5.0;

    for (int j = 0; j < STEPS; j++) {
        float i = float(j);
        float fi = i * STEP_F;
        float pt = (0.8 + fi * fi * 0.00042) * invRdY;
        vec3 bpos = 5.5 + pt * rd;
        
        float rzt = getAuroraSample(bpos.zx * 0.08, t * 80.0);
        float wave = 0.92 + 0.16 * (sin(fi * 0.043 + 1.0) * 0.5 + 0.5);
        vec3 tint = mix(tintLow, tintHigh, clamp(fi / (STEP_F * float(STEPS)), 0.0, 1.0));
        
        vec4 col2 = vec4(tint * wave, rzt);
        col2.rgb *= rzt;
        
        float layerFade = 1.0 - clamp(fi / 50.0, 0.0, 1.0);
        col += col2 * (layerFade * layerFade * layerFade * 0.28) * smoothstep(0.0, 5.0, fi);
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
    vec3 base = mix(vec3(0.0, 0.01, 0.04) + theme * 0.015,
                    vec3(0.0, 0.0, 0.02) + theme * 0.01,
                    horizonMix);

    vec3 dirS = vec3(dir.x * Scale, dir.y, dir.z * Scale);
    vec3 sky = base + auroraVolumetric(dirS, themeA, themeB, t).rgb * 0.85;
    
    if (dir.y > 0.0) {
        vec2 starUv = dir.xz / (dir.y + 0.08) * 145.0;
        vec2 starCell = floor(starUv);
        vec2 starPos = fract(starUv) - 0.5;
        float starSeed = hash(starCell);
        float starShape = smoothstep(0.035, 0.0, length(starPos));
        float starGate = smoothstep(0.985, 1.0, starSeed);
        sky += vec3(1.0, 0.95, 0.9) * starShape * starGate * dir.y * 1.8;
    }

    sky = sqrt(max(sky, vec3(0.0)));
    sky = smoothstep(vec3(0.0), vec3(1.0), sky);

    OutColor = vec4(sky, 1.0);
}
