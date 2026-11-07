#version 150

uniform sampler2D AfterSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D BlurredSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform ChamsData {
    vec4 parameters;
    vec4 resolution;
} data;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 uv = TexCoord;
    float mosaic = data.parameters.w;
    
    if (mosaic > 1.0) {
        vec2 res = data.resolution.xy;
        uv = (floor(uv * res / mosaic) + 0.5) * mosaic / res;
    }

    vec4 afterColor = texture(AfterSampler, TexCoord);
    vec3 trueBackground = texture(BeforeSampler, TexCoord).rgb;
    vec3 beforeColor = texture(BeforeSampler, uv).rgb;
    vec4 blurredSample = texture(BlurredSampler, uv);
    
    float realMask = smoothstep(0.015, 0.96, texture(MaskSampler, TexCoord).a);
    float jaggedMask = smoothstep(0.015, 0.96, texture(MaskSampler, uv).a);
    
    float smoothEdges = data.resolution.z;
    float mask = mix(jaggedMask, realMask, smoothEdges);
    
    float itemOpacity = clamp(data.parameters.y, 0.0, 1.0);
    float textureVisibility = clamp(data.parameters.z, 0.0, 1.0);
    
    vec3 blurredItem = clamp(blurredSample.rgb / max(blurredSample.a, 0.001), 0.0, 1.0);
    vec3 textureBlur = mix(beforeColor, blurredItem, itemOpacity);
    vec3 pixelatedAfter = texture(AfterSampler, uv).rgb;
    vec3 backgroundBlur = mix(blurredSample.rgb, pixelatedAfter, textureVisibility * itemOpacity);
    
    vec3 material = mix(textureBlur, backgroundBlur, step(0.5, data.parameters.x));
    
    float eraseMask = smoothstep(0.0, 0.1, texture(MaskSampler, TexCoord).a);
    vec3 baseColor = mix(afterColor.rgb, trueBackground, eraseMask);
    
    OutColor = vec4(mix(baseColor, material, mask), afterColor.a);
}
