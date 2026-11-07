package ru.zov.common.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gl.ScissorState;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.vertex.VertexFormat;
import ru.zov.mixins.render.RenderLayerAccessor;
import ru.zov.mixins.render.RenderSetupAccessor;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RenderCompat {
    public static float activeAlpha = 1.0f;
    public static org.joml.Matrix4f modelViewOverride;

    public static class PipelineSettings {
        public BlendFunction blend = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        public boolean cull = false;
        public DepthTestFunction depthTest = DepthTestFunction.NO_DEPTH_TEST;
        public boolean depthWrite = false;

        public static PipelineSettings gui() {
            return new PipelineSettings();
        }

        public static PipelineSettings esp() {
            PipelineSettings settings = new PipelineSettings();
            settings.depthTest = DepthTestFunction.LEQUAL_DEPTH_TEST;
            settings.depthWrite = true;
            return settings;
        }

        public static PipelineSettings glow() {
            PipelineSettings settings = new PipelineSettings();
            settings.blend = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ZERO, DestFactor.ONE);
            return settings;
        }

        public static PipelineSettings glowEsp() {
            PipelineSettings settings = glow();
            settings.depthTest = DepthTestFunction.LEQUAL_DEPTH_TEST;
            return settings;
        }

        public static PipelineSettings sky() {
            PipelineSettings settings = new PipelineSettings();
            settings.depthTest = DepthTestFunction.LEQUAL_DEPTH_TEST;
            settings.depthWrite = false;
            return settings;
        }
    }

    private static final Map<String, RenderLayer> CACHED_LAYERS = new ConcurrentHashMap<>();
    private static MappableRingBuffer uniformRingBuffer;
    private static int ringBufferOffset = 0;
    private static final int RING_BUFFER_SIZE = 65536;
    private static final org.joml.Matrix4f IDENTITY_MATRIX = new org.joml.Matrix4f();
    private static final org.joml.Matrix4f ORTHO_MATRIX = new org.joml.Matrix4f();
    private static final org.joml.Vector4f WHITE_COLOR = new org.joml.Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final org.joml.Vector3f ZERO_OFFSET = new org.joml.Vector3f();
    private static int orthoWidth = -1;
    private static int orthoHeight = -1;
    private static double orthoScale = -1.0;

    public static RenderLayer getCustomLayer(String name, Identifier shaderId, VertexFormat format, VertexFormat.DrawMode drawMode) {
        return getCustomLayer(name, shaderId, format, drawMode, PipelineSettings.gui());
    }

    public static RenderLayer getCustomLayer(String name, Identifier shaderId, VertexFormat format, VertexFormat.DrawMode drawMode, PipelineSettings settings) {
        String cacheKey = name + "_" + shaderId.toString() + "_" + format.hashCode() + "_" + drawMode.name() + "_s_" + settings.cull + "_" + settings.depthWrite + "_" + settings.depthTest.name() + "_" + settings.blend;
        return CACHED_LAYERS.computeIfAbsent(cacheKey, key -> {
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(pipelineId(name, cacheKey))
                .withVertexShader(shaderId)
                .withFragmentShader(shaderId)
                .withVertexFormat(format, drawMode)
                .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                .withCull(settings.cull)
                .withDepthWrite(settings.depthWrite)
                .withDepthTestFunction(settings.depthTest);
            if (settings.blend != null) {
                builder.withBlend(settings.blend);
            }
            RenderPipeline pipeline = builder.build();
            RenderSetup setup = RenderSetup.builder(pipeline).build();
            return ru.zov.mixins.render.RenderLayerAccessor.of(name, setup);
        });
    }

    public static RenderLayer getCustomTexturedLayer(String name, Identifier shaderId, VertexFormat format, VertexFormat.DrawMode drawMode, Identifier texture) {
        return getCustomTexturedLayer(name, shaderId, format, drawMode, texture, PipelineSettings.gui());
    }

    public static RenderLayer getCustomTexturedLayer(String name, Identifier shaderId, VertexFormat format, VertexFormat.DrawMode drawMode, Identifier texture, PipelineSettings settings) {
        String cacheKey = name + "_" + shaderId.toString() + "_" + format.hashCode() + "_" + drawMode.name() + "_" + texture.toString() + "_s_" + settings.cull + "_" + settings.depthWrite + "_" + settings.depthTest.name() + "_" + settings.blend;
        return CACHED_LAYERS.computeIfAbsent(cacheKey, key -> {
            Identifier vertexShader = shaderId.getPath().endsWith("/data")
                ? Identifier.of(shaderId.getNamespace(), shaderId.getPath().substring(0, shaderId.getPath().length() - 4) + "vertex")
                : shaderId;
            Identifier fragmentShader = shaderId.getPath().endsWith("/data")
                ? Identifier.of(shaderId.getNamespace(), shaderId.getPath().substring(0, shaderId.getPath().length() - 4) + "fragment")
                : shaderId;
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(pipelineId(name, cacheKey))
                .withVertexShader(vertexShader)
                .withFragmentShader(fragmentShader)
                .withSampler("Sampler0")
                .withVertexFormat(format, drawMode)
                .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                .withCull(settings.cull)
                .withDepthWrite(settings.depthWrite)
                .withDepthTestFunction(settings.depthTest);
            if (settings.blend != null) {
                builder.withBlend(settings.blend);
            }
            RenderPipeline pipeline = builder.build();
            RenderSetup setup = RenderSetup.builder(pipeline)
                .texture("Sampler0", texture)
                .build();
            return ru.zov.mixins.render.RenderLayerAccessor.of(name, setup);
        });
    }

    public static RenderLayer getCustomMultiTexturedLayer(String name, Identifier shaderId, VertexFormat format, VertexFormat.DrawMode drawMode, String[] samplerNames) {
        return getCustomMultiTexturedLayer(name, shaderId, format, drawMode, samplerNames, PipelineSettings.gui());
    }

    public static RenderLayer getCustomMultiTexturedLayer(String name, Identifier shaderId, VertexFormat format, VertexFormat.DrawMode drawMode, String[] samplerNames, PipelineSettings settings) {
        String cacheKey = name + "_" + shaderId.toString() + "_" + format.hashCode() + "_" + drawMode.name() + "_" + String.join(",", samplerNames) + "_s_" + settings.cull + "_" + settings.depthWrite + "_" + settings.depthTest.name() + "_" + settings.blend;
        return CACHED_LAYERS.computeIfAbsent(cacheKey, key -> {
            Identifier vertexShader = shaderId;
            Identifier fragmentShader = shaderId;
            if (shaderId.getPath().endsWith("/data")) {
                String shaderPath = shaderId.getPath().substring(0, shaderId.getPath().length() - 5);
                vertexShader = Identifier.of(shaderId.getNamespace(), "core/sky/position_texture_color");
                fragmentShader = Identifier.of(shaderId.getNamespace(), shaderPath + "/fragment");
            }
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(pipelineId(name, cacheKey))
                .withVertexShader(vertexShader)
                .withFragmentShader(fragmentShader)
                .withVertexFormat(format, drawMode)
                .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                .withCull(settings.cull)
                .withDepthWrite(settings.depthWrite)
                .withDepthTestFunction(settings.depthTest);
            for (String sampler : samplerNames) {
                builder.withSampler(sampler);
            }
            if (settings.blend != null) {
                builder.withBlend(settings.blend);
            }
            RenderPipeline pipeline = builder.build();
            RenderSetup.Builder setupBuilder = RenderSetup.builder(pipeline);
            for (String sampler : samplerNames) {
                setupBuilder.texture(sampler, Identifier.of("minecraft", "dynamic/placeholder"));
            }
            return ru.zov.mixins.render.RenderLayerAccessor.of(name, setupBuilder.build());
        });
    }

    public static MappableRingBuffer getUniformRingBuffer() {
        if (uniformRingBuffer == null) {
            uniformRingBuffer = new MappableRingBuffer(() -> "client_uniforms", 130, RING_BUFFER_SIZE);
        }
        return uniformRingBuffer;
    }

    private static Identifier pipelineId(String name, String cacheKey) {
        String path = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return Identifier.of("zov", "pipeline/" + path + "/" + Integer.toUnsignedString(cacheKey.hashCode(), 16));
    }

    public static org.joml.Matrix4f getOrthoProjMatrix() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        double scale = mc.getWindow().getScaleFactor();
        if (width != orthoWidth || height != orthoHeight || scale != orthoScale) {
            ORTHO_MATRIX.setOrtho(0.0F, (float)width / (float)scale, (float)height / (float)scale, 0.0F, -21000.0F, 21000.0F);
            orthoWidth = width;
            orthoHeight = height;
            orthoScale = scale;
        }
        return ORTHO_MATRIX;
    }

    public static org.joml.Matrix4f getIdentityMatrix() {
        return IDENTITY_MATRIX;
    }

    public static GpuBufferSlice allocateUniforms(int size, java.util.function.Consumer<ByteBuffer> writer) {
        MappableRingBuffer ring = getUniformRingBuffer();
        int alignment = RenderSystem.getDevice().getUniformOffsetAlignment();
        int alignedSize = (size + alignment - 1) & ~(alignment - 1);
        
        if (ringBufferOffset + alignedSize > RING_BUFFER_SIZE) {
            ring.rotate();
            ringBufferOffset = 0;
        }
        
        GpuBuffer gpuBuffer = ring.getBlocking();
        GpuBufferSlice slice = gpuBuffer.slice(ringBufferOffset, size);
        
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (com.mojang.blaze3d.buffers.GpuBuffer.MappedView view = encoder.mapBuffer(slice, false, true)) {
            writer.accept(view.data());
        }
        
        ringBufferOffset += alignedSize;
        return slice;
    }

    public static void drawCustom(BuiltBuffer buffer, RenderLayer layer, GpuBufferSlice customUniforms) {
        drawCustom(buffer, layer, customUniforms, (Map<String, com.mojang.blaze3d.textures.GpuTextureView>) null);
    }

    public static void drawCustom(BuiltBuffer buffer, RenderLayer layer, GpuBufferSlice customUniforms, com.mojang.blaze3d.textures.GpuTextureView textureOverride) {
        java.util.Map<String, com.mojang.blaze3d.textures.GpuTextureView> map = null;
        if (textureOverride != null) {
            map = new java.util.HashMap<>();
            map.put("Sampler0", textureOverride);
        }
        drawCustom(buffer, layer, customUniforms, map);
    }

    public static void drawCustom(BuiltBuffer buffer, RenderLayer layer, GpuBufferSlice customUniforms, Map<String, com.mojang.blaze3d.textures.GpuTextureView> textureOverrides) {
        drawCustom(buffer, layer, customUniforms, textureOverrides, MinecraftClient.getInstance().getFramebuffer());
    }

    public static void drawCustom(BuiltBuffer buffer, RenderLayer layer, GpuBufferSlice customUniforms, Framebuffer target) {
        drawCustom(buffer, layer, customUniforms, null, target);
    }

    public static void drawCustom(BuiltBuffer buffer, RenderLayer layer, GpuBufferSlice customUniforms, Map<String, com.mojang.blaze3d.textures.GpuTextureView> textureOverrides, Framebuffer target) {
        try {
        RenderSetup setup = ((RenderLayerAccessor) layer).getRenderSetup();

        org.joml.Matrix4f modelView = modelViewOverride != null ? modelViewOverride : RenderSystem.getModelViewMatrix();
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
            modelView,
            WHITE_COLOR,
            ZERO_OFFSET,
            ((RenderSetupAccessor)(Object) setup).getTextureTransform().getTransformSupplier()
        );

        GpuBuffer vertexBuffer = ((ru.zov.mixins.render.RenderSetupAccessor)(Object)setup).getPipeline().getVertexFormat().uploadImmediateVertexBuffer(buffer.getBuffer());

        GpuBuffer indexBuffer;
        VertexFormat.IndexType indexType;
        if (buffer.getSortedBuffer() == null) {
            RenderSystem.ShapeIndexBuffer shapeIndex = RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());
            indexBuffer = shapeIndex.getIndexBuffer(buffer.getDrawParameters().indexCount());
            indexType = shapeIndex.getIndexType();
        } else {
            indexBuffer = ((ru.zov.mixins.render.RenderSetupAccessor)(Object)setup).getPipeline().getVertexFormat().uploadImmediateIndexBuffer(buffer.getSortedBuffer());
            indexType = buffer.getDrawParameters().indexType();
        }

        Map<String, RenderSetup.Texture> resolvedTextures = setup.resolveTextures();

        com.mojang.blaze3d.textures.GpuTextureView colorView = target.getColorAttachmentView();
        com.mojang.blaze3d.textures.GpuTextureView depthView = target.useDepthAttachment ? target.getDepthAttachmentView() : null;
        
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        
        RenderPass pass = encoder.createRenderPass(
            () -> "draw_custom",
            colorView,
            java.util.OptionalInt.empty(),
            depthView,
            java.util.OptionalDouble.empty()
        );
        
        pass.setPipeline(((RenderSetupAccessor)(Object) setup).getPipeline());
        
        if (ScissorManager.isScissorActive()) {
            int[] scissor = ScissorManager.getActiveScissor();
            pass.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
        } else {
            ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissor.isEnabled()) {
                pass.enableScissor(scissor.getX(), scissor.getY(), scissor.getWidth(), scissor.getHeight());
            }
        }
        
        RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("DynamicTransforms", dynamicTransforms);
        
        if (customUniforms != null) {
            pass.setUniform("Uniforms", customUniforms);
        }
        
        pass.setVertexBuffer(0, vertexBuffer);
        pass.setIndexBuffer(indexBuffer, indexType);
        
        for (Map.Entry<String, RenderSetup.Texture> entry : resolvedTextures.entrySet()) {
            com.mojang.blaze3d.textures.GpuTextureView view = entry.getValue().textureView();
            if (textureOverrides != null) {
                com.mojang.blaze3d.textures.GpuTextureView override = textureOverrides.get(entry.getKey());
                if (override != null) {
                    view = override;
                }
            }
            pass.bindTexture(entry.getKey(), view, entry.getValue().sampler());
        }
        
        pass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
        
        pass.close();
        } finally {
            buffer.close();
        }
    }
}







