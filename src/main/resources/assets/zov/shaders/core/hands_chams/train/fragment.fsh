#version 150

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 resolutionTime;
    vec4 color1;
    vec4 color2;
    vec4 parameters;
} data;

in vec2 TexCoord;
out vec4 OutColor;

float band(float distanceToCenter, float radius, float feather) {
    return 1.0 - smoothstep(radius, radius + feather, distanceToCenter);
}

float circularDistance(float first, float second) {
    return abs(fract(first - second + 0.5) - 0.5);
}

void main() {
    vec2 pixel = data.resolutionTime.xy * max(data.color2.a, 0.75);
    vec4 centerSample = texture(MaskSampler, TexCoord);
    vec4 rightSample = texture(MaskSampler, TexCoord + vec2(pixel.x, 0.0));
    vec4 leftSample = texture(MaskSampler, TexCoord - vec2(pixel.x, 0.0));
    vec4 downSample = texture(MaskSampler, TexCoord + vec2(0.0, pixel.y));
    vec4 upSample = texture(MaskSampler, TexCoord - vec2(0.0, pixel.y));
    vec4 downRightSample = texture(MaskSampler, TexCoord + pixel);
    vec4 upLeftSample = texture(MaskSampler, TexCoord - pixel);
    vec4 upRightSample = texture(MaskSampler, TexCoord + vec2(pixel.x, -pixel.y));
    vec4 downLeftSample = texture(MaskSampler, TexCoord + vec2(-pixel.x, pixel.y));

    float maximumMask = max(centerSample.a, max(max(rightSample.a, leftSample.a), max(downSample.a, upSample.a)));
    maximumMask = max(maximumMask, max(max(downRightSample.a, upLeftSample.a), max(upRightSample.a, downLeftSample.a)));
    float minimumMask = min(centerSample.a, min(min(rightSample.a, leftSample.a), min(downSample.a, upSample.a)));
    minimumMask = min(minimumMask, min(min(downRightSample.a, upLeftSample.a), min(upRightSample.a, downLeftSample.a)));
    float contour = smoothstep(0.025, 0.72, maximumMask - minimumMask);
    if (contour <= 0.001) discard;

    vec4 colorAccumulator = centerSample * 2.0 + rightSample + leftSample + downSample + upSample;
    colorAccumulator += (downRightSample + upLeftSample + upRightSample + downLeftSample) * 0.72;
    vec3 itemColor = clamp(colorAccumulator.rgb / max(colorAccumulator.a, 0.001), 0.0, 1.0);
    float aspect = data.resolutionTime.y / data.resolutionTime.x;
    float time = data.resolutionTime.z;
    float speed = max(data.resolutionTime.w, 0.01);
    float softness = clamp(data.parameters.w, 0.0, 1.0);
    float widthFactor = clamp((data.parameters.z - 0.04) / 0.31, 0.0, 1.0);
    float width = mix(0.012, 0.082, widthFactor);
    float feather = width * mix(0.20, 0.92, softness);
    vec2 position = vec2((TexCoord.x - 0.5) * aspect, TexCoord.y - 0.58);
    float angle = atan(position.y, position.x) / 6.28318530718 + 0.5;
    float radius = length(position);
    float contourPhase = fract(angle + radius * 0.075 + sin(angle * 18.8495559215 + radius * 7.0) * 0.008
            - time * speed * 0.135);

    float headBand = band(circularDistance(contourPhase, 0.0), width * 0.58, feather * 0.66);
    float carriage1 = band(circularDistance(contourPhase, 1.0 - width * 2.15), width * 0.46, feather);
    float carriage2 = band(circularDistance(contourPhase, 1.0 - width * 3.75), width * 0.39, feather * 1.08);
    float carriage3 = band(circularDistance(contourPhase, 1.0 - width * 5.18), width * 0.33, feather * 1.16);
    float carriage4 = band(circularDistance(contourPhase, 1.0 - width * 6.48), width * 0.27, feather * 1.24);
    float convoy = headBand + carriage1 * 0.74 + carriage2 * 0.56 + carriage3 * 0.40 + carriage4 * 0.27;
    float haze = band(circularDistance(contourPhase, 1.0 - width * 2.75), width * 3.65, feather * 2.8) * 0.22;
    float signal = clamp(convoy + haze, 0.0, 1.0);
    float core = clamp(headBand * 0.88 + carriage1 * 0.42 + carriage2 * 0.24, 0.0, 1.0);
    float pulse = 0.95 + sin(time * speed * 2.15) * 0.05;
    float strength = max(data.parameters.x, 0.0);
    float alpha = clamp(contour * signal * strength * data.parameters.y * pulse, 0.0, 0.88);
    if (alpha <= 0.001) discard;

    float gradientFactor = clamp(contourPhase * 0.36 + TexCoord.y * 0.34 + signal * 0.30, 0.0, 1.0);
    vec3 gradient = mix(data.color1.rgb, data.color2.rgb, gradientFactor);
    float itemColorMix = clamp(data.color1.a, 0.0, 1.0);
    gradient = mix(gradient, itemColor, itemColorMix);
    vec3 cream = vec3(1.0, 0.985, 0.965);
    vec3 creamyColor = mix(gradient, cream, (0.10 + core * 0.54) * mix(1.0, 0.18, itemColorMix));
    OutColor = vec4(creamyColor, alpha);
}
