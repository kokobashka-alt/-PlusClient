package ru.zov.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLike;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.implement.events.player.EntityUpdateEvent;

@Mixin(targets = "net.minecraft.world.entity.ClientEntityManager$Listener")
public class ClientEntityManagerMixin<T extends EntityLike> implements QuickImports {
   @Shadow
   @Final
   private T entity;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   protected void init(CallbackInfo ci) {
      EntityLike var3 = this.entity;
      if (var3 instanceof Entity ent) {
         EventManager.callEvent(new EntityUpdateEvent(ent, EntityUpdateEvent.Type.ADD));
      }

   }

   @Inject(
      method = {"remove"},
      at = {@At("HEAD")}
   )
   public void removeEntityHook(Entity.RemovalReason reason, CallbackInfo ci) {
      EntityLike var4 = this.entity;
      if (var4 instanceof Entity ent) {
         EventManager.callEvent(new EntityUpdateEvent(ent, EntityUpdateEvent.Type.REMOVE));
      }

   }
}
