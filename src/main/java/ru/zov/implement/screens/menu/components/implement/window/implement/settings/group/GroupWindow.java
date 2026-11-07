package ru.zov.implement.screens.menu.components.implement.window.implement.settings.group;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.module.setting.SettingComponentAdder;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.screens.menu.components.AbstractComponent;
import ru.zov.implement.screens.menu.components.implement.settings.AbstractSettingComponent;
import ru.zov.implement.screens.menu.components.implement.window.AbstractWindow;

public class GroupWindow extends AbstractWindow {
   private final List<AbstractSettingComponent> components = new ArrayList();
   private final GroupSetting setting;

   public GroupWindow(GroupSetting setting) {
      this.setting = setting;
      (new SettingComponentAdder()).addSettingComponent(setting.getSubSettings(), this.components);
   }

   public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      ScissorManager scissorManager = main.getScissorManager();
      this.height = (float)MathHelper.clamp(this.getComponentHeight(), 0, 200);
      rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)this.height).round(4.0F).thickness(2.0F).softness(1.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1.0F)).build());
      FontRenderer var10000 = Fonts.getSize(15, Fonts.Type.BOLD);
      org.joml.Matrix3x2fStack var10001 = context.getMatrices();
      String var10002 = this.setting.getName();
      var10000.drawString(var10001, "Settings " + var10002, (double)(this.x + 9.0F), (double)(this.y + 10.0F), -1);
      boolean isLimitedHeight = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F;
      if (isLimitedHeight) {
         scissorManager.push(matrix, this.x, this.y + 23.0F, this.width, this.height - 28.0F);
      }

      float offset = 0.0F;
      int totalHeight = 0;

      for(int i = this.components.size() - 1; i >= 0; --i) {
         AbstractSettingComponent component = (AbstractSettingComponent)this.components.get(i);
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || (Boolean)visible.get()) {
            component.x = this.x;
            component.y = (float)((double)(this.y + 19.0F + offset + ((float)(this.getComponentHeight() - 25) - component.height)) + this.smoothedScroll);
            component.width = this.width;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
            totalHeight += (int)component.height;
         }
      }

      if (isLimitedHeight) {
         scissorManager.pop();
      }

      int maxScroll = (int)Math.max(0.0F, (float)totalHeight - (this.height - 23.0F));
      this.scroll = MathHelper.clamp(this.scroll, (double)(-maxScroll), (double)0.0F);
      this.smoothedScroll = MathHelper.lerp((double)0.1F, this.smoothedScroll, this.scroll);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.draggable(MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)19.0F) && button == 0);
      boolean isAnyComponentHovered = this.components.stream().anyMatch((abstractComponent) -> abstractComponent.isHover(mouseX, mouseY));
      if (isAnyComponentHovered) {
         this.components.forEach((abstractComponent) -> {
            if (abstractComponent.isHover(mouseX, mouseY)) {
               abstractComponent.mouseClicked(mouseX, mouseY, button);
            }

         });
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         this.components.forEach((abstractComponent) -> abstractComponent.mouseClicked(mouseX, mouseY, button));
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean isHover(double mouseX, double mouseY) {
      this.components.forEach((abstractComponentx) -> abstractComponentx.isHover(mouseX, mouseY));

      for(AbstractComponent abstractComponent : this.components) {
         if (abstractComponent.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return super.isHover(mouseX, mouseY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.components.forEach((abstractComponent) -> abstractComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      boolean scrolled = MathHelper.clamp(this.height, 0.0F, 200.0F) == 200.0F && MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height);
      if (scrolled) {
         this.scroll += amount * (double)20.0F;
      }

      this.components.forEach((abstractComponent) -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
      return scrolled;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.components.forEach((abstractComponent) -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach((abstractComponent) -> abstractComponent.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   public int getComponentHeight() {
      float offsetY = 0.0F;

      for(AbstractSettingComponent component : this.components) {
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || (Boolean)visible.get()) {
            offsetY += component.height;
         }
      }

      return (int)(offsetY + 25.0F);
   }

   public List<AbstractSettingComponent> getComponents() {
      return this.components;
   }

   public GroupSetting getSetting() {
      return this.setting;
   }
}
