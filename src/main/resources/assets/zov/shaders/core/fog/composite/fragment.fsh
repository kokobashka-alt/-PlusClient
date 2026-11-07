#version 150

uniform sampler2D SharpSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FogCompositeData {
    vec4 fogParams;
    vec4 depthProjection;
    vec4 tintColor;
} data;

in vec2 TexCoord;
out vec4 OutColor;

float viewDistance(float depth) {
    float ndcDepth = depth * 2.0 - 1.0;
    float denominator = data.depthProjection.z * ndcDepth + data.depthProjection.w;
    if (abs(denominator) < 0.000001) {
        return data.fogParams.w;
    }
    return min(abs((data.depthProjection.x * ndcDepth + data.depthProjection.y) / denominator), data.fogParams.w);
}

void main() {
    vec4 sharpColor = texture(SharpSampler, TexCoord);
    vec3 blurredColor = texture(BlurSampler, TexCoord).rgb;
    float distanceFromCamera = viewDistance(texture(DepthSampler, TexCoord).r);
    float fogFactor = smoothstep(data.fogParams.x, data.fogParams.y, distanceFromCamera);
    fogFactor = 1.0 - pow(1.0 - fogFactor, 1.35);
    float mixAmount = clamp(fogFactor * data.fogParams.z, 0.0, 1.0);
    vec3 coloredBlur = mix(blurredColor, data.tintColor.rgb, clamp(data.tintColor.a, 0.0, 1.0) * 0.85);
    OutColor = vec4(mix(sharpColor.rgb, coloredBlur, mixAmount), sharpColor.a);
}
