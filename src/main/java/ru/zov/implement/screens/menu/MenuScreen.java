package ru.zov.implement.screens.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.api.system.sound.SoundManager;
import ru.zov.common.QuickImports;
import ru.zov.common.util.math.MathUtil;
import ru.zov.implement.screens.menu.components.AbstractComponent;
import ru.zov.implement.screens.menu.components.implement.other.BackgroundComponent;
import ru.zov.implement.screens.menu.components.implement.other.CategoryContainerComponent;
import ru.zov.implement.screens.menu.components.implement.other.SearchComponent;
import ru.zov.implement.screens.menu.components.implement.other.UserComponent;
import ru.zov.implement.screens.menu.components.implement.settings.TextComponent;

public class MenuScreen extends Screen implements QuickImports {
   public static MenuScreen INSTANCE = new MenuScreen();
   private final List<AbstractComponent> components = new ArrayList();
   private final BackgroundComponent backgroundComponent = new BackgroundComponent();
   private final UserComponent userComponent = new UserComponent();
   private final SearchComponent searchComponent = new SearchComponent();
   private final CategoryContainerComponent categoryContainerComponent = new CategoryContainerComponent();
   public final Animation animation = (new DecelerateAnimation()).setMs(200).setValue((double)1.0F);
   public ModuleCategory category;
   public int x;
   public int y;
   public int width;
   public int height;

   public void initialize() {
      this.animation.setDirection(Direction.FORWARDS);
      this.categoryContainerComponent.initializeCategoryComponents();
      this.components.addAll(Arrays.asList(this.backgroundComponent, this.userComponent, this.searchComponent, this.categoryContainerComponent));
   }

   public MenuScreen() {
      super(Text.of("MenuScreen"));
      this.category = ModuleCategory.COMBAT;
      this.initialize();
   }

   @Override
   public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
      return this.mouseClicked(click.x(), click.y(), click.button()) || super.mouseClicked(click, doubleClick);
   }

   @Override
   public boolean mouseReleased(net.minecraft.client.gui.Click click) {
      return this.mouseReleased(click.x(), click.y(), click.button()) || super.mouseReleased(click);
   }

   @Override
   public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
      return this.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY) || super.mouseDragged(click, deltaX, deltaY);
   }

   @Override
   public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
      return this.keyPressed(input.key(), input.scancode(), input.modifiers()) || super.keyPressed(input);
   }

   @Override
   public boolean charTyped(net.minecraft.client.input.CharInput input) {
      return this.charTyped((char)input.codepoint(), input.modifiers()) || super.charTyped(input);
   }

   public void tick() {
      this.close();
      this.components.forEach(AbstractComponent::tick);
      super.tick();
   }

   @Native
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.x = window.getScaledWidth() / 2 - 200;
      this.y = window.getScaledHeight() / 2 - 125;
      this.width = 400;
      this.height = 250;
      rectangle.render(ShapeProperties.create(context.getMatrices(), (double)0.0F, (double)0.0F, (double)window.getScaledWidth(), (double)window.getScaledHeight()).color(MathUtil.applyOpacity(-16777216, 100.0F * this.getScaleAnimation())).build());
      this.backgroundComponent.position((float)this.x, (float)this.y).size((float)this.width, (float)this.height);
      this.userComponent.position((float)this.x, (float)(this.y + this.height));
      this.searchComponent.position((float)(this.x + 300), (float)(this.y + 6));
      this.categoryContainerComponent.position((float)this.x, (float)this.y);
      MathUtil.scale(context.getMatrices(), (float)this.x + (float)this.width / 2.0F, (float)this.y + (float)this.height / 2.0F, this.getScaleAnimation(), () -> {
         this.components.forEach((component) -> component.render(context, mouseX, mouseY, delta));
         windowManager.render(context, mouseX, mouseY, delta);
      });
      super.render(context, mouseX, mouseY, delta);
   }

   @Native
   public void openGui() {
      this.animation.setDirection(Direction.FORWARDS);
      mc.setScreen(this);
      SoundManager.playSound(SoundManager.OPEN_GUI);
   }

   public float getScaleAnimation() {
      return this.animation.getOutput().floatValue();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!windowManager.mouseClicked(mouseX, mouseY, button)) {
         this.components.forEach((component) -> component.mouseClicked(mouseX, mouseY, button));
      }

      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.components.forEach((component) -> component.mouseReleased(mouseX, mouseY, button));
      windowManager.mouseReleased(mouseX, mouseY, button);
      return false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (!windowManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
         this.components.forEach((component) -> component.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
      }

      return false;
   }

   @Native
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
      if (!windowManager.mouseScrolled(mouseX, mouseY, vertical)) {
         this.components.forEach((component) -> component.mouseScrolled(mouseX, mouseY, vertical));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256 && this.shouldCloseOnEsc()) {
         SoundManager.playSound(SoundManager.CLOSE_GUI);
         this.animation.setDirection(Direction.BACKWARDS);
         return true;
      } else {
         if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            this.components.forEach((component) -> component.keyPressed(keyCode, scanCode, modifiers));
         }

         return false;
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (!windowManager.charTyped(chr, modifiers)) {
         this.components.forEach((component) -> component.charTyped(chr, modifiers));
      }

      return false;
   }

   public boolean shouldPause() {
      return false;
   }

   @Native
   public void close() {
      if (this.animation.isFinished(Direction.BACKWARDS)) {
         TextComponent.typing = false;
         super.close();
      }

   }

   public void setCategory(ModuleCategory category) {
      this.category = category;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public List<AbstractComponent> getComponents() {
      return this.components;
   }

   public BackgroundComponent getBackgroundComponent() {
      return this.backgroundComponent;
   }

   public UserComponent getUserComponent() {
      return this.userComponent;
   }

   public SearchComponent getSearchComponent() {
      return this.searchComponent;
   }

   public CategoryContainerComponent getCategoryContainerComponent() {
      return this.categoryContainerComponent;
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }
}
