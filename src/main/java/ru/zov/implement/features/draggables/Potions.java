package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Formatting;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.events.packet.PacketEvent;

public class Potions extends AbstractDraggable {
   private final List<Potion> list = new ArrayList();

   public Potions() {
      super("Potions", 210, 10, 80, 23, true);
   }

   public boolean visible() {
      return !this.list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   public void tick() {
      if (mc.player == null) {
         this.list.clear();
         return;
      }

      Collection<StatusEffectInstance> activeEffects = mc.player.getStatusEffects();
      for(StatusEffectInstance activeEffect : activeEffects) {
         Potion potion = this.list.stream()
            .filter(p -> p.effect.getEffectType().equals(activeEffect.getEffectType()))
            .findFirst()
            .orElse(null);
         if (potion == null) {
            this.list.add(new Potion(activeEffect, (new DecelerateAnimation()).setMs(150).setValue((double)1.0F)));
         } else {
            potion.effect = activeEffect;
            potion.anim.setDirection(Direction.FORWARDS);
         }
      }

      this.list.stream()
         .filter(potion -> activeEffects.stream().noneMatch(effect -> effect.getEffectType().equals(potion.effect.getEffectType())))
         .forEach(potion -> potion.anim.setDirection(Direction.BACKWARDS));
      this.list.removeIf((p) -> p.anim.isFinished(Direction.BACKWARDS));
      this.list.sort(Comparator.comparingInt((p) -> p.effect.getEffectType().value().getCategory().equals(StatusEffectCategory.HARMFUL) ? 0 : -1));
   }

   public void packet(PacketEvent e) {
      Packet packet = e.getPacket();
      if (packet instanceof PlayerRespawnS2CPacket || packet instanceof GameJoinS2CPacket) {
         this.list.clear();
      }
   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.DEFAULT);
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)17.5F).round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)((float)this.getY() + 17.0F), (double)this.getWidth(), (double)(this.getHeight() - 17)).round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      float centerX = (float)this.getX() + (float)this.getWidth() / 2.0F;
      int offset = 23;
      int maxWidth = 80;
      font.drawString(matrix, this.getName(), (double)((int)(centerX - font.getStringWidth(this.getName()) / 2.0F)), (double)(this.getY() + 7), ColorUtil.getText());

      for(Potion potion : this.list) {
         StatusEffectInstance effect = potion.effect;
         float animation = potion.anim.getOutput().floatValue();
         float centerY = (float)(this.getY() + offset);
         int amplifier = effect.getAmplifier();
         boolean bad = ((StatusEffect)potion.effect.getEffectType().value()).getCategory().equals(StatusEffectCategory.HARMFUL);
         String var10000 = bad ? ColorUtil.formatting(ColorUtil.getColor(255, 100, 100)) : "" + String.valueOf(Formatting.RESET);
         String name = var10000 + ((StatusEffect)effect.getEffectType().value()).getName().getString() + String.valueOf(Formatting.RESET);
         String duration = this.getDuration(effect);
         String lvl = amplifier > 0 ? ColorUtil.formatting(ColorUtil.getClientColor()) + " " + (amplifier + 1) + String.valueOf(Formatting.RESET) : "";
         MathUtil.scale(matrix, centerX, centerY, 1.0F, animation, () -> {
            float blinking = effect.getDuration() != -1 && effect.getDuration() <= 120 ? MathUtil.blinking((double)1000.0F, 8.0F) : 1.0F;
            int color = bad ? ColorUtil.multGreen(ColorUtil.getText(), blinking) : ColorUtil.multRed(ColorUtil.getText(), blinking);
            context.drawGuiTexture(
               RenderPipelines.GUI_TEXTURED,
               net.minecraft.client.gui.hud.InGameHud.getEffectTexture(effect.getEffectType()),
               this.getX() + 5,
               (int)centerY - 2,
               8,
               8
            );
            rectangle.render(ShapeProperties.create(matrix, (double)this.getX() + (double)14.5F, (double)(centerY - 1.0F), (double)0.5F, (double)7.0F).color(ColorUtil.getOutline(0.75F, 0.5F)).build());
            fontPotion.drawString(matrix, name + lvl, (double)(this.getX() + 18), (double)(centerY + 1.0F), ColorUtil.getText());
            fontPotion.drawString(matrix, duration, (double)((float)(this.getX() + this.getWidth() - 5) - fontPotion.getStringWidth(duration)), (double)(centerY + 1.0F), color);
         });
         int width = (int)fontPotion.getStringWidth(name + lvl + duration) + 30;
         maxWidth = Math.max(width, maxWidth);
         offset += (int)(11.0F * animation);
      }

      this.setWidth(maxWidth);
      this.setHeight(offset);
   }

   private String getDuration(StatusEffectInstance pe) {
      int var1 = pe.getDuration();
      int mins = var1 / 1200;
      return !pe.isInfinite() && mins <= 60 ? mins + ":" + String.format("%02d", var1 % 1200 / 20) : "**:**";
   }

   private static final class Potion {
      private StatusEffectInstance effect;
      private final Animation anim;

      private Potion(StatusEffectInstance effect, Animation anim) {
         this.effect = effect;
         this.anim = anim;
      }
   }
}
