package ru.zov.api.feature.module;

import java.util.ArrayList;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.implement.features.modules.combat.AntiBot;
import ru.zov.implement.features.modules.combat.Aura;
import ru.zov.implement.features.modules.combat.AutoArmor;
import ru.zov.implement.features.modules.combat.AutoCrystal;
import ru.zov.implement.features.modules.combat.AutoSwap;
import ru.zov.implement.features.modules.combat.AutoTotem;
import ru.zov.implement.features.modules.combat.AutoWeb;
import ru.zov.implement.features.modules.combat.Criticals;
import ru.zov.implement.features.modules.combat.NoFriendDamage;
import ru.zov.implement.features.modules.combat.NoInteract;
import ru.zov.implement.features.modules.combat.TargetPearl;
import ru.zov.implement.features.modules.combat.TriggerBot;
import ru.zov.implement.features.modules.misc.AutoBuy;
import ru.zov.implement.features.modules.misc.AutoLeave;
import ru.zov.implement.features.modules.misc.AutoTpAccept;
import ru.zov.implement.features.modules.misc.ClanUpgrade;
import ru.zov.implement.features.modules.misc.ClickAction;
import ru.zov.implement.features.modules.misc.ContainerStealer;
import ru.zov.implement.features.modules.misc.DebugCamera;
import ru.zov.implement.features.modules.misc.ElytraHelper;
import ru.zov.implement.features.modules.misc.InteractThroughWalls;
import ru.zov.implement.features.modules.misc.ItemTweaks;
import ru.zov.implement.features.modules.misc.ServerHelper;
import ru.zov.implement.features.modules.misc.ServerRPSpoofer;
import ru.zov.implement.features.modules.movement.AbilitiesFly;
import ru.zov.implement.features.modules.movement.AirJump;
import ru.zov.implement.features.modules.movement.AutoSprint;
import ru.zov.implement.features.modules.movement.Blink;
import ru.zov.implement.features.modules.movement.ElytraFly;
import ru.zov.implement.features.modules.movement.ElytraRecast;
import ru.zov.implement.features.modules.movement.FireWorkBooster;
import ru.zov.implement.features.modules.movement.HighJump;
import ru.zov.implement.features.modules.movement.NoSlow;
import ru.zov.implement.features.modules.movement.ScreenWalk;
import ru.zov.implement.features.modules.movement.Speed;
import ru.zov.implement.features.modules.movement.WaterLeave;
import ru.zov.implement.features.modules.movement.WaterSpeed;
import ru.zov.implement.features.modules.player.AutoRespawn;
import ru.zov.implement.features.modules.player.AutoTool;
import ru.zov.implement.features.modules.player.AutoUse;
import ru.zov.implement.features.modules.player.EnderChestPlus;
import ru.zov.implement.features.modules.player.FastBreak;
import ru.zov.implement.features.modules.player.NameProtect;
import ru.zov.implement.features.modules.player.NoDelay;
import ru.zov.implement.features.modules.player.Nuker;
import ru.zov.implement.features.modules.render.Arrows;
import ru.zov.implement.features.modules.render.AuctionHelper;
import ru.zov.implement.features.modules.render.CameraTweaks;
import ru.zov.implement.features.modules.render.CrossHair;
import ru.zov.implement.features.modules.render.EntityESP;
import ru.zov.implement.features.modules.render.EntityFinder;
import ru.zov.implement.features.modules.render.HandTweaks;
import ru.zov.implement.features.modules.render.Hud;
import ru.zov.implement.features.modules.render.NoRender;
import ru.zov.implement.features.modules.render.ProjectilePrediction;
import ru.zov.implement.features.modules.render.SeeInvisible;
import ru.zov.implement.features.modules.render.WorldTweaks;
import ru.zov.implement.features.modules.render.XRay;
import ru.zov.implement.features.modules.combat.AutoCart;
import ru.zov.implement.features.modules.combat.AutoGApple;
import ru.zov.implement.features.modules.render.SystemHud;
import ru.zov.implement.features.modules.render.TargetESP;
import ru.zov.implement.features.modules.render.JumpCircle;
import ru.zov.implement.features.modules.combat.SpearAura;
import ru.zov.implement.features.modules.combat.CrystalAura;
import ru.zov.implement.features.modules.render.PouchHud;
import ru.zov.implement.features.modules.combat.AimAssist;
import ru.zov.implement.features.modules.render.Cubes;
import ru.zov.implement.features.modules.render.ViewModel;
import ru.zov.implement.features.modules.render.SwingAnimations;
import ru.zov.implement.features.modules.render.TalismanIndication;

public class ModuleRepository {
   private final List<Module> modules = new ArrayList<>();

   @Native
   public void setup() {
      this.register(
         new ServerHelper(),
         new WaterSpeed(),
         new ClickAction(),
         new ItemTweaks(),
         new Hud(),
         new AuctionHelper(),
         new ProjectilePrediction(),
         new XRay(),
         new Aura(),
         new Speed(),
         new AutoSwap(),
         new NoFriendDamage(),
         new AntiBot(),
         new AutoCrystal(),
         new AutoSprint(),
         new ElytraHelper(),
         new ClanUpgrade(),
         new NoDelay(),
         new AutoRespawn(),
         new NoSlow(),
         new WaterLeave(),
         new ElytraFly(),
         new ScreenWalk(),
         new Blink(),
         new ElytraRecast(),
         new AutoTool(),
         new Nuker(),
         new FastBreak(),
         new CameraTweaks(),
         new HandTweaks(),
         new InteractThroughWalls(),
         new NoRender(),
         new AutoTotem(),
         new EnderChestPlus(),
         new DebugCamera(),
         new TriggerBot(),
         new ContainerStealer(),
         new AutoTpAccept(),
         new Arrows(),
         new AutoLeave(),
         new WorldTweaks(),
         new EntityESP(),
         new TargetPearl(),
         new NameProtect(),
         new AbilitiesFly(),
         new Criticals(),
         new SeeInvisible(),
         new AutoArmor(),
         new AutoUse(),
         new AirJump(),
         new NoInteract(),
         new CrossHair(),
         new FireWorkBooster(),
         new HighJump(),
         new EntityFinder(),
         new AutoWeb(),
         new AutoBuy(),
         new ServerRPSpoofer(),
            new AutoCart(),
   new AutoGApple(),
   new SystemHud(),
   new TargetESP(),
   new SpearAura(),
  new JumpCircle(),
   new CrystalAura(),
   new PouchHud(),
   new AimAssist(),
   new Cubes(),
   new ViewModel(),
   new TalismanIndication(),
   new SwingAnimations()
);
   }

   @Native
   public void register(Module... module) {
      this.modules.addAll(List.of(module));
   }

   @Native
   public List<Module> modules() {
      return this.modules;
   }
}
