package ru.zov.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zov.common.QuickImports;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.implement.features.modules.misc.AutoBuy;

@Mixin({GenericContainerScreen.class})
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> implements QuickImports {
   @Unique
   private final PlayerInventory playerInventory;
   @Shadow
   @Final
   private static Identifier TEXTURE;
   @Shadow
   @Final
   private int rows;

   public GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
      super(handler, inventory, title);
      this.playerInventory = inventory;
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   protected void init(CallbackInfo ci) {
      String title = this.title.getString().toLowerCase();
      if (!AutoBuy.getInstance().getVisibleName().equalsIgnoreCase(title) && !title.contains("маркет") && !title.contains("аукцион") && !title.contains("поиск")) {
         int y = mc.getWindow().getScaledHeight() / 2 - 80 - this.rows * 9;
         this.addDrawableChild(ButtonWidget.builder(Text.literal("Сложить"), (button) -> PlayerInventoryComponent.script.addTickStep(0, () -> {
               button.setFocused(false);
               PlayerInventoryUtil.getSlots().stream().filter((s) -> s.id > this.rows * 9 && !s.getStack().isEmpty()).forEach((s) -> PlayerInventoryUtil.clickSlot(s, 1, SlotActionType.QUICK_MOVE, false));
            })).width(80).position(mc.getWindow().getScaledWidth() / 2 - 83, y).build());
         this.addDrawableChild(ButtonWidget.builder(Text.literal("Забрать"), (button) -> PlayerInventoryComponent.script.addTickStep(0, () -> {
               button.setFocused(false);
               PlayerInventoryUtil.getSlots().stream().filter((s) -> s.id < this.rows * 9 && !s.getStack().isEmpty()).forEach((s) -> PlayerInventoryUtil.clickSlot(s, 1, SlotActionType.QUICK_MOVE, false));
            })).width(80).position(mc.getWindow().getScaledWidth() / 2 + 3, y).build());
      }
   }

   @Inject(
      method = {"drawBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
      int i = (this.width - this.backgroundWidth) / 2;
      int j = (this.height - this.backgroundHeight) / 2;
      context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.rows * 18 + 17, 256, 256);
      context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j + this.rows * 18 + 17, 0.0F, 126.0F, this.backgroundWidth, 96, 256, 256);
      Screen var9 = mc.currentScreen;
      if (var9 instanceof GenericContainerScreen screen) {
         if (screen.getTitle().getString().equals(AutoBuy.getInstance().getVisibleName())) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j + this.rows * 18 + 84, 0.0F, 139.0F, this.backgroundWidth, 96, 256, 256);
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j + this.rows * 18 + 138, 0.0F, 139.0F, this.backgroundWidth, 96, 256, 256);
         }
      }

      ci.cancel();
   }
}
