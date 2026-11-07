#version 150

uniform sampler2D BloomSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 resolutionTime;
    vec4 color1;
    vec4 color2;
    vec4 parameters;
    vec4 cameraMotion;
} data;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 point) {
    point = fract(point * vec2(123.34, 345.45));
    point += dot(point, point + 34.345);
    return fract(point.x * point.y);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 point) {
    float value = 0.0;
    float amplitude = 0.56;
    for (int index = 0; index < 3; index++) {
        value += noise(point) * amplitude;
        point = point * 2.04 + vec2(7.3, 5.1);
        amplitude *= 0.48;
    }
    return value;
}

void main() {
    vec2 uv = TexCoord;
    float time = data.resolutionTime.z;
    float speed = max(data.resolutionTime.w, 0.01);
    float height = data.parameters.x;
    float windStrength = data.parameters.y;
    float intensity = data.parameters.z;
    float aspect = data.resolutionTime.y / data.resolutionTime.x;
    vec2 position = vec2(uv.x * aspect, uv.y);
    float animationTime = time * speed;
    float field = fbm(position * 2.75 + vec2(animationTime * 0.09, -animationTime * 0.19));
    float slowWind = sin(animationTime * 0.64) * 0.34;
    slowWind += sin(animationTime * 0.31 + 1.8) * 0.22;
    slowWind += sin(animationTime * 1.13 + 0.4) * 0.08;
    vec2 direction = vec2(
        slowWind * windStrength + data.cameraMotion.x * 7.0,
        max(0.34, 1.0 + data.cameraMotion.y * 5.0 - data.cameraMotion.z * 0.10)
    );
    direction = normalize(direction);
    vec2 perpendicular = vec2(-direction.y, direction.x);
    float plume = 0.0;
    float softPlume = 0.0;
    float bestDistance = 0.0;
    vec3 plumeColor = mix(data.color1.rgb, data.color2.rgb, 0.5);
    const int steps = 20;
    for (int index = 1; index <= steps; index++) {
        float progress = float(index) / float(steps);
        float waveA = sin(progress * 5.4 - animationTime * 0.78 + field * 2.2) * 0.30;
        float waveB = sin(progress * 8.7 + animationTime * 0.43 + 1.9) * 0.13;
        float lateral = (waveA + waveB + (field - 0.5) * 0.24) * windStrength * height * progress;
        float spread = height * (0.024 + progress * 0.115);
        vec2 sampleCenter = position - direction * progress * height + perpendicular * lateral;
        float branchWaveA = sin(progress * 7.2 + animationTime * 0.37 + 0.8) * height * progress * 0.026;
        float branchWaveB = sin(progress * 6.1 - animationTime * 0.29 + 2.1) * height * progress * 0.022;
        vec2 samplePositionA = sampleCenter + perpendicular * (spread + branchWaveA);
        vec2 samplePositionB = sampleCenter - perpendicular * (spread + branchWaveB);
        vec2 sampleUvCenter = vec2(sampleCenter.x / aspect, sampleCenter.y);
        vec2 sampleUvA = vec2(samplePositionA.x / aspect, samplePositionA.y);
        vec2 sampleUvB = vec2(samplePositionB.x / aspect, samplePositionB.y);
        float insideCenter = step(0.0, sampleUvCenter.x) * step(sampleUvCenter.x, 1.0)
                * step(0.0, sampleUvCenter.y) * step(sampleUvCenter.y, 1.0);
        float insideA = step(0.0, sampleUvA.x) * step(sampleUvA.x, 1.0) * step(0.0, sampleUvA.y) * step(sampleUvA.y, 1.0);
        float insideB = step(0.0, sampleUvB.x) * step(sampleUvB.x, 1.0) * step(0.0, sampleUvB.y) * step(sampleUvB.y, 1.0);
        vec4 centerSample = texture(BloomSampler, sampleUvCenter) * insideCenter;
        vec4 branchSampleA = texture(BloomSampler, sampleUvA);
        vec4 branchSampleB = texture(BloomSampler, sampleUvB);
        branchSampleA *= insideA;
        branchSampleB *= insideB;
        float centerEnvelope = pow(1.0 - progress, 0.76) * (0.88 + field * 0.12);
        float branchEnvelopeA = pow(1.0 - progress, 0.92) * (0.80 + field * 0.20);
        float branchEnvelopeB = pow(1.0 - progress, 1.08) * (0.84 + (1.0 - field) * 0.16);
        float centerCandidate = centerSample.a * centerEnvelope;
        float branchCandidateA = branchSampleA.a * branchEnvelopeA * 0.92;
        float branchCandidateB = branchSampleB.a * branchEnvelopeB * 0.86;
        float candidate = centerCandidate;
        vec4 sourceSample = centerSample;
        if (branchCandidateA > candidate) {
            candidate = branchCandidateA;
            sourceSample = branchSampleA;
        }
        if (branchCandidateB > candidate) {
            candidate = branchCandidateB;
            sourceSample = branchSampleB;
        }
        candidate *= smoothstep(0.010, 0.20, sourceSample.a);
        softPlume += centerCandidate * 0.52 + branchCandidateA * 0.27 + branchCandidateB * 0.21;
        if (candidate > plume) {
            plume = candidate;
            bestDistance = progress;
            plumeColor = clamp(sourceSample.rgb / max(sourceSample.a, 0.001), 0.0, 1.0);
        }
    }

    float self = smoothstep(0.04, 0.78, texture(MaskSampler, uv).a);
    vec4 baseBloomSample = texture(BloomSampler, uv);
    float basePlume = max(baseBloomSample.a - self * 0.68, 0.0) * 0.48;
    if (basePlume > plume) {
        plume = basePlume;
        bestDistance = 0.0;
        plumeColor = clamp(baseBloomSample.rgb / max(baseBloomSample.a, 0.001), 0.0, 1.0);
    }
    softPlume = softPlume / float(steps);
    float fire = max(plume * 1.08, softPlume * 1.45);
    fire = max(fire, basePlume) * (1.0 - self * 0.96);
    if (fire <= 0.001) discard;
    float mist = fbm(position * 3.2 + vec2(-animationTime * 0.07, animationTime * 0.18));
    float rollingWave = 0.5 + 0.5 * sin(position.x * 6.4 + position.y * 2.7 - animationTime * 0.48 + mist * 2.4);
    float tongueWave = 0.5 + 0.5 * sin(position.x * 8.2 - animationTime * 0.41 + mist * 3.0);
    float breathing = 0.89 + sin(animationTime * 0.82 + position.x * 2.8) * 0.07;
    breathing += sin(animationTime * 0.39 + position.y * 4.1 + 1.3) * 0.04;
    float density = mix(0.88, 1.18, mist) * mix(0.94, 1.10, rollingWave);
    density *= mix(0.86, 1.16, smoothstep(0.18, 0.88, tongueWave));
    float signal = clamp(fire * intensity * breathing * density, 0.0, 1.0);
    float outer = smoothstep(0.012, 0.13, signal);
    float body = smoothstep(0.065, 0.37, signal);
    float core = smoothstep(0.30, 0.72, signal);
    if (outer <= 0.002) discard;
    float tip = clamp(bestDistance * 1.18 + (1.0 - body) * 0.10, 0.0, 1.0);
    vec3 gradient = mix(data.color2.rgb, data.color1.rgb, clamp(0.24 + (1.0 - tip) * 0.48 + body * 0.14, 0.0, 1.0));
    float itemColorMix = clamp(data.parameters.w, 0.0, 1.0);
    gradient = mix(gradient, plumeColor, itemColorMix);
    gradient = mix(gradient, data.color2.rgb, mist * 0.08);
    vec3 creamTint = vec3(0.985, 1.0, 0.975);
    float creamMix = (0.045 + body * 0.055 + core * 0.17) * mix(1.0, 0.20, itemColorMix);
    vec3 creamy = mix(gradient, creamTint, creamMix);
    float alpha = outer * 0.12 + body * 0.21 + core * 0.14;
    OutColor = vec4(creamy, alpha);
}
