package ru.zov.implement.features.modules.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;
import org.joml.Vector4d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.DrawEvent;

public class Esp2D extends Module {
   private static final int OUTLINE = 0xB4000000;
   private static final float HEALTH_TEXT_RANGE_SQ = 900.0F;
   private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   private final MultiSelectSetting targets = new MultiSelectSetting("Выбор целей", "Какие сущности показывать")
      .value("Игроки", "Мобы", "Животные", "Невидимые")
      .selected("Игроки");
   private final MultiSelectSetting display = new MultiSelectSetting("Отображать", "Что рисовать у сущности")
      .value("Бокс", "Здоровье", "Неймтег", "Пинг", "Броню", "Эффекты", "Предмет")
      .selected("Здоровье", "Неймтег", "Броню", "Эффекты", "Предмет");
   private final ValueSetting width = new ValueSetting("Ширина", "Толщина рамки и полоски здоровья")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(() -> this.display.isSelected("Бокс") || this.display.isSelected("Здоровье"));

   private List<LivingEntity> entities = new ArrayList<>();

   public Esp2D() {
      super("2D ESP", ModuleCategory.RENDER);
      this.setup(this.targets, this.display, this.width);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      List<LivingEntity> found = new ArrayList<>();
      if (mc.player != null && mc.world != null) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && this.shouldShow(living)) {
               found.add(living);
            }
         }
      }

      this.entities = found;
   }

   private boolean shouldShow(LivingEntity entity) {
      if (!entity.isAlive() || entity instanceof ArmorStandEntity) {
         return false;
      }

      if (entity == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
         return false;
      }

      if (entity.isInvisible() && !this.targets.isSelected("Невидимые")) {
         return false;
      }

      if (entity instanceof PlayerEntity) {
         return this.targets.isSelected("Игроки");
      }

      if (entity instanceof AnimalEntity) {
         return this.targets.isSelected("Животные");
      }

      return this.targets.isSelected("Мобы");
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (mc.player == null || this.entities.isEmpty()) {
         return;
      }

      DrawContext context = e.context();
      Matrix3x2fStack m = context.getMatrices();
      FontRenderer font = Fonts.getSize(12, Fonts.Type.DEFAULT);
      FontRenderer small = Fonts.getSize(9, Fonts.Type.DEFAULT);
      int c0 = ColorUtil.fade(0);
      int c1 = ColorUtil.fade(90);
      int c2 = ColorUtil.fade(180);
      int c3 = ColorUtil.fade(270);
      float t = this.width.getValue();

      for (LivingEntity entity : this.entities) {
         if (!entity.isAlive()) {
            continue;
         }

         Vector4d v = ProjectionUtil.getVector4D(entity);
         if (v == null || ProjectionUtil.cantSee(v)) {
            continue;
         }

         float x1 = (float) v.x;
         float y1 = (float) v.y;
         float x2 = (float) v.z;
         float y2 = (float) v.w;
         float w = x2 - x1;
         float h = y2 - y1;
         float tagTop = y1;
         float tagHeight = 0.0F;
         Text tag = null;
         float tagWidth = 0.0F;
         float tagX = 0.0F;

         if (this.display.isSelected("Неймтег")) {
            tag = this.buildTag(entity);
            tagHeight = font.getStringHeight(tag) + 2.0F;
            tagWidth = font.getStringWidth(tag) + 2.0F;
            tagX = x1 + w / 2.0F - tagWidth / 2.0F - 1.0F;
            tagTop = y1 - tagHeight - 1.5F;
         }

         if (this.display.isSelected("Бокс")) {
            this.drawFrame(m, x1, y1, x2, y2, t, c0, c1, c2, c3);
         }

         if (tag != null) {
            blur.render(ShapeProperties.create(m, tagX, tagTop, tagWidth, tagHeight).round(1.0F).color(OUTLINE).build());
            font.drawText(m, tag, tagX + 1.0F, tagTop + 1.5F);
         }

         if (this.display.isSelected("Предмет")) {
            this.drawItemNames(m, entity, small, x1 + w / 2.0F, y2);
         }

         if (this.display.isSelected("Броню")) {
            this.drawArmorRow(context, entity, x1, x2, tagTop);
         }

         if (this.display.isSelected("Эффекты")) {
            this.drawEffects(m, entity, Fonts.getSize(10, Fonts.Type.DEFAULT), x2 + 2.0F, y1 + 1.0F);
         }

         if (this.display.isSelected("Здоровье")) {
            this.drawHealthBar(m, entity, small, x1 - t - 1.5F, y1, t, h, c0, c3);
         }
      }
   }

   private Text buildTag(LivingEntity entity) {
      MutableText text = Text.empty().append(entity.getDisplayName());
      text.append(Text.literal(" | ").formatted(Formatting.GRAY)).append(Text.literal((int) entity.getHealth() + "HP").formatted(Formatting.RED));
      float absorption = entity.getAbsorptionAmount();
      if (absorption > 0.0F) {
         text.append(Text.literal(" | ").formatted(Formatting.GRAY)).append(Text.literal("+" + (int) absorption).formatted(Formatting.YELLOW));
      }

      if (this.display.isSelected("Пинг") && entity instanceof PlayerEntity player && mc.getNetworkHandler() != null) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         if (entry != null) {
            text.append(Text.literal(" | ").formatted(Formatting.GRAY)).append(Text.literal(entry.getLatency() + "ms").formatted(Formatting.WHITE));
         }
      }

      return text;
   }

   private void bar(Matrix3x2fStack m, float x, float y, float w, float h, int tl, int bl, int br, int tr) {
      rectangle.render(ShapeProperties.create(m, x, y, w, h).round(0.0F).color(tl, bl, br, tr).build());
   }

   private void drawFrame(Matrix3x2fStack m, float x1, float y1, float x2, float y2, float t, int c0, int c1, int c2, int c3) {
      float o = 0.5F;
      float w = x2 - x1;
      float h = y2 - y1;
      this.frameLines(m, x1 - o, y1 - o, x2 + o, y2 + o, w + o * 2.0F, h + o * 2.0F, t + o * 2.0F, OUTLINE, OUTLINE, OUTLINE, OUTLINE);
      this.frameLines(m, x1, y1, x2, y2, w, h, t, c0, c1, c2, c3);
   }

   private void frameLines(Matrix3x2fStack m, float x1, float y1, float x2, float y2, float w, float h, float t, int c0, int c1, int c2, int c3) {
      this.bar(m, x1 + t, y1, w - t * 2.0F, t, c0, c0, c1, c1);
      this.bar(m, x1, y1, t, h, c0, c3, c3, c0);
      this.bar(m, x1 + t, y2 - t, w - t * 2.0F, t, c3, c3, c2, c2);
      this.bar(m, x2 - t, y1, t, h, c1, c2, c2, c1);
   }

   private void drawHealthBar(Matrix3x2fStack m, LivingEntity entity, FontRenderer font, float x, float y, float w, float h, int top, int bottom) {
      float frac = Math.max(0.0F, Math.min(1.0F, entity.getHealth() / Math.max(1.0F, entity.getMaxHealth())));
      float fillHeight = h * frac;
      float fillTop = y + h - fillHeight;
      this.bar(m, x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, OUTLINE, OUTLINE, OUTLINE, OUTLINE);
      this.bar(m, x, fillTop, w, fillHeight, top, bottom, bottom, top);

      boolean hurt = entity.getHealth() < entity.getMaxHealth() * 0.9F;
      boolean near = entity == mc.player || mc.player.squaredDistanceTo(entity) < HEALTH_TEXT_RANGE_SQ;
      if (hurt && near) {
         Text percent = Text.literal(Math.round(frac * 100.0F) + "%");
         font.drawText(m, percent, x + w / 2.0F - font.getStringWidth(percent) / 2.0F, fillTop - font.getStringHeight(percent) - 1.0F);
      }
   }

   private void drawItemNames(Matrix3x2fStack m, LivingEntity entity, FontRenderer font, float centerX, float bottom) {
      float y = bottom + 3.0F;
      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack stack = entity.getEquippedStack(slot);
         if (stack.isEmpty()) {
            continue;
         }

         MutableText text = Text.empty().append(stack.getName());
         if (stack.getCount() > 1) {
            text.append(Text.literal(" x" + stack.getCount()).formatted(Formatting.RED));
         }

         font.drawText(m, text, centerX - font.getStringWidth(text) / 2.0F, y);
         y += font.getStringHeight(text) + 1.0F;
      }
   }

   private void drawArmorRow(DrawContext context, LivingEntity entity, float x1, float x2, float tagTop) {
      List<ItemStack> items = new ArrayList<>();
      ItemStack mainHand = entity.getMainHandStack();
      ItemStack offHand = entity.getOffHandStack();
      if (!mainHand.isEmpty()) {
         items.add(mainHand);
      }

      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = entity.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            items.add(stack);
         }
      }

      if (!offHand.isEmpty()) {
         items.add(offHand);
      }

      if (items.isEmpty()) {
         return;
      }

      float step = 10.0F;
      float startX = x1 + (x2 - x1) / 2.0F - step * (items.size() - 1) / 2.0F - 4.5F;
      float y = tagTop - 9.0F;
      for (int i = 0; i < items.size(); i++) {
         Render2DUtil.defaultDrawStack(context, items.get(i), startX + i * step, y, false, true, 0.5F);
      }
   }

   private void drawEffects(Matrix3x2fStack m, LivingEntity entity, FontRenderer font, float x, float y) {
      for (StatusEffectInstance effect : entity.getStatusEffects()) {
         String name = effect.getEffectType().value().getName().getString();
         String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
         String time = effect.isInfinite() ? "**:**" : (effect.getDuration() / 20 / 60) + ":" + String.format("%02d", effect.getDuration() / 20 % 60);
         Text line = Text.literal(name + level + " " + time);
         font.drawText(m, line, x, y);
         y += font.getStringHeight(line) + 1.0F;
      }
   }
}
