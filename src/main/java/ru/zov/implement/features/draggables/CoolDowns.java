package ru.zov.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.other.StringUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.events.packet.PacketEvent;

public class CoolDowns extends AbstractDraggable {
   public final List<CoolDown> list = new ArrayList();

   public static CoolDowns getInstance() {
      return (CoolDowns)Instance.getDraggable(CoolDowns.class);
   }

   public CoolDowns() {
      super("CoolDowns", 120, 10, 80, 23, true);
   }

   public boolean visible() {
      return !this.list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   public void tick() {
      this.list.removeIf((coolDown) -> coolDown.anim.isFinished(Direction.BACKWARDS));
      this.list.stream().filter((coolDown) -> !ItemUtil.hasCooldown(coolDown.item)).forEach((coolDown) -> coolDown.anim.setDirection(Direction.BACKWARDS));
   }

   public void packet(PacketEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         Packet var10000 = e.getPacket();
         Objects.requireNonNull(var10000);
         Packet var2 = var10000;
         if (var2 instanceof CooldownUpdateS2CPacket cooldownUpdate) {
            Item item = (Item)Registries.ITEM.get(cooldownUpdate.cooldownGroup());
            this.list.stream().filter((coolDown) -> coolDown.item.equals(item)).forEach((coolDown) -> coolDown.anim.setDirection(Direction.BACKWARDS));
            int cooldown = MathHelper.clamp(cooldownUpdate.cooldown(), -1, 100000);
            if (cooldown != 0) {
               this.list.add(new CoolDown(item, (new StopWatch()).setMs(-cooldown * 50), (new DecelerateAnimation()).setMs(150).setValue((double)1.0F)));
            }
         } else if (var2 instanceof PlayerRespawnS2CPacket) {
            this.list.clear();
         }

      }
   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)17.5F).round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)(this.getY() + 17), (double)this.getWidth(), (double)(this.getHeight() - 17)).round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      float centerX = (float)this.getX() + (float)this.getWidth() / 2.0F;
      font.drawString(matrix, this.getName(), (double)((int)(centerX - font.getStringWidth(this.getName()) / 2.0F)), (double)(this.getY() + 7), ColorUtil.getText());
      int offset = 23;
      int maxWidth = 80;

      for(CoolDown coolDown : this.list) {
         float animation = coolDown.anim.getOutput().floatValue();
         float centerY = (float)(this.getY() + offset);
         int time = -coolDown.time.elapsedInt() / 100;
         String name = coolDown.item.getDefaultStack().getName().getString();
         String duration = StringUtil.getDurationMS(time, true);
         MathUtil.scale(matrix, centerX, centerY, 1.0F, animation, () -> {
            float green = time <= 50 ? MathUtil.blinking((double)1000.0F, 8.0F) : 1.0F;
            Render2DUtil.defaultDrawStack(context, coolDown.item.getDefaultStack(), (float)(this.getX() + 4), centerY - 3.0F, false, false, 0.5F);
            rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 15), (double)(centerY - 1.0F), (double)0.5F, (double)6.0F).color(ColorUtil.getOutline(1.0F, 0.5F)).build());
            fontCoolDown.drawString(matrix, name, (double)(this.getX() + 18), (double)(centerY + 1.0F), ColorUtil.getText());
            fontCoolDown.drawString(matrix, duration, (double)((float)(this.getX() + this.getWidth() - 5) - fontCoolDown.getStringWidth(duration)), (double)(centerY + 1.0F), ColorUtil.multGreen(ColorUtil.getText(), green));
         });
         int width = (int)fontCoolDown.getStringWidth(name + duration) + 30;
         maxWidth = Math.max(width, maxWidth);
         offset += (int)(11.0F * animation);
      }

      this.setWidth(maxWidth);
      this.setHeight(offset);
   }

   public static record CoolDown(Item item, StopWatch time, Animation anim) {
   }
}
