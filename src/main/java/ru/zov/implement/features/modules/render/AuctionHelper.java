package ru.zov.implement.features.modules.render;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.implement.events.container.HandledScreenEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.features.modules.misc.auction.AuctionUtil;

public class AuctionHelper extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final Script script = new Script();
   private Slot cheapestSlot;
   private Slot costEffectiveSlot;
   private final int[] RED_GREEN_COLORS = new int[]{-11796661, -46261};
   private final ColorSetting cheapestItemColorSetting;
   private final ColorSetting costEffectiveItemColorSetting;

   public AuctionHelper() {
      super("AuctionHelper", "Auction Helper", ModuleCategory.RENDER);
      this.cheapestItemColorSetting = (new ColorSetting("Cheapest Item", "Highlight color for the lowest priced item.")).setColor(-11796661).presets(this.RED_GREEN_COLORS);
      this.costEffectiveItemColorSetting = (new ColorSetting("Cost Effective Item", "Highlight color for the best item.")).setColor(-46261).presets(this.RED_GREEN_COLORS);
      this.setup(new Setting[]{this.cheapestItemColorSetting, this.costEffectiveItemColorSetting});
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof ScreenHandlerSlotUpdateS2CPacket || packet instanceof OpenScreenS2CPacket) {
         this.stopWatch.reset();
         this.script.cleanup().addTickStep(0, () -> {
            Screen patt0$temp = mc.currentScreen;
            if (patt0$temp instanceof GenericContainerScreen screen) {
               this.cheapestSlot = this.findSlotWithLowestPrice(((GenericContainerScreenHandler)screen.getScreenHandler()).slots);
               this.costEffectiveSlot = this.findSlotWithBestPricePerItem(((GenericContainerScreenHandler)screen.getScreenHandler()).slots);
            }

         });
      }

   }

   @EventHandler
   public void onHandledScreen(HandledScreenEvent e) {
      HandledScreen<?> screen = e.screen();
      String title = screen.getTitle().getString().toLowerCase();
      if (title.contains("аукцион") || title.contains("поиск")) {
         if (this.stopWatch.finished((double)100.0F)) {
            this.script.update();
         }

         org.joml.Matrix3x2fStack matrix = e.context().getMatrices();
         matrix.pushMatrix();
         matrix.translate((float)screen.x, (float)screen.y);
         if (this.cheapestSlot != this.costEffectiveSlot) {
            Render3DUtil.highlightSlot(matrix, this.cheapestSlot, this.getBlinkingColor(this.cheapestItemColorSetting.getColor()));
         }

         Render3DUtil.highlightSlot(matrix, this.costEffectiveSlot, this.getBlinkingColor(this.costEffectiveItemColorSetting.getColor()));
         matrix.popMatrix();
      }

   }

   private int getBlinkingColor(int color) {
      float alpha = (float)Math.abs(Math.sin((double)System.currentTimeMillis() / (double)10.0F * Math.PI / (double)180.0F));
      return ColorUtil.multAlpha(color, alpha);
   }

   private Slot findSlotWithLowestPrice(List<Slot> slots) {
      return (Slot)slots.stream().filter(this::hasValidPrice).min(Comparator.comparingInt(AuctionUtil::getPrice)).orElse(null);
   }

   private Slot findSlotWithBestPricePerItem(List<Slot> slots) {
      return (Slot)slots.stream().filter(this::hasValidPrice).min(Comparator.comparingInt((slot) -> AuctionUtil.getPrice(slot) / slot.getStack().getCount())).orElse(null);
   }

   private boolean hasValidPrice(Slot slot) {
      return AuctionUtil.getPrice(slot) >= 0;
   }
}
