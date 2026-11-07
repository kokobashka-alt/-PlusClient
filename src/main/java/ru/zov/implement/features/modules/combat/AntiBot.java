package ru.zov.implement.features.modules.combat;

import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;

public class AntiBot extends Module {
   private final Set<UUID> suspectSet = new HashSet();
   private final Set<UUID> botSet = new HashSet();
   private final Script script = new Script();

   public static AntiBot getInstance() {
      return (AntiBot)Instance.get(AntiBot.class);
   }

   public AntiBot() {
      super("AntiBot", ModuleCategory.COMBAT);
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof PlayerListS2CPacket list) {
         if (list.getActions().contains(Action.ADD_PLAYER)) {
            this.checkPlayerAfterSpawn(list);
         }
      } else if (var2 instanceof PlayerRemoveS2CPacket remove) {
         this.removePlayerBecauseLeftServer(remove);
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.script.update().cleanupIfFinished();
      if (!this.suspectSet.isEmpty()) {
         mc.world.getPlayers().stream().filter((p) -> this.suspectSet.contains(p.getUuid())).forEach(this::evaluateSuspectPlayer);
      }

   }

   private void checkPlayerAfterSpawn(PlayerListS2CPacket listS2CPacket) {
      listS2CPacket.getPlayerAdditionEntries().forEach((entry) -> {
         GameProfile profile = entry.profile();
         if (profile != null && !this.isRealPlayer(entry, profile)) {
            if (this.isDuplicateProfile(profile)) {
               this.botSet.add(profile.id());
            } else {
               this.suspectSet.add(profile.id());
            }

         }
      });
   }

   private void removePlayerBecauseLeftServer(PlayerRemoveS2CPacket removeS2CPacket) {
      removeS2CPacket.profileIds().forEach((uuid) -> {
         this.suspectSet.remove(uuid);
         this.botSet.remove(uuid);
      });
   }

   private boolean isRealPlayer(PlayerListS2CPacket.Entry entry, GameProfile profile) {
      return entry.latency() < 2 || profile.properties() != null && !profile.properties().isEmpty();
   }

   private void evaluateSuspectPlayer(PlayerEntity player) {
      if (this.isFullyEquipped(player)) {
         List<ItemStack> armor = this.getArmor(player).stream().map(ItemStack::copy).toList();
         this.script.addTickStep(1, () -> {
            if (!this.isFullyEquipped(player) || this.hasArmorChanged(player, armor)) {
               this.botSet.add(player.getUuid());
            }

            this.suspectSet.remove(player.getUuid());
         });
      }

   }

   public boolean isDuplicateProfile(GameProfile profile) {
      return ((ClientPlayNetworkHandler)Objects.requireNonNull(mc.getNetworkHandler())).getPlayerList().stream().filter((player) -> player.getProfile().name().equals(profile.name()) && !player.getProfile().id().equals(profile.id())).count() == 1L;
   }

   public boolean isFullyEquipped(PlayerEntity entity) {
      return this.getArmor(entity).stream().allMatch((stack) -> !stack.isEmpty());
   }

   public boolean hasArmorChanged(PlayerEntity entity, Iterable<ItemStack> prevArmor) {
      if (prevArmor == null) {
         return true;
      } else {
         List<ItemStack> currentArmorList = this.getArmor(entity);
         List<ItemStack> prevArmorList = StreamSupport.stream(prevArmor.spliterator(), false).toList();
         return !IntStream.range(0, Math.min(currentArmorList.size(), prevArmorList.size())).allMatch((i) -> ((ItemStack)currentArmorList.get(i)).equals(prevArmorList.get(i))) || currentArmorList.size() != prevArmorList.size();
      }
   }

   private List<ItemStack> getArmor(PlayerEntity entity) {
      return List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).stream().map(entity::getEquippedStack).toList();
   }

   public boolean isBot(PlayerEntity entity) {
      return this.botSet.contains(entity.getUuid());
   }

   public void reset() {
      this.suspectSet.clear();
      this.botSet.clear();
   }

   public void deactivate() {
      this.reset();
      super.deactivate();
   }
}
