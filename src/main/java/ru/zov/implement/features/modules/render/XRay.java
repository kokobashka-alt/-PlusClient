package ru.zov.implement.features.modules.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

public class XRay extends Module {
   private final Map<BlockPos, BlockState> map = new HashMap();
   private final SelectSetting modeSetting = (new SelectSetting("Mode", "Operating mode for XRay")).value("Block Update");
   private final MultiSelectSetting blockTypeSetting = (new MultiSelectSetting("Blocks", "Blocks that will be displayed")).value("Ancient Debris", "Diamond", "Emerald", "Iron", "Gold");

   public XRay() {
      super("XRay", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.modeSetting, this.blockTypeSetting});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      this.map.forEach((key, value) -> {
         if (this.blockTypeSetting.getSelected().toString().toLowerCase().contains(this.getBlockName(value))) {
            Render3DUtil.drawBox(new Box(key), this.getColorByBlock(value), 1.0F);
         }

      });
   }

   @EventHandler
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.map.clear();
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent e) {
      switch (e.type()) {
         case UPDATE -> e.list().forEach((pair) -> {
   BlockState state = pair.state();
   BlockPos pos = pair.pos();
   if (this.getColorByBlock(state) != -1 && !this.map.containsKey(pos)) {
      this.map.put(pos, state);
   }

   if (this.map.containsKey(pos) && !((BlockState)this.map.get(pos)).equals(state)) {
      this.map.remove(pos);
   }

});
         case UNLOAD -> e.list().forEach((pair) -> this.map.remove(pair.pos()));
      }
      if (e.type() == BlockUpdateEvent.Type.UNLOAD && e.chunkPos() != null) {
         this.map.keySet().removeIf(pos -> pos.getX() >> 4 == e.chunkPos().x && pos.getZ() >> 4 == e.chunkPos().z);
      }

   }

   private int getColorByBlock(BlockState block) {
      int var10000;
      switch (this.getBlockName(block)) {
         case "ancient debris" -> var10000 = -5868204;
         case "diamond" -> var10000 = -15107199;
         case "emerald" -> var10000 = -12482789;
         case "iron" -> var10000 = -9090017;
         case "gold" -> var10000 = -3819208;
         default -> var10000 = -1;
      }

      return var10000;
   }

   private String getBlockName(BlockState state) {
      return state.getBlock().asItem().toString().replace("minecraft:", "").replace("_ore", "").replace("_", " ");
   }
}
