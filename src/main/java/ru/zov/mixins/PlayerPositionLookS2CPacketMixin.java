package ru.zov.mixins;

import java.util.Set;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;

@Mixin({PlayerPositionLookS2CPacket.class})
public class PlayerPositionLookS2CPacketMixin implements QuickImports {
   @Shadow
   @Final
   private int teleportId;
   @Shadow
   @Final
   private Set<PositionFlag> relatives;
   @Shadow
   @Final
   private EntityPosition change;

   @Inject(
      method = {"apply(Lnet/minecraft/network/listener/ClientPlayPacketListener;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void applyHook(ClientPlayPacketListener clientPlayPacketListener, CallbackInfo ci) {
      Angle angle = AngleUtil.cameraAngle();
      if (!angle.equals(Angle.DEFAULT)) {
         clientPlayPacketListener.onPlayerPositionLook(new PlayerPositionLookS2CPacket(this.teleportId, this.change, this.relatives));
         PlayerIntersectionUtil.setAngle(angle.random(0.01F));
         ci.cancel();
      }

   }
}
