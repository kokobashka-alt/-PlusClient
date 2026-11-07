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

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    radius.x  = (center.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(center) - size + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){
    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

// --- Value-noise based fractal flow used for the liquid HUD effect ---
float flowHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float flowNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    // Quintic interpolation (smoother 2nd derivative) -> no grid artifacts,
    // gives the fluid a softer, more uniform consistency.
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(mix(flowHash(i + vec2(0.0, 0.0)), flowHash(i + vec2(1.0, 0.0)), u.x),
               mix(flowHash(i + vec2(0.0, 1.0)), flowHash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float flowFbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    float total = 0.0;
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    for (int i = 0; i < 3; i++) {
        v += a * flowNoise(p);
        total += a;
        p = m * p;
        a *= 0.5;
    }
    return v / total;
}

vec4 blur(vec2 uvOffset) {
    vec4 rectColor = createGradient((gl_FragCoord.xy - location) / size, color1, color2, color3, color4);
    vec2 Radius = Quality / InputResolution.xy;
    vec2 uv = gl_FragCoord.xy / InputResolution.xy + uvOffset;

    vec4 blurColor = texture(Sampler0, uv);
    blurColor.a = 1.0;

    float tintAlpha = 1.0 - pow(1.0 - clamp(rectColor.a, 0.0, 1.0), 1.16);
    vec4 tintColor = vec4(rectColor.rgb, tintAlpha);
    return vec4((blurColor * (1.0 - tintColor.a)).rgb, tintColor.a) + tintColor;
}
void main() {
    float distance = roundedBoxSDF(gl_FragCoord.xy - location - (size / 2.0), size / 2.0, radius);
    
    // Wave intensity
    float waveIntensity = 0.0;
    if (waveEnabled == 1) {
        float maxDist = InputResolution.x + InputResolution.y;
        float pixelX = gl_FragCoord.x;
        float pixelY = gl_FragCoord.y;
        
        // Normalized global distances (0.0 to 1.0)
        float dist1 = (pixelX + pixelY) / maxDist; 
        float dist2 = ((InputResolution.x - pixelX) + pixelY) / maxDist;
        
        // cycleTime is 8 seconds.
        float localTime = mod(time, 8.0);
        float progress;
        float waveCenter;
        float distToWave;
        
        if (localTime < 1.5) { 
            // Wave 1: 1.5 seconds to cross the screen (slow and beautiful)
            progress = localTime / 1.5; 
            waveCenter = -0.2 + progress * 1.4; // crosses from -0.2 to 1.2
            distToWave = abs(dist1 - waveCenter);
            waveIntensity = exp(-distToWave * distToWave * 120.0);
        } else if (localTime > 4.0 && localTime < 5.5) {
            // Wave 2: reverse wave 4 seconds later
            progress = (localTime - 4.0) / 1.5; 
            waveCenter = -0.2 + progress * 1.4; 
            distToWave = abs(dist2 - waveCenter);
            waveIntensity = exp(-distToWave * distToWave * 120.0);
        }
    }
    
    // --- Liquid flow: several large blobs that drift and merge. Instead of
    // painting a separate colour, they modulate the BLUR itself - lifting and
    // thinning it and refracting (pushing) it around, so the motion lives inside.
    vec2 flowOffset = vec2(0.0);
    float flowLift = 0.0;       // brightens the blur within the liquid
    float flowFade = 0.0;       // makes the blur slightly more transparent
    vec3  flowHue = vec3(0.0);  // faint theme hue mixed into the blur
    if (flowEnabled == 1 && flowStrength > 0.001) {
        vec2 luv = (gl_FragCoord.xy - location) / size;
        float aspect = size.x / max(size.y, 1.0);
        vec2 p = vec2(luv.x * aspect, luv.y);

        float gOff = (parentLocation.x + parentLocation.y) / max(InputResolution.x + InputResolution.y, 1.0);
        float seed = gOff * 47.0;
        float t = time * 0.8 + gOff * 10.0;

        // Two-octave warp of the sampling space -> richer, curvy organic edges
        // (still rounded overall, no stretched tails).
        vec2 wv  = vec2(flowFbm(p * 1.3 + vec2(t * 0.20, 0.0) + seed),
                        flowFbm(p * 1.3 + vec2(0.0, -t * 0.18) + seed + 5.0)) - 0.5;
        vec2 wv2 = vec2(flowFbm(p * 2.7 + vec2(-t * 0.15, t * 0.10) + seed * 1.7),
                        flowFbm(p * 2.7 + vec2(t * 0.12, 0.0) + seed + 9.0)) - 0.5;
        vec2 pw = p + wv * 0.40 + wv2 * 0.16;

        // A few large blobs, each with its own size, drift direction and a mild,
        // slowly varying ellipse -> varied, pleasing shapes that merge and split.
        float field = 0.0;
        for (int i = 0; i < 4; i++) {
            float fi = float(i);
            float sp = 0.24 + fi * 0.06;
            float dx = (mod(fi, 2.0) < 0.5) ? 1.0 : -1.0;
            float dy = (mod(fi, 3.0) < 1.5) ? -1.0 : 1.0;
            vec2 c = vec2(
                0.5 * aspect + cos(t * sp * dx + fi * 2.0 + seed)        * (0.28 * aspect),
                0.5          + sin(t * sp * 0.9 * dy + fi * 3.3 + seed * 1.4) * 0.28
            );
            float rad = 0.34 + fi * 0.04 + 0.06 * sin(t * 0.4 + fi * 1.7);
            vec2 d = pw - c;
            // Gentle ellipse only (no rotation) so shapes vary without tails.
            float st = 1.12 + 0.12 * sin(t * 0.25 + fi * 2.0 + seed);
            d.x *= st;
            d.y /= st;
            field += rad * rad / (dot(d, d) + 0.006);
        }

        // Soft mask -> big blobs merge with feathered edges.
        float mask = smoothstep(0.55, 1.30, field);
        float amount = mask * flowStrength;

        // Gentle refraction of the blur from the smooth warp vector (pushes /
        // spreads the blurred backdrop around like a liquid lens).
        flowOffset = wv * (22.0 * amount) / InputResolution.xy;

        // Faint theme hue (kept toward white) used only to lightly bias the blur,
        // not as an opaque overlay.
        float hue = clamp(0.5 + 0.5 * sin(t * 0.35 + field), 0.0, 1.0);
        flowHue = mix(mix(flowColor1, flowColor2, hue), vec3(1.0), 0.5);

        // Curved highlight on the blob rim + soft inner glow, expressed as how
        // much we lift/thin the blur rather than a separate colour.
        float level = 0.95 + 0.25 * sin(t * 0.45 + seed);
        float band = exp(-pow((field - level) / 0.20, 2.0));
        float glow = smoothstep(0.75, 1.7, field);

        flowLift = amount * 0.20 + band * amount * 0.55 + glow * amount * 0.12;
        flowFade = amount * 0.22;
    }

    // Tinted blur background (UV distorted by the liquid flow field)
    vec4 bColor = blur(flowOffset);
    
    // 1. Body (distance <= 0)
    float bodyAlpha = 1.0 - smoothstep(-1.0, 1.0, distance);

    // Modulate the BLUR itself with the liquid: lift its brightness, mix a faint
    // theme hue and make it a touch more transparent inside the blobs.
    vec3 baseRgb = bColor.rgb * (1.0 + flowLift);
    baseRgb = mix(baseRgb, baseRgb + flowHue * 0.12, flowFade);
    vec4 bodyColor = vec4(baseRgb, bColor.a * bodyAlpha * (1.0 - flowFade * 0.5));
    
    if (waveEnabled == 1 && waveIntensity > 0.0) {
        bodyColor.rgb += waveIntensity * 0.25 * masterAlpha;
        bodyColor.a = min(1.0, bodyColor.a + waveIntensity * 0.15 * bodyAlpha * masterAlpha);
    }
    
    // 2. Border (strictly outside, distance >= 0)
    float borderAlpha = 0.0;
    vec4 bdrColor = vec4(0.0);
    
    if (thickness > 0.0) {
        // Outer glow: nonlinear falloff makes it sleek, less fat, and perfectly beautiful
        float normDist = clamp(distance / max(softness, 1.0), 0.0, 1.0);
        borderAlpha = pow(1.0 - normDist, 2.0);
        
        // MASK: completely remove border from inside the module to prevent overlapping segments
        float outerMask = smoothstep(-1.0, 1.0, distance); 
        borderAlpha *= outerMask;
        
        bdrColor = vec4(outlineColor.rgb, borderAlpha * outlineColor.a);
        
        if (waveEnabled == 1 && waveIntensity > 0.0) {
            bdrColor.rgb += waveIntensity * 0.8 * masterAlpha;
            bdrColor.a = min(1.0, bdrColor.a + waveIntensity * 0.5 * borderAlpha * masterAlpha);
        }
    }
    
    // 3. Composite (Body over Border)
    float outA = bodyColor.a + bdrColor.a * (1.0 - bodyColor.a);
    vec3 outRGB = vec3(0.0);
    if (outA > 0.0001) {
        outRGB = (bodyColor.rgb * bodyColor.a + bdrColor.rgb * bdrColor.a * (1.0 - bodyColor.a)) / outA;
    }
    
    fragColor = vec4(outRGB, outA);
}
