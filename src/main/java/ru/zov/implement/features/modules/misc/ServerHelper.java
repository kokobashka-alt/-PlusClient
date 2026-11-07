package ru.zov.implement.features.modules.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.Render3DUtil;
import ru.zov.common.util.task.scripts.Script;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.container.OpenScreenEvent;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.packet.PacketEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.DrawEvent;
import ru.zov.implement.events.render.PlayerLoadEvent;
import ru.zov.implement.events.render.WorldRenderEvent;
import ru.zov.implement.features.draggables.CoolDowns;
import ru.zov.implement.features.modules.combat.AutoArmor;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.implement.features.modules.render.ProjectilePrediction;

public class ServerHelper extends Module {
   private final Map<BlockPos, BlockState> blockStateMap = new HashMap();
   private final List<ServerEvent> serverEvents = new ArrayList();
   private final List<Structure> structures = new ArrayList();
   private final List<KeyBind> keyBindings = new ArrayList();
   private final StopWatch shulkerWatch = new StopWatch();
   private final StopWatch repairWatch = new StopWatch();
   private final Script script = new Script();
   private final Script script2 = new Script();
   private final Map<Integer, Item> stacks = new HashMap();
   private final BooleanSetting autoShulkerSetting;
   private final BooleanSetting autoRepairSetting;
   private final BooleanSetting consumablesSetting;
   private final BooleanSetting autoPointSetting;

   public ServerHelper() {
      super("ServerHelper", "Server Helper", ModuleCategory.MISC);
      this.autoShulkerSetting = (new BooleanSetting("Auto Shulker [" + String.valueOf(Formatting.RED) + "TEST" + String.valueOf(Formatting.RESET) + "]", "Automatically puts loot into shulker")).setValue(true).visible(ServerUtil::isHolyWorld);
      this.autoRepairSetting = (new BooleanSetting("Auto Repair", "Auto repairs armor with experience bubble at low durability")).setValue(true).visible(ServerUtil::isHolyWorld);
      this.consumablesSetting = (new BooleanSetting("Consumables Timer", "Displays the time until the consumable runs out")).setValue(true).visible(() -> ServerUtil.isCopyTime() || ServerUtil.isHolyWorld());
      this.autoPointSetting = (new BooleanSetting("Auto Point", "Displays information on the event")).setValue(true).visible(ServerUtil::isFunTime);
      this.keyBindings.add(this.createBind(Items.FIREWORK_STAR, (new BindSetting("Anti Fly", "Anti Fly Key")).visible(ServerUtil::isReallyWorld)));
      this.keyBindings.add(this.createBind(Items.FLOWER_BANNER_PATTERN, (new BindSetting("Experience Scroll", "Experience Scroll Key")).visible(ServerUtil::isReallyWorld)));
      this.keyBindings.add(this.createBind(Items.PRISMARINE_SHARD, (new BindSetting("Explosive Trap", "Explosive Trap Key")).visible(ServerUtil::isHolyWorld), 5.0F));
      this.keyBindings.add(this.createBind(Items.POPPED_CHORUS_FRUIT, (new BindSetting("Default Trap", "Default Trap Key")).visible(ServerUtil::isHolyWorld)));
      this.keyBindings.add(this.createBind(Items.NETHER_STAR, (new BindSetting("Stun", "Stun Key")).visible(ServerUtil::isHolyWorld), 30.0F));
      this.keyBindings.add(this.createBind(Items.FIRE_CHARGE, (new BindSetting("Explosive Thing", "Explosive Thing Key")).visible(ServerUtil::isHolyWorld)));
      this.keyBindings.add(this.createBind(Items.SNOWBALL, (new BindSetting("SnowBall", "SnowBall Key")).visible(() -> ServerUtil.isCopyTime() || ServerUtil.isHolyWorld())));
      this.keyBindings.add(this.createBind(Items.PHANTOM_MEMBRANE, (new BindSetting("Gods Aura", "God's Aura Key")).visible(ServerUtil::isCopyTime)));
      this.keyBindings.add(this.createBind(Items.NETHERITE_SCRAP, (new BindSetting("Trap", "Trap Key")).visible(ServerUtil::isCopyTime)));
      this.keyBindings.add(this.createBind(Items.DRIED_KELP, (new BindSetting("Plast", "Plast Key")).visible(ServerUtil::isCopyTime)));
      this.keyBindings.add(this.createBind(Items.SUGAR, (new BindSetting("Clear Dust", "Clear Dust Key")).visible(ServerUtil::isCopyTime), 10.0F));
      this.keyBindings.add(this.createBind(Items.FIRE_CHARGE, (new BindSetting("Fire Tornado", "Fire Tornado Key")).visible(ServerUtil::isCopyTime), 10.0F));
      this.keyBindings.add(this.createBind(Items.ENDER_EYE, (new BindSetting("Disorientation", "Disorientation Key")).visible(ServerUtil::isCopyTime), 10.0F));
      this.keyBindings.forEach((bind) -> this.setup(new Setting[]{bind.setting}));
      this.setup(new Setting[]{this.consumablesSetting, this.autoPointSetting, this.autoShulkerSetting, this.autoRepairSetting});
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      this.keyBindings.stream().filter((bind) -> e.isKeyReleased(bind.setting) && this.validDistance(bind.distance)).forEach((bind) -> PlayerInventoryUtil.swapAndUse(bind.item));
   }

   @EventHandler
   @Native
   public void onPlayerLoad(PlayerLoadEvent e) {
      this.script2.cleanup();
      this.stacks.clear();
   }

   @EventHandler
   public void onOpenScreen(OpenScreenEvent e) {
      Screen var3 = e.getScreen();
      if (var3 instanceof GenericContainerScreen screen) {
         if (!this.script2.isFinished()) {
            if (screen.getTitle().getString().contains("Рюкзак")) {
               Script var10000 = this.script;
               Script var10002 = this.script2;
               Objects.requireNonNull(var10002);
               var10000.addTickStep(0, var10002::update);
            }

            e.setScreen((Screen)null);
         }
      }

   }

   @EventHandler
   @Native
   public void onPacket(PacketEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         Packet packet = e.getPacket();
         Objects.requireNonNull(packet);
         if (packet instanceof ItemPickupAnimationS2CPacket item) {
            if (this.autoShulkerSetting.isValue() && this.autoShulkerSetting.isVisible() && item.getCollectorEntityId() == mc.player.getId()) {
               Entity var25 = mc.world.getEntityById(item.getEntityId());
               if (var25 instanceof ItemEntity entity) {
                  ItemStack stack = entity.getStack();
                  if (!stack.contains(DataComponentTypes.CONTAINER)) {
                     this.stacks.put(-MathUtil.getRandom(1, 999999999), stack.getItem());
                     this.shulkerWatch.reset();
                  }
               }
            }
         } else if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slot) {
            if (slot.getSyncId() == 0) {
               Item item = slot.getStack().getItem();
               this.stacks.entrySet().stream().filter((entry) -> (Integer)entry.getKey() < 0 && ((Item)entry.getValue()).equals(item)).findFirst().ifPresent((entry) -> {
                  this.stacks.put(slot.getSlot() + 18, item);
                  this.stacks.remove(entry.getKey());
               });
            }
         } else if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDelta) {
            if (this.consumablesSetting.isValue() && ServerUtil.isCopyTime()) {
               chunkDelta.visitUpdates((pos, state) -> this.blockStateMap.put(pos.toImmutable(), state));
               this.script.addTickStep(0, () -> chunkDelta.visitUpdates((pos, state) -> {
                      Vec3d vec = pos.toImmutable().toCenterPos();
                      if (this.blockStateMap.size() > 50 && this.blockStateMap.size() < 600) {
                         if (this.isTrap(pos.up(2))) {
                            this.addStructure(Items.NETHERITE_SCRAP, vec, (double)15000.0F, 0.0F);
                         } else if (this.isBigTrap(pos.up(3))) {
                            this.addStructure(Items.NETHERITE_SCRAP, vec, (double)30000.0F, 0.0F);
                         }
                      }

                   }));
            }
         } else if (packet instanceof GameMessageS2CPacket gameMessage) {
            if (this.autoPointSetting.isValue() && this.autoPointSetting.isVisible()) {
               Text content = gameMessage.content();
               String contentString = content.toString();
               String message = content.getString();
               String name = StringUtils.substringBetween(message, "|||   [", "]   ");
               if (name != null) {
                  String position = StringUtils.substringBetween(contentString, "value='/gps ", "'");
                  String lvl = StringUtils.substringBetween(message, "\u0423\u0440\u043e\u0432\u0435\u043d\u044c \u043b\u0443\u0442\u0430: ", "\n \u2551");
                  String owner = StringUtils.substringBetween(message, "\u041f\u0440\u0438\u0437\u0432\u0430\u043d \u0438\u0433\u0440\u043e\u043a\u043e\u043c: ", "\n \u2551");
                  if (position != null) {
                     String[] pose = position.split(" ");
                     Vec3d center = BlockPos.ofFloored((double)Integer.parseInt(pose[0]), (double)Integer.parseInt(pose[1]), (double)Integer.parseInt(pose[2])).toCenterPos();
                     switch (name) {
                        case "\u041c\u0438\u0441\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u0441\u0443\u043d\u0434\u0443\u043a":
                           this.addEvent(name, lvl, owner, center, "overworld", 300, 0);
                           return;
                        case "\u0412\u0443\u043b\u043a\u0430\u043d":
                           this.addEvent(name, lvl, owner, center, "overworld", 300, 120);
                           return;
                        case "\u041c\u0435\u0442\u0435\u043e\u0440\u0438\u0442\u043d\u044b\u0439 \u0434\u043e\u0436\u0434\u044c":
                        case "\u041c\u0430\u044f\u043a \u0443\u0431\u0438\u0439\u0446\u0430":
                        case "\u041c\u0438\u0441\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439 \u0410\u043b\u0442\u0430\u0440\u044c":
                           this.addEvent(name, lvl, owner, center, "overworld", 360, 0);
                           return;
                        case "\u0417\u0430\u0433\u0430\u0434\u043e\u0447\u043d\u044b\u0439 \u043c\u0430\u044f\u043a":
                           this.addEvent(name, lvl, owner, center, "overworld", 60, 180);
                           return;
                     }
                  } else {
                     switch (name) {
                        case "\u0421\u0443\u043d\u0434\u0443\u043a \u0441\u043c\u0435\u0440\u0442\u0438":
                           this.addEvent(name, lvl, owner, BlockPos.ofFloored((double)-155.0F, (double)64.0F, (double)205.0F).toCenterPos(), "lobby", 300, 0);
                           return;
                        case "\u0410\u0434\u0441\u043a\u0430\u044f \u0440\u0435\u0437\u043d\u044f":
                           this.addEvent(name, lvl, owner, BlockPos.ofFloored((double)48.0F, (double)87.0F, (double)73.0F).toCenterPos(), "lobby", 180, 120);
                           return;
                     }
                  }
               }

               return;
            }

            String message = gameMessage.content().getString();
            if (message.contains("\u25b6 \u041f\u043e\u0432\u0442\u043e\u0440\u043d\u043e \u0430\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u041f\u0443\u0437\u044b\u0440\u044c \u043e\u043f\u044b\u0442\u0430 \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e \u0447\u0435\u0440\u0435\u0437")) {
               String subString = StringUtils.substringBetween(message, "\u0447\u0435\u0440\u0435\u0437 ", " \u0441\u0435\u043a\u0443\u043d\u0434");
               if (subString != null && !subString.isEmpty()) {
                  int duration = Integer.parseInt(subString) * 20;
                  ItemCooldownManager manager = mc.player.getItemCooldownManager();
                  manager.set(Items.EXPERIENCE_BOTTLE.getDefaultStack(), duration);
                  CoolDowns.getInstance().packet(new PacketEvent(new CooldownUpdateS2CPacket(manager.getGroup(Items.EXPERIENCE_BOTTLE.getDefaultStack()), duration), PacketEvent.Type.RECEIVE));
               }
            }
         } else if (packet instanceof PlaySoundS2CPacket sound && sound.getCategory().equals(SoundCategory.MASTER) && this.consumablesSetting.isValue() && ServerUtil.isHolyWorld()) {
            Vec3d vec3d = BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ()).toCenterPos();
            switch (sound.getSound().getIdAsString()) {
               case "minecraft:block.beacon.deactivate":
                  this.addStructure(Items.NETHER_STAR, vec3d, (double)15000.0F, 14.5F);
                  return;
               case "minecraft:entity.generic.explode":
                  this.addStructure(Items.PRISMARINE_SHARD, vec3d.add((double)0.0F, (double)-1.0F, (double)0.0F), (double)11000.0F, 0.0F);
                  return;
            }
         }
      }
   }

   @EventHandler(1)
   @Native
   public void onTick(TickEvent e) {
      if (this.autoRepairSetting.isValue() && this.autoRepairSetting.isVisible() && java.util.Arrays.stream(net.minecraft.entity.EquipmentSlot.values()).filter(net.minecraft.entity.EquipmentSlot::isArmorSlot).map(mc.player::getEquippedStack).anyMatch((stack) -> AutoArmor.getInstance().isBroken(stack) && ItemUtil.getEnchantmentLvl(stack, net.minecraft.enchantment.Enchantments.MENDING) != 0)) {
         PlayerInventoryUtil.slots().filter((slot) -> {
            Item item = slot.getStack().getItem();
            NbtComponent component = (NbtComponent)slot.getStack().get(DataComponentTypes.CUSTOM_DATA);
            return !ItemUtil.hasCooldown(item) && item.equals(Items.EXPERIENCE_BOTTLE) && component != null && component.toString().contains("полностью ремонтирует") && this.repairWatch.every((double)5000.0F);
         }).findFirst().ifPresent((slot) -> PlayerInventoryUtil.swapAndUse(slot, AngleUtil.cameraAngle(), true));
      }

      if (!this.stacks.isEmpty() && this.script2.isFinished() && this.script.isFinished() && this.shulkerWatch.finished((double)300.0F) && !PlayerInventoryUtil.isServerScreen() && !mc.player.isUsingItem()) {
         this.stacks.remove(mc.player.inventory.selectedSlot + 54);
         if (!this.stacks.isEmpty()) {
            PlayerInventoryUtil.slots().filter((s) -> {
               NbtComponent component = (NbtComponent)s.getStack().get(DataComponentTypes.CUSTOM_DATA);
               return component != null && component.toString().contains("\"litebackpacks:backpack\"");
            }).max(Comparator.comparingDouble((s) -> (double)((ContainerComponent)s.getStack().getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT)).stacks.stream().filter((item) -> !item.isEmpty()).toList().size())).ifPresent((shulker) -> {
               PlayerInventoryUtil.swapHand(shulker, Hand.MAIN_HAND, false);
               PlayerInventoryUtil.closeScreen(true);
               this.script.addTickStep(1, () -> {
                  PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, true);
                  this.script2.cleanup().addTickStep(0, () -> {
                     PlayerInventoryUtil.slots().forEach((slot) -> this.stacks.entrySet().stream().filter((entry) -> slot.inventory.equals(mc.player.inventory) && ((Item)entry.getValue()).equals(slot.getStack().getItem()) && (Integer)entry.getKey() == slot.id).forEach((entry) -> PlayerInventoryUtil.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false)));
                     PlayerInventoryUtil.closeScreen(false);
                     this.script.addTickStep(1, () -> {
                        PlayerInventoryUtil.swapHand(shulker, Hand.MAIN_HAND, false);
                        PlayerInventoryUtil.closeScreen(true);
                     });
                     this.shulkerWatch.reset();
                     this.stacks.clear();
                  });
               });
            });
         }
      }

      this.serverEvents.removeIf((event) -> event.timeEnd + (double)90000.0F - (double)System.currentTimeMillis() <= (double)0.0F);
      this.structures.removeIf((cons) -> cons.time - (double)System.currentTimeMillis() <= (double)0.0F);
      this.script.update().cleanupIfFinished();
      this.script2.cleanupIfFinished();
      this.blockStateMap.clear();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.player != null) {
         MatrixStack matrix = e.stack();
         this.keyBindings.forEach((bind) -> MathUtil.multAlpha(bind.anim.getOutput().floatValue(), () -> {
               BindSetting setting = bind.setting;
               Animation animation = bind.anim;
               Item item = bind.item;
               Slot slot = PlayerInventoryUtil.getSlot(item);
               animation.setDirection(PlayerIntersectionUtil.isKey(setting) && slot != null && !ItemUtil.hasCooldown(item));
               if (!animation.isFinished(Direction.BACKWARDS)) {
                  BlockPos playerPos = mc.player.getBlockPos();
                  Vec3d smooth = MathUtil.interpolate(Vec3d.of(BlockPos.ofFloored(mc.player.lastX, mc.player.lastY, mc.player.lastZ)), Vec3d.of(playerPos)).subtract(Vec3d.of(playerPos));
                  switch (setting.getName()) {
                     case "Trap":
                     case "Default Trap":
                        this.drawItemCube(playerPos, smooth, 1.99F, ColorUtil.getClientColor());
                        break;
                     case "Disorientation":
                     case "Fire Tornado":
                     case "Clear Dust":
                        this.drawItemRadius(bind.distance, ColorUtil.LIGHT_RED);
                        break;
                     case "Explosive Thing":
                        this.drawItemRadius(5.0F, ColorUtil.getClientColor());
                        break;
                     case "Plast":
                        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
                        if (Math.abs(mc.player.getPitch()) > 60.0F) {
                           BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
                           Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
                           Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
                           Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3.0F, true, true, true);
                        } else if (!(yaw <= -157.5F) && !(yaw >= 157.5F)) {
                           if (yaw <= -112.5F) {
                              this.drawSidePlast(playerPos.east(5).south().down(), smooth, ColorUtil.getClientColor(), -1, true);
                           } else if (yaw <= -67.5F) {
                              BlockPos blockPos = playerPos.east(2).up();
                              Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                              Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                              Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3.0F, true, true, true);
                           } else if (yaw <= -22.5F) {
                              this.drawSidePlast(playerPos.east(5).down(), smooth, ColorUtil.getClientColor(), 1, false);
                           } else if ((double)yaw >= (double)-22.5F && (double)yaw <= (double)22.5F) {
                              BlockPos blockPos = playerPos.south(2).up();
                              Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                              Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                              Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3.0F, true, true, true);
                           } else if (yaw <= 67.5F) {
                              this.drawSidePlast(playerPos.west(4).down(), smooth, ColorUtil.getClientColor(), 1, true);
                           } else if (yaw <= 112.5F) {
                              BlockPos blockPos = playerPos.west(3).up();
                              Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                              Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                              Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3.0F, true, true, true);
                           } else if (yaw <= 157.5F) {
                              this.drawSidePlast(playerPos.west(4).south().down(), smooth, ColorUtil.getClientColor(), -1, false);
                           }
                        } else {
                           BlockPos blockPos = playerPos.north(3).up();
                           Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                           Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                           Render3DUtil.drawBox(new Box(pos1, pos2), ColorUtil.getClientColor(), 3.0F, true, true, true);
                        }
                        break;
                     case "Explosive Trap":
                        this.drawItemCube(playerPos, smooth, 3.99F, ColorUtil.LIGHT_RED);
                        break;
                     case "Stun":
                        this.drawItemCube(playerPos, smooth, 15.01F, ColorUtil.LIGHT_RED);
                        break;
                     case "SnowBall":
                        if (slot != null) {
                           ProjectilePrediction.getInstance().drawPredictionItem(matrix, slot.getStack(), AngleUtil.cameraAngle());
                        }
                  }
               }

            }));
      }
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      DrawContext context = e.context();
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      this.structures.forEach((cons) -> {
         double time = (cons.time - (double)System.currentTimeMillis()) / (double)1000.0F;
         Vec3d vec3d = ProjectionUtil.worldToScreen(cons.vec);
         String text = MathUtil.round(time, (double)0.1F) + "с";
         FontRenderer font = Fonts.getSize(14);
         float width = font.getStringWidth(text);
         float posX = (float)(vec3d.x - (double)(width / 2.0F));
         float posY = (float)vec3d.y;
         float padding = 2.0F;
         if (cons.boxSize != 0.0F) {
            Render3DUtil.drawBox(Box.from(cons.vec.add((double)-0.5F)).expand((double)cons.boxSize), ColorUtil.getClientColor(), 3.0F, true, true, true);
         }

         if (ProjectionUtil.canSee(cons.vec)) {
            blur.render(ShapeProperties.create(matrix, (double)(posX - padding), (double)(posY - padding), (double)(width + padding * 2.0F), (double)10.0F).round(1.5F).color(ColorUtil.HALF_BLACK).build());
            font.drawString(matrix, text, (double)posX, (double)(posY + 1.0F), ColorUtil.getText());
            Render2DUtil.defaultDrawStack(context, cons.item.getDefaultStack(), posX - 14.0F, posY - 2.5F, true, false, 0.5F);
         }

      });
      this.serverEvents.forEach((event) -> {
         Vec3d vec3d = ProjectionUtil.worldToScreen(event.vec);
         double timeOpen = (event.timeOpen - (double)System.currentTimeMillis()) / (double)1000.0F;
         double timeEnd = (event.timeEnd - (double)System.currentTimeMillis()) / (double)1000.0F;
         double var10000 = mc.getEntityRenderDispatcher().camera.getCameraPos().distanceTo(event.vec);
         String distance = " [" + MathUtil.round(var10000, 0.1) + "m]";
         String time = timeOpen > (double)0.0F ? ("До начала: " + MathUtil.round(timeOpen, timeOpen < (double)30.0F ? (double)0.1F : (double)1.0F) + "с").replace(".0", "") : (timeEnd > (double)0.0F ? ("До конца: " + MathUtil.round(timeEnd, timeEnd < (double)30.0F ? (double)0.1F : (double)1.0F) + "с").replace(".0", "") : "Конец ивента!");

         if (ProjectionUtil.canSee(event.vec)) {
            List<String> list = new ArrayList(Collections.singletonList(event.name + distance));
            if (event.owner != null) {
               String var10001 = String.valueOf(Formatting.GOLD);
               list.add("Призван: " + var10001 + event.owner);
            }

            list.add(time);
            if (event.lvl != null) {
               list.add(event.lvl);
            }

            this.draw(matrix, Fonts.getSize(14), list, vec3d);
         }

      });
   }
   private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int color) {
      Box box = (new Box(playerPos.up())).offset(smooth).expand((double)size);
      boolean inBox = mc.world.getPlayers().stream().anyMatch((player) -> player != mc.player && box.intersects(player.getBoundingBox()) && !FriendUtils.isFriend((Entity)player));
      Render3DUtil.drawBox(box, inBox ? ColorUtil.getFriendColor() : color, 3.0F, true, true, true);
   }

   private void drawItemRadius(float distance, int color) {
      if (this.validDistance(distance)) {
         color = ColorUtil.getFriendColor();
      }

      float playerHalfWidth = ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getWidth() / 2.0F;
      Render3DUtil.drawRadius(MathUtil.interpolate(mc.player).add((double)playerHalfWidth, (double)0.0F, (double)playerHalfWidth), distance, 2.0F, color);
   }

   private void draw(org.joml.Matrix3x2fStack matrix, FontRenderer font, List<String> list, Vec3d vec3d) {
      for(int i = 0; i < list.size(); ++i) {
         String string = (String)list.get(i);
         float width = font.getStringWidth(string);
         float posX = (float)(vec3d.x - (double)(width / 2.0F));
         float offsetY = (float)(i * 10);
         blur.render(ShapeProperties.create(matrix, (double)(posX - 2.0F), vec3d.y - (double)2.0F + (double)offsetY, (double)(width + 4.0F), (double)10.0F).softness(3.0F).round(this.getRound(font, list, i, width)).color(ColorUtil.HALF_BLACK).build());
         font.drawString(matrix, string, (double)posX, vec3d.y + (double)1.0F + (double)offsetY, ColorUtil.getText());
      }

   }

   public void drawSidePlast(BlockPos blockPos, Vec3d smooth, int color, int i, boolean ff) {
      Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
      float width = 2.0F;
      int quadColor = ColorUtil.multAlpha(color, 0.15F);
      this.drawHorizontalLines(vec3d, color, width, i, ff);
      this.drawHorizontalLines(vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, i, ff);
      this.drawVerticalLines(vec3d, color, width, i, ff);
      this.drawHorizontalQuads(vec3d, quadColor, i, ff);
      this.drawHorizontalQuads(vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), quadColor, i, ff);
      this.drawVerticalQuads(vec3d, quadColor, i, ff);
   }

   private void drawHorizontalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
      float x = ff ? (float)i : (float)(-i);
      Vec3d var8;
      Render3DUtil.drawLine(vec3d, var8 = vec3d.add((double)x, (double)0.0F, (double)0.0F), color, width, true);

      for(int f = 0; f < 4; ++f) {
         Render3DUtil.drawLine(var8, vec3d = var8.add((double)0.0F, (double)0.0F, (double)i), color, width, true);
         Render3DUtil.drawLine(vec3d, var8 = vec3d.add((double)x, (double)0.0F, (double)0.0F), color, width, true);
      }

      Render3DUtil.drawLine(var8, vec3d = var8.add((double)0.0F, (double)0.0F, (double)i), color, width, true);
      Vec3d var11;
      Render3DUtil.drawLine(vec3d, var11 = vec3d.add((double)(x * -2.0F), (double)0.0F, (double)0.0F), color, width, true);

      for(int f = 0; f < 3; ++f) {
         Render3DUtil.drawLine(var11, vec3d = var11.add((double)0.0F, (double)0.0F, (double)(i * -1)), color, width, true);
         Render3DUtil.drawLine(vec3d, var11 = vec3d.add((double)(x * -1.0F), (double)0.0F, (double)0.0F), color, width, true);
      }

      Render3DUtil.drawLine(var11, var11.add((double)0.0F, (double)0.0F, (double)(i * -2)), color, width, true);
   }

   private void drawVerticalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
      float x = ff ? (float)i : (float)(-i);
      Render3DUtil.drawLine(vec3d, vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, true);
      Vec3d var8;
      Render3DUtil.drawLine(var8 = vec3d.add((double)x, (double)0.0F, (double)0.0F), var8.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, true);

      for(int f = 0; f < 4; ++f) {
         Render3DUtil.drawLine(var8 = var8.add((double)x, (double)0.0F, (double)i), var8.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, true);
      }

      Render3DUtil.drawLine(vec3d = var8.add((double)0.0F, (double)0.0F, (double)i), vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, true);
      Vec3d var10;
      Render3DUtil.drawLine(var10 = vec3d.add((double)(x * -2.0F), (double)0.0F, (double)0.0F), var10.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, true);

      for(int f = 0; f < 3; ++f) {
         Render3DUtil.drawLine(var10 = var10.add((double)(x * -1.0F), (double)0.0F, (double)(i * -1)), var10.add((double)0.0F, (double)5.0F, (double)0.0F), color, width, true);
      }

   }

   private void drawHorizontalQuads(Vec3d vec3d, int color, int i, boolean ff) {
      vec3d = vec3d.add((double)0.0F, 0.001, (double)0.0F);
      float x = ff ? (float)i : (float)(-i);
      Render3DUtil.drawQuad(vec3d, vec3d.add((double)x, (double)0.0F, (double)0.0F), vec3d.add((double)x, (double)0.0F, (double)(i * 2)), vec3d.add((double)0.0F, (double)0.0F, (double)(i * 2)), color, true);

      for(int f = 0; f < 3; ++f) {
         Render3DUtil.drawQuad(vec3d = vec3d.add((double)x, (double)0.0F, (double)i), vec3d.add((double)x, (double)0.0F, (double)0.0F), vec3d.add((double)x, (double)0.0F, (double)(i * 2)), vec3d.add((double)0.0F, (double)0.0F, (double)(i * 2)), color, true);
      }

      Vec3d var8;
      Render3DUtil.drawQuad(var8 = vec3d.add((double)x, (double)0.0F, (double)i), var8.add((double)x, (double)0.0F, (double)0.0F), var8.add((double)x, (double)0.0F, (double)i), var8.add((double)0.0F, (double)0.0F, (double)i), color, true);
   }

   private void drawVerticalQuads(Vec3d vec3d, int color, int i, boolean ff) {
      float x = ff ? (float)i : (float)(-i);
      Render3DUtil.drawQuad(vec3d, vec3d.add((double)x, (double)0.0F, (double)0.0F), vec3d.add((double)x, (double)5.0F, (double)0.0F), vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);

      for(int f = 0; f < 4; ++f) {
         Vec3d var7;
         Render3DUtil.drawQuad(var7 = vec3d.add((double)x, (double)0.0F, (double)0.0F), var7.add((double)0.0F, (double)0.0F, (double)i), var7.add((double)0.0F, (double)5.0F, (double)i), var7.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
         Render3DUtil.drawQuad(vec3d = var7.add((double)0.0F, (double)0.0F, (double)i), vec3d.add((double)x, (double)0.0F, (double)0.0F), vec3d.add((double)x, (double)5.0F, (double)0.0F), vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
      }

      Vec3d var8;
      Render3DUtil.drawQuad(var8 = vec3d.add((double)x, (double)0.0F, (double)0.0F), var8.add((double)0.0F, (double)0.0F, (double)i), var8.add((double)0.0F, (double)5.0F, (double)i), var8.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
      Render3DUtil.drawQuad(vec3d = var8.add((double)0.0F, (double)0.0F, (double)i), vec3d.add((double)(x * -2.0F), (double)0.0F, (double)0.0F), vec3d.add((double)(x * -2.0F), (double)5.0F, (double)0.0F), vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
      vec3d = vec3d.add((double)(x * -1.0F), (double)0.0F, (double)0.0F);

      for(int f = 0; f < 3; ++f) {
         Vec3d var11;
         Render3DUtil.drawQuad(var11 = vec3d.add((double)(x * -1.0F), (double)0.0F, (double)0.0F), var11.add((double)0.0F, (double)0.0F, (double)(i * -1)), var11.add((double)0.0F, (double)5.0F, (double)(i * -1)), var11.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
         Render3DUtil.drawQuad(vec3d = var11.add((double)0.0F, (double)0.0F, (double)(i * -1)), vec3d.add((double)(x * -1.0F), (double)0.0F, (double)0.0F), vec3d.add((double)(x * -1.0F), (double)5.0F, (double)0.0F), vec3d.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
      }

      Vec3d var12;
      Render3DUtil.drawQuad(var12 = vec3d.add((double)(x * -1.0F), (double)0.0F, (double)0.0F), var12.add((double)0.0F, (double)0.0F, (double)(i * -2)), var12.add((double)0.0F, (double)5.0F, (double)(i * -2)), var12.add((double)0.0F, (double)5.0F, (double)0.0F), color, true);
   }

   private void addEvent(String name, String lvl, String owner, Vec3d vec3d, String world, int timeOpen, int timeLoot) {
      if (this.serverEvents.stream().noneMatch((server) -> server.vec.equals(vec3d))) {
         long open = System.currentTimeMillis() + (long)timeOpen * 1000L;
         long loot = open + (long)timeLoot * 1000L;
         this.serverEvents.add(new ServerEvent(name, lvl, owner, vec3d, world, ServerUtil.getAnarchy(), (double)open, (double)loot));
      }

   }

   private void addStructure(Item item, Vec3d vec, double time, float boxSize) {
      if (this.structures.stream().noneMatch((str) -> str.vec.equals(vec))) {
         this.structures.add(new Structure(item, vec, ServerUtil.getWorldType(), ServerUtil.getAnarchy(), (double)System.currentTimeMillis() + time, boxSize));
      }

   }

   private Vector4f getRound(FontRenderer font, List<String> list, int i, float width) {
      if (i == 0) {
         float next = font.getStringWidth((String)list.get(i + 1));
         return next >= width ? new Vector4f(2.0F, 0.0F, 2.0F, 0.0F) : new Vector4f(2.0F);
      } else if (i == list.size() - 1) {
         float prev = font.getStringWidth((String)list.get(i - 1));
         return prev >= width ? new Vector4f(0.0F, 2.0F, 0.0F, 2.0F) : new Vector4f(2.0F);
      } else {
         float prev = font.getStringWidth((String)list.get(i - 1));
         float next = font.getStringWidth((String)list.get(i + 1));
         return prev >= width ? (next >= width ? new Vector4f() : new Vector4f(0.0F, 2.0F, 0.0F, 2.0F)) : new Vector4f(2.0F);
      }
   }

   private boolean validDistance(float dist) {
      return dist == 0.0F || mc.world.getPlayers().stream().anyMatch((p) -> p != mc.player && !FriendUtils.isFriend((Entity)p) && mc.player.distanceTo(p) <= dist);
   }

   private boolean isTrap(BlockPos center) {
      int inconsistencies = 0;

      for(BlockPos pos : PlayerIntersectionUtil.getCube(center, 2.0F)) {
         if (pos.toCenterPos().distanceTo(center.toCenterPos()) < (double)2.0F) {
            BlockState state = (BlockState)this.blockStateMap.get(pos);
            if (state != null && !state.isAir()) {
               ++inconsistencies;
            }
         } else if (!pos.equals(center.up(2).north().east()) && !pos.equals(center.up(2).north().west()) && !pos.equals(center.up(2).south().east()) && !pos.equals(center.up(2).south().west())) {
            BlockState state = (BlockState)this.blockStateMap.get(pos);
            if (state == null || state.isAir()) {
               ++inconsistencies;
            }
         }

         if (inconsistencies > 1) {
            return false;
         }
      }

      return true;
   }

   private boolean isBigTrap(BlockPos center) {
      int inconsistencies = 0;

      for(BlockPos pos : PlayerIntersectionUtil.getCube(center, 3.0F)) {
         if (Math.abs(pos.getX() - center.getX()) <= 2 && Math.abs(pos.getY() - center.getY()) <= 2 && Math.abs(pos.getZ() - center.getZ()) <= 2) {
            BlockState state = (BlockState)this.blockStateMap.get(pos);
            if (state != null && !state.isAir()) {
               ++inconsistencies;
            }
         } else if (!pos.equals(center.up(3))) {
            BlockState state = (BlockState)this.blockStateMap.get(pos);
            if (state == null || state.isAir()) {
               ++inconsistencies;
            }
         }

         if (inconsistencies > 1) {
            return false;
         }
      }

      return true;
   }

   public KeyBind createBind(Item item, BindSetting setting) {
      return this.createBind(item, setting, 0.0F);
   }

   public KeyBind createBind(Item item, BindSetting setting, float distance) {
      return new KeyBind(item, setting, distance, (new DecelerateAnimation()).setMs(200).setValue((double)1.0F).setDirection(Direction.BACKWARDS));
   }

   public static record KeyBind(Item item, BindSetting setting, float distance, Animation anim) {
   }

   public static record Structure(Item item, Vec3d vec, String world, int anarchy, double time, float boxSize) {
   }

   public static record ServerEvent(String name, String lvl, String owner, Vec3d vec, String world, int anarchy, double timeOpen, double timeEnd) {
   }
}
