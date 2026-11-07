package ru.zov.implement.screens.menu.components.implement.category;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.core.Main;
import ru.zov.implement.screens.menu.MenuScreen;
import ru.zov.implement.screens.menu.components.AbstractComponent;
import ru.zov.implement.screens.menu.components.implement.module.ModuleComponent;

public class CategoryComponent extends AbstractComponent {
   private final List<ModuleComponent> moduleComponents = new ArrayList();
   private static final Set<ModuleComponent> globalModuleComponents = new HashSet();
   private final ModuleCategory category;
   private final Animation alphaAnimation = (new DecelerateAnimation()).setMs(300).setValue((double)1.0F);

   @Native
   private void initialize() {
      for(Module module : Main.getInstance().getModuleRepository().modules()) {
         ModuleComponent newComponent = new ModuleComponent(module);
         if (globalModuleComponents.add(newComponent)) {
            this.moduleComponents.add(newComponent);
         }
      }

   }

   public CategoryComponent(ModuleCategory category) {
      this.category = category;
      this.initialize();
   }

   @Native
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      globalModuleComponents.clear();
      ScissorManager scissorManager = main.getScissorManager();
      this.drawCategoryTab(context, context.getMatrices());
      int[] offsets = this.calculateOffsets();
      int columnWidth = 137;
      int column = 0;
      int maxScroll = 0;
      float offsetX = 84.0F;
      float offsetY = 29.0F;
      scissorManager.push(context.getMatrices(), (float)menuScreen.x + offsetX, (float)menuScreen.y + offsetY, (float)menuScreen.width - offsetX, (float)menuScreen.height - offsetY - 1.0F);

      for(int i = this.moduleComponents.size() - 1; i >= 0; --i) {
         ModuleComponent component = (ModuleComponent)this.moduleComponents.get(i);
         if (this.shouldRenderComponent(component)) {
            int componentHeight = component.getComponentHeight() + 9;
            component.x = (float)(menuScreen.x + 95 + column * (columnWidth + 10));
            component.y = (float)((double)(menuScreen.y + 39 + offsets[column] - componentHeight) + this.smoothedScroll);
            component.width = (float)columnWidth;
            if (component.y > (float)(menuScreen.y - componentHeight) && (float)(menuScreen.y + menuScreen.height + 5) > component.y) {
               component.render(context, mouseX, mouseY, delta);
            }

            offsets[column] -= componentHeight;
            maxScroll = Math.max(maxScroll, offsets[column]);
            column = (column + 1) % 2;
         }
      }

      scissorManager.pop();
      int clamped = MathHelper.clamp(maxScroll - (menuScreen.height / 2 - 80), 0, maxScroll);
      this.scroll = MathHelper.clamp(this.scroll, (double)(-clamped), (double)0.0F);
      this.smoothedScroll = MathUtil.interpolateSmooth((double)2.0F, this.smoothedScroll, this.scroll);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      if (MathUtil.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height) && button == 0) {
         MenuScreen.INSTANCE.setCategory(this.category);
      }

      float offsetX = 84.0F;
      float offsetY = 29.0F;
      if (MathUtil.isHovered(mouseX, mouseY, (double)((float)menuScreen.x + offsetX), (double)((float)menuScreen.y + offsetY), (double)((float)menuScreen.width - offsetX), (double)((float)menuScreen.height - offsetY))) {
         boolean isAnyComponentHovered = this.moduleComponents.stream().anyMatch((moduleComponent) -> moduleComponent.isHover(mouseX, mouseY));
         if (isAnyComponentHovered) {
            this.moduleComponents.forEach((moduleComponent) -> {
               if (this.shouldRenderComponent(moduleComponent) && moduleComponent.isHover(mouseX, mouseY)) {
                  moduleComponent.mouseClicked(mouseX, mouseY, button);
               }

            });
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean isHover(double mouseX, double mouseY) {
      this.moduleComponents.forEach((moduleComponentx) -> moduleComponentx.isHover(mouseX, mouseY));

      for(ModuleComponent moduleComponent : this.moduleComponents) {
         if (moduleComponent.isHover(mouseX, mouseY)) {
            return true;
         }
      }

      return super.isHover(mouseX, mouseY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.moduleComponents.forEach((moduleComponent) -> moduleComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      float offsetX = 84.0F;
      float offsetY = 29.0F;
      if (MathUtil.isHovered(mouseX, mouseY, (double)((float)menuScreen.x + offsetX), (double)((float)menuScreen.y + offsetY), (double)((float)menuScreen.width - offsetX), (double)((float)menuScreen.height - offsetY))) {
         this.scroll += amount * (double)20.0F;
      }

      this.moduleComponents.forEach((moduleComponent) -> {
         if (this.shouldRenderComponent(moduleComponent)) {
            moduleComponent.mouseScrolled(mouseX, mouseY, amount);
         }

      });
      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.moduleComponents.forEach((moduleComponent) -> {
         if (this.shouldRenderComponent(moduleComponent)) {
            moduleComponent.keyPressed(keyCode, scanCode, modifiers);
         }

      });
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      this.moduleComponents.forEach((moduleComponent) -> {
         if (this.shouldRenderComponent(moduleComponent)) {
            moduleComponent.charTyped(chr, modifiers);
         }

      });
      return super.charTyped(chr, modifiers);
   }

   private void drawCategoryTab(DrawContext context, org.joml.Matrix3x2fStack matrix) {
      this.alphaAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(this.category) ? Direction.FORWARDS : Direction.BACKWARDS);
      float anim = this.alphaAnimation.getOutput().floatValue();
      int selectColor = MenuScreen.INSTANCE.getCategory().equals(this.category) ? ColorUtil.getClientColor() : ColorUtil.getText();
      if (anim != 0.0F) {
         rectangle.render(ShapeProperties.create(matrix, (double)this.x, (double)this.y, (double)this.width, (double)this.height).round(2.25F).thickness(2.0F).outlineColor(ColorUtil.getClientColor(anim)).color(ColorUtil.getGuiRectColor2(anim / 2.0F)).build());
      }

      String texture = "textures/" + this.category.getReadableName().toLowerCase() + ".png";
      image.setTexture(texture).render(ShapeProperties.create(matrix, (double)(this.x + 7.0F), (double)(this.y + 4.5F), (double)8.0F, (double)8.0F).color(selectColor).build());
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), this.category.getReadableName(), (double)((int)(this.x + 22.0F)), (double)(this.y + 7.0F), selectColor);
   }

   private int[] calculateOffsets() {
      int[] offsets = new int[2];
      int column = 0;

      for(int i = this.moduleComponents.size() - 1; i >= 0; --i) {
         ModuleComponent component = (ModuleComponent)this.moduleComponents.get(i);
         if (this.shouldRenderComponent(component)) {
            int componentHeight = component.getComponentHeight() + 9;
            offsets[column] += componentHeight;
            column = (column + 1) % 2;
         }
      }

      return offsets;
   }

   private boolean shouldRenderComponent(ModuleComponent component) {
      MenuScreen menuScreen = MenuScreen.INSTANCE;
      ModuleCategory moduleCategory = component.getModule().getCategory();
      String text = menuScreen.getSearchComponent().getText().toLowerCase();
      String moduleName = component.getModule().getVisibleName().toLowerCase();
      return text.equalsIgnoreCase("") ? moduleCategory.equals(menuScreen.getCategory()) : moduleName.replaceAll(" ", "").contains(text) || moduleName.contains(text);
   }
}
