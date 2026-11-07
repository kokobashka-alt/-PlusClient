package ru.zov.api.system.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ru.zov.api.system.shape.Shape;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.render.RenderCompat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class Arc implements Shape, QuickImports {
    private static final Identifier SHADER_ID = Identifier.of("minecraft", "core/arc");

    @Override
    public void render(ShapeProperties shape) {
        float scale = (float) mc.getWindow().getScaleFactor();
        float alpha = RenderCompat.activeAlpha;

        Matrix4f matrix4f = shape.getPositionMatrix();
        Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0, new Vector3f()).mul(scale);
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
        Vector4f round = shape.getRound().mul(size.y);

        float width = shape.getWidth() * size.x;
        float height = shape.getHeight() * size.y;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        drawEngine.quad(matrix4f, buffer, shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());

        float c1_r = ColorUtil.redf(shape.getColor().x);
        float c1_g = ColorUtil.greenf(shape.getColor().x);
        float c1_b = ColorUtil.bluef(shape.getColor().x);
        float c1_a = ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().x, alpha));

        float c2_r = ColorUtil.redf(shape.getColor().y);
        float c2_g = ColorUtil.greenf(shape.getColor().y);
        float c2_b = ColorUtil.bluef(shape.getColor().y);
        float c2_a = ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().y, alpha));

        float loc_x = pos.x;
        float loc_y = mc.getWindow().getHeight() - height - pos.y;

        com.mojang.blaze3d.buffers.GpuBufferSlice uniforms = RenderCompat.allocateUniforms(192, bytes -> {
            com.mojang.blaze3d.buffers.Std140Builder builder = com.mojang.blaze3d.buffers.Std140Builder.intoBuffer(bytes);
            builder.putMat4f(RenderCompat.getIdentityMatrix()); // ModelViewMat (identity, since coords are already transformed)
            builder.putMat4f(RenderCompat.getOrthoProjMatrix()); // ProjMat
            builder.putVec4(c1_r, c1_g, c1_b, c1_a);             // color1
            builder.putVec4(c2_r, c2_g, c2_b, c2_a);             // color2
            builder.putVec2(width, height);                      // size
            builder.putVec2(loc_x, loc_y);                       // location
            builder.putFloat(round.x);                           // radius
            builder.putFloat(shape.getThickness());              // thickness
            builder.putFloat(shape.getStart());                  // start
            builder.putFloat(shape.getEnd());                    // end
        });

        RenderLayer layer = RenderCompat.getCustomLayer("arc_shape", SHADER_ID, VertexFormats.POSITION, VertexFormat.DrawMode.QUADS);
        RenderCompat.drawCustom(buffer.end(), layer, uniforms);
    }
}


