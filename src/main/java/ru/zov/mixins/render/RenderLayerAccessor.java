package ru.zov.mixins.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderLayer.class)
public interface RenderLayerAccessor {
    @Accessor("renderSetup")
    RenderSetup getRenderSetup();

    @org.spongepowered.asm.mixin.gen.Invoker("of")
    static RenderLayer of(String name, RenderSetup setup) {
        throw new UnsupportedOperationException();
    }
}



