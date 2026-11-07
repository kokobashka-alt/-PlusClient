package ru.zov.common.util.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import ru.zov.api.event.EventHandler;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.common.QuickImports;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.EntityUpdateEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.features.modules.render.EntityFinder;
import ru.zov.implement.features.modules.movement.Speed;

public class EntityStory implements QuickImports {
   public static EntityStory INSTANCE = new EntityStory();
   private final List<PlayerEntity> lastPlayers = new ArrayList();
   private final List<CustomEntity> entities = new ArrayList();
   private final List<LivingEntity> list = new ArrayList();
   private final Script script = new Script();

   public EntityStory() {
      main.getEventManager().register(this);
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.entities.clear();
      this.list.clear();
      this.lastPlayers.clear();
   }

   @EventHandler
   public void onEntityUpdate(EntityUpdateEvent e) {
      Entity entity = e.entity();
      if (entity != mc.player) {
         switch (e.type()) {
            case ADD:
               if (entity instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)entity;
                  EntityFinder.getInstance().entities.removeIf((ce) -> ce.entity.getUuid().equals(living.getUuid()));
               }

               this.entities.add(new CustomEntity(entity, (new DecelerateAnimation()).setMs(1000).setValue((double)1.0F)));
               break;
            case REMOVE:
               this.entities.stream().filter((cp) -> cp.entity.getId() == entity.getId()).forEach((cp) -> {
                  entity.lastX = entity.getX();
                  entity.lastY = entity.getY();
                  entity.lastZ = entity.getZ();
                  cp.animation.setDirection(Direction.BACKWARDS);
               });
         }
      }

      if (entity instanceof PlayerEntity player) {
         if (!this.lastPlayers.contains(player)) {
            this.lastPlayers.add(player);
         }
      }

   }

   @EventHandler(0)
   public void onTick(TickEvent e) {
      this.entities.removeIf((cp) -> cp.animation.isFinished(Direction.BACKWARDS));
      this.list.removeIf((living) -> {
         ++living.age;
         return living.age > 3;
      });
      boolean trackOtherEntities = Instance.get(Speed.class).isState();
      for(Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity living) {
            if (living != mc.player && !trackOtherEntities) {
               continue;
            }
            LivingEntity prev = this.createEntity(living.getType());
            prev.copyFrom(living);
            prev.age = 0;
            prev.setId(living.getId());
            prev.dimensions = living.dimensions;
            this.list.add(prev);
         }
      }
   }

   public LivingEntity getEntityOrDefault(LivingEntity living, int ticks) {
      return (LivingEntity)this.getEntity(living, ticks).orElse(living);
   }

   public Optional<LivingEntity> getEntity(LivingEntity living, int ticks) {
      return this.list.stream().filter((s) -> s.getId() == living.getId() && s.age == ticks).findFirst();
   }

   public Stream<LivingEntity> getEntities(LivingEntity living, int maxTicks) {
      return this.list.stream().filter((s) -> s.getId() == living.getId() && s.age <= maxTicks);
   }

   public Optional<PlayerEntity> getLastPlayer(int id) {
      return this.lastPlayers.stream().filter((s) -> s.getId() == id).findFirst();
   }

   public LivingEntity createEntity(EntityType<?> type) {
      return new LivingEntity((EntityType<? extends LivingEntity>)type, mc.world) {
         public Iterable<ItemStack> getArmorItems() {
            return null;
         }

         public ItemStack getEquippedStack(EquipmentSlot slot) {
            return null;
         }

         public void equipStack(EquipmentSlot slot, ItemStack stack) {
         }

         public Arm getMainArm() {
            return null;
         }
      };
   }

   public List<PlayerEntity> getLastPlayers() {
      return this.lastPlayers;
   }

   public List<CustomEntity> getEntities() {
      return this.entities;
   }

   public List<LivingEntity> getList() {
      return this.list;
   }

   public Script getScript() {
      return this.script;
   }

   public static record CustomEntity(Entity entity, Animation animation) {
   }
}
