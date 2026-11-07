package ru.zov.api.system.shape.implement;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.zov.api.system.shape.Shape;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.api.system.shape.implement.blur.BlurCapture;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.RenderCompat;

public class Blur implements Shape, QuickImports {
    private static final Identifier SHADER_ID = Identifier.of("minecraft", "core/blur");
    private static final Identifier INVENTORY_SHADER_ID = Identifier.of("zov", "core/inventory_blur");

    private static BlurCapture hudCapture;
    private static BlurCapture menuCapture;
    private static boolean menuFrame;
    private boolean capturedLayer;

    public Vector2f resolution = new Vector2f();

    private float flowStrength;
    private long lastFlowMs;

    @Override
    public void render(ShapeProperties shape) {
        render(shape, SHADER_ID, "blur_shape");
    }

    public void renderInventory(ShapeProperties shape) {
        render(shape, INVENTORY_SHADER_ID, "inventory_blur_shape");
    }

    public void setup() {
        capture();
    }

    public void capture() {
        getHudCapture().captureScreen();
    }

    public void captureCurrentLayer() {
        getHudCapture().captureScreen();
        capturedLayer = true;
    }

    public static void beginMenuFrame() {
        getMenuCapture().captureScreen();
        menuFrame = true;
    }

    public static void endMenuFrame() {
        menuFrame = false;
    }

    private void render(ShapeProperties shape, Identifier shaderId, String layerName) {
        if (RenderCompat.activeAlpha <= 0.005F) {
            return;
        }

        BlurCapture activeCapture = menuFrame ? getMenuCapture() : getHudCapture();
        if (capturedLayer) {
            activeCapture = getHudCapture();
        }

        float quality = shape.getQuality() * 1.5F;
        Framebuffer source = activeCapture.getBlurredFramebuffer(quality);
        if (source == null) {
            return;
        }

        resolution.set(source.textureWidth, source.textureHeight);
        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = RenderCompat.activeAlpha;
        Matrix4f matrix = shape.getPositionMatrix();
        Vector3f position = matrix.transformPosition(shape.getX(), shape.getY(), 0, new Vector3f()).mul(scale);
        Vector3f size = matrix.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);
        float softness = shape.getSoftness();
        float thickness = shape.getThickness();
        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix, buffer, shape.getX() - softness / 2.0F, shape.getY() - softness / 2.0F, shape.getWidth() + softness, shape.getHeight() + softness);

        int color1 = shape.getColor().x;
        int color2 = shape.getColor().y;
        int color3 = shape.getColor().z;
        int color4 = shape.getColor().w;
        int outlineColor = shape.getOutlineColor();
        float locationX = position.x;
        float locationY = mc.getWindow().getHeight() - height - position.y;
        float parentX = shape.getParentX() != 0 ? shape.getParentX() * scale : position.x;
        float parentY = shape.getParentY() != 0 ? mc.getWindow().getHeight() - shape.getParentHeight() * scale - shape.getParentY() * scale : locationY;
        float parentWidth = shape.getParentWidth() != 0 ? shape.getParentWidth() * scale : width;
        float parentHeight = shape.getParentHeight() != 0 ? shape.getParentHeight() * scale : height;
        float currentTime = (float) (System.currentTimeMillis() % 100000L) / 1000.0F;
        int waveEnabled = 1;
        int flowColor1 = ColorUtil.getClientColor();
        int flowColor2 = ColorUtil.getClientColorSecondary();
        float currentFlowStrength = updateFlowStrength(false);

        com.mojang.blaze3d.buffers.GpuBufferSlice uniforms = RenderCompat.allocateUniforms(384, bytes -> {
            com.mojang.blaze3d.buffers.Std140Builder builder = com.mojang.blaze3d.buffers.Std140Builder.intoBuffer(bytes);
            builder.putMat4f(RenderCompat.getIdentityMatrix());
            builder.putMat4f(RenderCompat.getOrthoProjMatrix());
            builder.putVec2(resolution.x, resolution.y);
            builder.putFloat(quality);
            builder.putVec4(ColorUtil.redf(color1), ColorUtil.greenf(color1), ColorUtil.bluef(color1), ColorUtil.alphaf(ColorUtil.multAlpha(color1, alpha)));
            builder.putVec4(ColorUtil.redf(color2), ColorUtil.greenf(color2), ColorUtil.bluef(color2), ColorUtil.alphaf(ColorUtil.multAlpha(color2, alpha)));
            builder.putVec4(ColorUtil.redf(color3), ColorUtil.greenf(color3), ColorUtil.bluef(color3), ColorUtil.alphaf(ColorUtil.multAlpha(color3, alpha)));
            builder.putVec4(ColorUtil.redf(color4), ColorUtil.greenf(color4), ColorUtil.bluef(color4), ColorUtil.alphaf(ColorUtil.multAlpha(color4, alpha)));
            builder.putVec4(ColorUtil.redf(outlineColor), ColorUtil.greenf(outlineColor), ColorUtil.bluef(outlineColor), ColorUtil.alphaf(ColorUtil.multAlpha(outlineColor, alpha)));
            builder.putVec2(width, height);
            builder.putVec2(locationX, locationY);
            builder.putVec4(round.x, round.y, round.z, round.w);
            builder.putFloat(thickness);
            builder.putFloat(softness);
            builder.putFloat(currentTime);
            builder.putInt(waveEnabled);
            builder.putFloat(alpha);
            builder.putInt(0);
            builder.putFloat(currentFlowStrength);
            builder.putVec3(ColorUtil.redf(flowColor1), ColorUtil.greenf(flowColor1), ColorUtil.bluef(flowColor1));
            builder.putVec3(ColorUtil.redf(flowColor2), ColorUtil.greenf(flowColor2), ColorUtil.bluef(flowColor2));
            builder.putVec2(parentX, parentY);
            builder.putVec2(parentWidth, parentHeight);
        });

        RenderLayer layer = RenderCompat.getCustomTexturedLayer(layerName, shaderId, VertexFormats.POSITION, VertexFormat.DrawMode.QUADS, Identifier.of("minecraft", "dynamic/input"));
        RenderCompat.drawCustom(buffer.end(), layer, uniforms, source.getColorAttachmentView());
        capturedLayer = false;
    }

    private float updateFlowStrength(boolean enabled) {
        long now = System.currentTimeMillis();
        float deltaTime = lastFlowMs == 0L ? 1.0F / 60.0F : Math.min((now - lastFlowMs) / 1000.0F, 0.1F);
        lastFlowMs = now;
        float target = enabled ? 1.0F : 0.0F;
        float rate = 1.0F - (float) Math.exp(-5.0F * deltaTime);
        flowStrength += (target - flowStrength) * rate;
        if (Math.abs(target - flowStrength) < 0.001F) {
            flowStrength = target;
        }
        return flowStrength;
    }

    private static BlurCapture getHudCapture() {
        if (hudCapture == null) {
            hudCapture = new BlurCapture();
        }
        return hudCapture;
    }

    private static BlurCapture getMenuCapture() {
        if (menuCapture == null) {
            menuCapture = new BlurCapture();
        }
        return menuCapture;
    }
}
