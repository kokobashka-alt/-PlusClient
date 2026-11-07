package ru.zov.implement.features.modules.combat;

import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.repository.FriendUtils;
import ru.zov.implement.events.player.AttackEvent;

public class NoFriendDamage extends Module {
   public NoFriendDamage() {
      super("NoFriendDamage", "No Friend Damage", ModuleCategory.COMBAT);
   }

   @EventHandler
   @Native
   public void onAttack(AttackEvent e) {
      e.setCancelled(FriendUtils.isFriend(e.getEntity()));
   }
}
