package ru.zov.implement.screens.menu.components.implement.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.setting.SettingComponentAdder;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;
import ru.zov.implement.screens.menu.components.implement.other.CheckComponent;
import ru.zov.implement.screens.menu.components.implement.settings.AbstractSettingComponent;

public class ModuleComponent extends AbstractComponent {
   private final List<AbstractSettingComponent> components = new ArrayList<>();
   private final CheckComponent checkComponent = new CheckComponent();
   private final Module module;
   private boolean binding;

   @Native
   private void initialize() {
      new SettingComponentAdder().addSettingComponent(this.module.settings(), this.components);
   }

   public ModuleComponent(Module module) {
      this.module = module;
      this.initialize();
   }

   @Native
   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x, this.y, this.width, 18.0)
            .round(5.0F, 0.0F, 5.0F, 0.0F)
            .color(ColorUtil.getGuiRectColor2(1.0F))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x, this.y, this.width, this.height = this.getComponentHeight())
            .round(5.0F)
            .softness(1.0F)
            .thickness(2.2F)
            .outlineColor(-1876087231)
            .color(2960961)
            .build()
      );
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), this.module.getVisibleName(), this.x + 10.0F, this.y + 8.0F, -2828575);
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), "Enable", this.x + 9.0F, this.y + 27.0F, -2828575);
      Fonts.getSize(12, Fonts.Type.BOLD)
         .drawString(context.getMatrices(), "Enables the " + this.module.getVisibleName().toLowerCase() + " feature.", this.x + 9.0F, this.y + 36.0F, -7894892);
      ((CheckComponent)this.checkComponent.position(this.x + this.width - 16.0F, this.y + 28.5F))
         .setRunnable(this.module::switchState)
         .setState(this.module.isState())
         .render(context, mouseX, mouseY, delta);
      this.drawBind(context);
      float offset = this.y + 42.0F;

      for (int i = this.components.size() - 1; i >= 0; i--) {
         AbstractSettingComponent component = this.components.get(i);
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            component.x = this.x;
            component.y = offset + (this.getComponentHeight() - 46 - component.height);
            component.width = this.width;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      boolean isAnyComponentHovered = this.components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
      if (isAnyComponentHovered) {
         this.components.forEach(abstractComponent -> {
            if (abstractComponent.isHover(mouseX, mouseY)) {
               abstractComponent.mouseClicked(mouseX, mouseY, button);
            }
         });
         return super.mouseClicked(mouseX, mouseY, button);
      }

      String bindName = StringUtil.getBindName(this.module.getKey());
      float stringWidth = Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(bindName);
      if (MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 15.0F - stringWidth, this.y + 8.0F, stringWidth + 6.0F, 9.0) && button == 0) {
         this.binding = !this.binding;
      } else if (this.binding) {
         this.module.setKey(button);
         this.binding = false;
      }

      this.checkComponent.mouseClicked(mouseX, mouseY, button);
      this.components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      for (AbstractComponent abstractComponent : this.components) {
         if (abstractComponent.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   @Override
   public void tick() {
      this.components.forEach(AbstractComponent::tick);
      super.tick();
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      this.components.forEach(abstractComponent -> abstractComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      this.components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      int key = keyCode == 261 ? -1 : keyCode;
      if (this.binding) {
         this.module.setKey(key);
         this.binding = false;
      }

      this.components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }

   public int getComponentHeight() {
      float offsetY = 0.0F;

      for (AbstractSettingComponent component : this.components) {
         Supplier<Boolean> visible = component.getSetting().getVisible();
         if (visible == null || visible.get()) {
            offsetY += component.height;
         }
      }

      return (int)(offsetY + 46.0F);
   }

   private void drawBind(DrawContext context) {
      String bindName = StringUtil.getBindName(this.module.getKey());
      String name = this.binding ? "(" + bindName + ") ..." : bindName;
      float stringWidth = Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(name);
      rectangle.render(
         ShapeProperties.create(context.getMatrices(), this.x + this.width - stringWidth - 15.0F, this.y + 4.5F, stringWidth + 6.0F, 9.0)
            .round(2.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(1.0F))
            .build()
      );
      int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);
      Fonts.getSize(12, Fonts.Type.BOLD).drawString(context.getMatrices(), name, this.x + this.width - 12.0F - stringWidth, this.y + 8.0F, bindingColor);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ModuleComponent that = (ModuleComponent)o;
         return this.module.equals(that.module);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.module);
   }

   public List<AbstractSettingComponent> getComponents() {
      return this.components;
   }

   public CheckComponent getCheckComponent() {
      return this.checkComponent;
   }

   public Module getModule() {
      return this.module;
   }

   public boolean isBinding() {
      return this.binding;
   }
}
