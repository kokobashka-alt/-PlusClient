package ru.zov.implement.features.modules.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.EntityStory;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.DrawEvent;

public class EntityFinder extends Module {
   public final List<EntityStory.CustomEntity> entities = new ArrayList();
   private final Script script = new Script();
   public final MultiSelectSetting renderType = (new MultiSelectSetting("Render Type", "Entity Display Type")).value("Tag", "Arrow").selected("Tag");
   private final ValueSetting radiusSetting = (new ValueSetting("Arrow Radius", "Radius of arrows")).setValue(50.0F).range(30, 100).visible(() -> this.renderType.isSelected("Arrow"));
   private final ValueSetting arrowSizeSetting = (new ValueSetting("Arrow Size", "Size of arrows")).setValue(16.0F).range(8, 20).visible(() -> this.renderType.isSelected("Arrow"));
   private final ValueSetting tagSizeSetting = (new ValueSetting("Tag Size", "Tags size")).setValue(13.0F).range(10, 20).visible(() -> this.renderType.isSelected("Tag"));
   private final ValueSetting timeSetting = (new ValueSetting("Time Until Deletion", "Time until tag removal")).setValue(5.0F).range(2, 20);

   public static EntityFinder getInstance() {
      return (EntityFinder)Instance.get(EntityFinder.class);
   }

   public EntityFinder() {
      super("EntityFinder", "Entity Finder", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.renderType, this.radiusSetting, this.arrowSizeSetting, this.tagSizeSetting, this.timeSetting});
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      org.joml.Matrix3x2fStack matrix = e.context().getMatrices();
      FontRenderer font = Fonts.getSize(this.tagSizeSetting.getInt(), Fonts.Type.DEFAULT);
      this.entities.forEach((ce) -> {
         Animation animation = ce.animation();
         Entity entity = ce.entity();
         Vec3d pos = entity.getEyePos();
         float anim = animation.getOutput().floatValue();
         if (this.renderType.isSelected("Tag")) {
            Vec3d vec = ProjectionUtil.worldToScreen(pos);
            Text customName = entity.getDisplayName();
            if (ProjectionUtil.canSee(pos) && customName != null) {
               MathUtil.multAlpha(anim, () -> {
                  MutableText text = customName.getString().length() < 3 ? Text.literal("") : customName.copy().append(" ");
                  double time = animation.isDirection(Direction.BACKWARDS) ? (double)this.timeSetting.getInt() : MathUtil.round((double)animation.counter.elapsedInt() / (double)1000.0F, (double)0.1F);
                  EntityESP var10000 = EntityESP.getInstance();
                  String var10003 = String.valueOf(Formatting.RED);
                  var10000.drawText(matrix, text.append("[" + var10003 + time + String.valueOf(Formatting.RESET) + "]"), vec.x, vec.y, font, ColorUtil.getColor(20, 12, 0, 125));
               });
            }
         }

         if (this.renderType.isSelected("Arrow")) {
            Render2DUtil.drawArrow(ce, this.arrowSizeSetting.getInt(), this.radiusSetting.getInt(), ColorUtil.getColor(255, 170, 10));
         }

      });
   }

   @EventHandler
   public void onTick(TickEvent e) {
      this.entities.removeIf((ce) -> {
         if (ce.animation().counter.elapsedInt() > this.timeSetting.getInt() * 1000) {
            ce.animation().setDirection(Direction.BACKWARDS);
         }

         return ce.animation().isFinished(Direction.BACKWARDS);
      });
      this.script.update().cleanup();
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet var10000 = e.getPacket();
      Objects.requireNonNull(var10000);
      Packet var2 = var10000;
      if (var2 instanceof EntityPositionSyncS2CPacket entitySync) {
         this.script.addTickStep(1, () -> {
            if (EntityStory.INSTANCE.getEntities().stream().noneMatch((ce) -> ce.entity().getId() == entitySync.id())) {
               EntityStory.INSTANCE.getLastPlayer(entitySync.id()).ifPresentOrElse((ent) -> {
                  Vec3d vec3d = entitySync.values().position();
                  ent.updatePosition(vec3d.x, vec3d.y, vec3d.z);
                  this.entities.add(new EntityStory.CustomEntity(ent, (new DecelerateAnimation()).setMs(200).setValue((double)1.0F)));
               }, () -> {
                  if (mc.world != null && mc.world.getEntityById(entitySync.id()) == null) {
                     Vec3d vec3d = entitySync.values().position();
                     LivingEntity living = EntityStory.INSTANCE.createEntity(EntityType.PLAYER);
                     living.setCustomName(Text.of(""));
                     living.updatePosition(vec3d.x, vec3d.y, vec3d.z);
                     this.entities.add(new EntityStory.CustomEntity(living, (new DecelerateAnimation()).setMs(200).setValue((double)1.0F)));
                  }

               });
            }

         });
      }
   }
}
