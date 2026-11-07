#version 150

uniform sampler2D SourceSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FogBlurData {
    vec4 sampleData;
    vec4 direction;
    vec4 depthProjection;
    vec4 depthControl;
} data;

in vec2 TexCoord;
out vec4 OutColor;

float viewDistance(float depth) {
    float ndcDepth = depth * 2.0 - 1.0;
    float denominator = data.depthProjection.z * ndcDepth + data.depthProjection.w;
    if (abs(denominator) < 0.000001) {
        return data.depthControl.w;
    }
    return min(abs((data.depthProjection.x * ndcDepth + data.depthProjection.y) / denominator), data.depthControl.w);
}

void addSample(float sampleIndex, float centerDistance, inout vec4 colorSum, inout float weightSum) {
    float offsetPixels = sampleIndex * data.sampleData.z;
    vec2 offset = data.direction.xy * data.sampleData.xy * offsetPixels;
    vec2 uv = clamp(TexCoord + offset, vec2(0.0), vec2(1.0));
    float sampleDistance = viewDistance(texture(DepthSampler, uv).r);
    float tolerance = data.depthControl.x + min(centerDistance, sampleDistance) * data.depthControl.y
            + abs(offsetPixels) * data.depthControl.z;
    float ratio = abs(sampleDistance - centerDistance) / max(tolerance, 0.0001);
    float depthWeight = 1.0 / (1.0 + ratio * ratio);
    float gaussianPosition = sampleIndex / 2.2;
    float spatialWeight = exp(-0.5 * gaussianPosition * gaussianPosition);
    float weight = spatialWeight * depthWeight;
    colorSum += texture(SourceSampler, uv) * weight;
    weightSum += weight;
}

void main() {
    float centerDistance = viewDistance(texture(DepthSampler, TexCoord).r);
    vec4 colorSum = vec4(0.0);
    float weightSum = 0.0;
    for (int index = -5; index <= 5; index++) {
        addSample(float(index), centerDistance, colorSum, weightSum);
    }

    OutColor = colorSum / max(weightSum, 0.0001);
}
