package ru.zov.mixins;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.implement.features.modules.render.EntityESP;
import ru.zov.implement.features.modules.combat.Aura;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void hideBelownameScore(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        EntityESP esp = EntityESP.getInstance();
        if (shouldHideVanillaPlayerLabel(state, esp)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (player instanceof ClientPlayerEntity clientPlayer) {
            Aura aura = Aura.getInstance();
            if (aura != null && aura.shouldHoldSprintVisual(clientPlayer)) {
                state.pose = aura.visualSprintPose();
                state.isSwimming = aura.visualSprintSwimming();
                state.leaningPitch = Math.max(state.leaningPitch, aura.visualSprintLeaningPitch());
            }
        }
        EntityESP esp = EntityESP.getInstance();
        if (shouldHideVanillaPlayerLabel(state, esp)) {
            state.playerName = null;
            state.displayName = null;
        }
    }

    @Unique
    private static boolean shouldHideVanillaPlayerLabel(PlayerEntityRenderState state, EntityESP esp) {
        return esp != null
                && esp.isState()
                && esp.entityType.isSelected("Player")
                && state.displayName != null;
    }
}
