package ru.zov.implement.features.draggables;

import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.TriggerBot;
import ru.zov.implement.features.modules.render.Hud;

public class TargetHud extends AbstractDraggable {
   private final Animation animation = (new DecelerateAnimation()).setMs(200).setValue((double)1.0F);
   private final StopWatch stopWatch = new StopWatch();
   private LivingEntity lastTarget;
   private Item lastItem;
   private float health;

   public TargetHud() {
      super("Target Hud", 10, 40, 100, 36, true);
      this.lastItem = Items.AIR;
   }

   public boolean visible() {
      return this.scaleAnimation.isDirection(Direction.FORWARDS);
   }

   public void tick() {
      Hud hud = Hud.getInstance();
      LivingEntity auraTarget = Aura.getInstance().getTarget();
      LivingEntity triggerBotTarget = TriggerBot.getInstance().getTarget();
      if (hud.isState() && hud.interfaceSettings.isSelected("Target Hud")) {
         if (auraTarget != null) {
            this.lastTarget = auraTarget;
            this.startAnimation();
         } else if (triggerBotTarget != null) {
            this.lastTarget = triggerBotTarget;
            this.startAnimation();
            this.stopWatch.reset();
         } else if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            this.lastTarget = mc.player;
            this.startAnimation();
         } else if (this.stopWatch.finished((double)500.0F)) {
            this.stopAnimation();
         }

      }
   }

   public void drawDraggable(DrawContext context) {
      if (this.lastTarget != null) {
         org.joml.Matrix3x2fStack matrix = context.getMatrices();
         this.drawUsingItem(context, matrix);
         this.drawArmor(context, matrix);
         this.drawMain(matrix);
      }

   }

   private void drawMain(org.joml.Matrix3x2fStack matrix) {
      FontRenderer font = Fonts.getSize(18);
      float hp = PlayerIntersectionUtil.getHealth(this.lastTarget);
      float widthHp = 61.0F;
      String stringHp = PlayerIntersectionUtil.getHealthString(hp);
      this.health = MathHelper.clamp(MathUtil.interpolateSmooth((double)1.0F, this.health, (float)Math.round(hp / this.lastTarget.getMaxHealth() * widthHp)), 2.0F, widthHp);
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)this.getHeight()).round(5.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      if (font.getStringWidth(this.lastTarget.getName().getString()) > 60.0F) {
         ScissorManager scissorManager = main.getScissorManager();
         scissorManager.push(matrix, (float)this.getX(), (float)this.getY(), (float)(this.getWidth() - 1), (float)this.getHeight());
         font.drawGradientString(matrix, this.lastTarget.getName().getString(), (double)(this.getX() + 34), (double)(this.getY() + 8), ColorUtil.getText(), ColorUtil.getText(0.6F));
         scissorManager.pop();
      } else {
         font.drawString(matrix, this.lastTarget.getName().getString(), (double)(this.getX() + 34), (double)(this.getY() + 8), ColorUtil.getText());
      }

      rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 34), (double)((float)this.getY() + 27.0F), (double)widthHp, (double)2.0F).round(0.75F).color(ColorUtil.getRectDarker(0.5F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 34), (double)((float)this.getY() + 27.0F), (double)this.health, (double)2.0F).softness(4.0F).round(1.0F).color(ColorUtil.roundClientColor(0.2F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 34), (double)((float)this.getY() + 27.0F), (double)this.health, (double)2.0F).round(0.75F).color(ColorUtil.roundClientColor(1.0F)).build());
      float width = Fonts.getSize(14).getStringWidth(stringHp);
      Fonts.getSize(14).drawString(matrix, stringHp, (double)((float)this.getX() + MathHelper.clamp(34.0F + this.health - width / 2.0F, 34.0F, 95.0F - width)), (double)(this.getY() + 21), ColorUtil.getText());
      Render2DUtil.drawTexture(matrix, Render2DUtil.getHeadId(this.lastTarget), (float)(this.getX() + 5), (float)this.getY() + 5.5F, 25.0F, 3.0F, 1, 1, 8, ColorUtil.getRect(1.0F), ColorUtil.multRed(-1, 1.0F + (float)this.lastTarget.hurtTime / 4.0F));
   }

   private void drawArmor(DrawContext context, org.joml.Matrix3x2fStack matrix) {
      List<ItemStack> items = java.util.Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmorSlot).map(this.lastTarget::getEquippedStack).filter((s) -> !s.isEmpty()).toList();
      if (!items.isEmpty()) {
         float x = (float)this.getX() + (float)this.getWidth() / 2.0F - (float)items.size() * 5.5F;
         float y = (float)(this.getY() - 13);
         float itemX = -10.5F;
         matrix.pushMatrix();
         matrix.translate(x, y);
         blur.render(ShapeProperties.create(matrix, (double)0.0F, (double)0.0F, (double)(items.size() * 11), (double)11.0F).round(2.5F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

         for(ItemStack stack : items) {
            Render2DUtil.defaultDrawStack(context, stack, itemX += 11.0F, 0.5F, false, true, 0.5F);
         }

         matrix.popMatrix();
      }

   }

   private void drawUsingItem(DrawContext context, org.joml.Matrix3x2fStack matrix) {
      this.animation.setDirection(this.lastTarget.isUsingItem() ? Direction.FORWARDS : Direction.BACKWARDS);
      if (!this.lastTarget.getActiveItem().isEmpty() && this.lastTarget.getActiveItem().getCount() != 0) {
         this.lastItem = this.lastTarget.getActiveItem().getItem();
      }

      if (!this.animation.isFinished(Direction.BACKWARDS) && !this.lastItem.equals(Items.AIR)) {
         int size = 24;
         float anim = this.animation.getOutput().floatValue();
         float progress = ((float)this.lastTarget.getItemUseTime() + tickCounter.getTickProgress(true)) / (float)ItemUtil.maxUseTick(this.lastItem) * 360.0F;
         float x = (float)this.getX() - (float)(size + 5) * anim;
         float y = (float)(this.getY() + 6);
         ScissorManager scissorManager = main.getScissorManager();
         scissorManager.push(matrix, (float)(this.getX() - 50), (float)this.getY(), 50.0F, (float)this.getHeight());
         MathUtil.multAlpha(anim, () -> {
            matrix.pushMatrix();
            matrix.translate(x, y);
            blur.render(ShapeProperties.create(matrix, (double)0.0F, (double)0.0F, (double)size, (double)size).round(12.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
            arc.render(ShapeProperties.create(matrix, (double)0.0F, (double)0.0F, (double)size, (double)size).round(0.4F).thickness(0.2F).end(progress).color(ColorUtil.fade(0), ColorUtil.fade(200), ColorUtil.fade(0), ColorUtil.fade(200)).build());
            Render2DUtil.defaultDrawStack(context, this.lastItem.getDefaultStack(), 3.0F, 3.0F, false, false, 1.0F);
            matrix.popMatrix();
         });
         scissorManager.pop();
      }

   }
}
