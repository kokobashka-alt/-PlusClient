package ru.zov.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.util.world.ServerUtil;

@Mixin({ClientCommonNetworkHandler.class})
public class ClientCommonNetworkHandlerMixin {
   @Shadow
   @Final
   protected @Nullable ServerInfo serverInfo;

   @Inject(
      method = {"onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/session/telemetry/WorldSession;setBrand(Ljava/lang/String;)V"
)}
   )
   public void onCustomPayloadHook(CustomPayloadS2CPacket packet, CallbackInfo ci, @Local(ordinal = 0) BrandCustomPayload brandCustomPayload) {
      ServerUtil.updateServerInfo(this.serverInfo, brandCustomPayload.brand());
   }
}
