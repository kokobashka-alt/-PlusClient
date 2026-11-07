package ru.zov.mixins.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("pipeline")
    RenderPipeline getPipeline();

    @Accessor("textureTransform")
    TextureTransform getTextureTransform();
}



