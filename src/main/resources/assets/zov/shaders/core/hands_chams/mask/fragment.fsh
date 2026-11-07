#version 150

uniform sampler2D AfterSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D DepthBeforeSampler;
uniform sampler2D DepthAfterSampler;

layout(std140) uniform ChamsData {
    vec4 texelSize;
} data;

in vec2 TexCoord;
out vec4 OutColor;

vec3 enhanceItemColor(vec3 color) {
    color = clamp(color, 0.0, 1.0);
    float highest = max(max(color.r, color.g), color.b);
    float lowest = min(min(color.r, color.g), color.b);
    float chroma = highest - lowest;
    if (chroma <= 0.0025) {
        return color;
    }

    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float saturation = chroma / max(highest, 0.001);
    float saturationBoost = mix(3.2, 1.35, smoothstep(0.12, 0.72, saturation));
    vec3 enhanced = clamp(vec3(luminance) + (color - vec3(luminance)) * saturationBoost, 0.0, 1.0);
    float enhancedHighest = max(max(enhanced.r, enhanced.g), enhanced.b);
    float darkLift = 1.0 - smoothstep(0.055, 0.42, luminance);
    float targetValue = max(enhancedHighest, mix(0.18, 0.42, darkLift));
    enhanced *= targetValue / max(enhancedHighest, 0.001);
    return clamp(enhanced, 0.0, 1.0);
}

vec4 sampleItem(vec2 uv) {
    vec3 afterColor = texture(AfterSampler, uv).rgb;
    vec3 beforeColor = texture(BeforeSampler, uv).rgb;
    float beforeDepth = texture(DepthBeforeSampler, uv).r;
    float afterDepth = texture(DepthAfterSampler, uv).r;
    vec3 difference = abs(afterColor - beforeColor);
    float colorDelta = max(max(difference.r, difference.g), difference.b);
    float depthDelta = abs(beforeDepth - afterDepth);
    float colorMask = smoothstep(0.0015, 0.018, colorDelta);
    float depthMask = smoothstep(0.0000005, 0.000008, depthDelta);
    float mask = max(colorMask, depthMask);
    return vec4(afterColor * mask, mask);
}

void main() {
    vec2 pixel = data.texelSize.xy;
    vec4 accumulated = sampleItem(TexCoord) * 0.68;
    accumulated += sampleItem(TexCoord + vec2(pixel.x, 0.0)) * 0.08;
    accumulated += sampleItem(TexCoord - vec2(pixel.x, 0.0)) * 0.08;
    accumulated += sampleItem(TexCoord + vec2(0.0, pixel.y)) * 0.08;
    accumulated += sampleItem(TexCoord - vec2(0.0, pixel.y)) * 0.08;
    float mask = smoothstep(0.035, 0.88, accumulated.a);
    vec3 localColor = enhanceItemColor(accumulated.rgb / max(accumulated.a, 0.001));
    OutColor = vec4(localColor * mask, mask);
}
