package ru.zov.api.feature.draggable;

import java.util.ArrayList;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.implement.features.draggables.Armor;
import ru.zov.implement.features.draggables.BossBars;
import ru.zov.implement.features.draggables.CoolDowns;
import ru.zov.implement.features.draggables.HotBar;
import ru.zov.implement.features.draggables.HotKeys;
import ru.zov.implement.features.draggables.Inventory;
import ru.zov.implement.features.draggables.MediaPlayer;
import ru.zov.implement.features.draggables.MiniMap;
import ru.zov.implement.features.draggables.Notifications;
import ru.zov.implement.features.draggables.PlayerInfo;
import ru.zov.implement.features.draggables.Potions;
import ru.zov.implement.features.draggables.ScoreBoard;
import ru.zov.implement.features.draggables.StaffList;
import ru.zov.implement.features.draggables.TargetHud;
import ru.zov.implement.features.draggables.Watermark;
import ru.zov.implement.features.draggables.SystemUseTracker;
import ru.zov.implement.features.draggables.pouch.PouchWatermark;
import ru.zov.implement.features.draggables.pouch.PouchHotKeys;
import ru.zov.implement.features.draggables.pouch.PouchPotions;
import ru.zov.implement.features.draggables.pouch.PouchStaffList;
import ru.zov.implement.features.draggables.pouch.PouchTargetInfo;
import ru.zov.implement.features.draggables.pouch.PouchArmor;
import ru.zov.implement.features.draggables.pouch.PouchCooldowns;

public class DraggableRepository {
   private final List<AbstractDraggable> draggable = new ArrayList<>();

   @Native
   public void setup() {
      this.register(
         new TargetHud(),
         new Potions(),
         new HotKeys(),
         new BossBars(),
         new Armor(),
         new Watermark(),
         new Inventory(),
         new CoolDowns(),
         new StaffList(),
         new Notifications(),
         new ScoreBoard(),
         new MediaPlayer(),
         new HotBar(),
         new MiniMap(),
         new PlayerInfo(),
         new SystemUseTracker(),
         new PouchWatermark(),
    new PouchHotKeys(),
    new PouchPotions(),
    new PouchStaffList(),
    new PouchTargetInfo(),
    new PouchArmor(),
    new PouchCooldowns()
);
   }

   @Native
   public void register(AbstractDraggable... module) {
      this.draggable.addAll(List.of(module));
   }

   public List<AbstractDraggable> draggable() {
      return this.draggable;
   }

   public <T extends AbstractDraggable> T get(String name) {
      return this.draggable.stream().filter(module -> module.getName().equalsIgnoreCase(name)).map(module -> (T)module).findFirst().orElse(null);
   }

   public <T extends AbstractDraggable> T get(Class<T> clazz) {
      return this.draggable.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null);
   }
}
