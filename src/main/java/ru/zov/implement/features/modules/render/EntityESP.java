package ru.zov.implement.features.modules.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Vector4d;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.repository.FriendUtils;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.EntityStory;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.ProjectionUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.render.DrawEvent;
import ru.zov.implement.features.modules.combat.AntiBot;

public class EntityESP extends Module {
   private static final EquipmentSlot[] ARMOR_SLOTS = {
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };
   private final Identifier TEXTURE = Identifier.of("textures/container.png");
   private final ValueSetting sizeSetting = (new ValueSetting("Tag Size", "Tags size")).setValue(13.0F).range(10, 20);
   public final MultiSelectSetting entityType = (new MultiSelectSetting("Entity Type", "Entity that will be displayed")).value("Player", "Item", "TNT").selected("Player", "Item", "TNT");
   private final MultiSelectSetting playerSetting = (new MultiSelectSetting("Player Settings", "Settings for players")).value("Flat Box", "Armor", "Prefix", "Hand Items", "Effects").visible(() -> this.entityType.isSelected("Player")).selected("Armor", "Prefix");
   private final MultiSelectSetting tntSetting = (new MultiSelectSetting("TNT Settings", "Settings for tnts")).value("Time").visible(() -> this.entityType.isSelected("TNT")).selected("Time");

   public static EntityESP getInstance() {
      return (EntityESP)Instance.get(EntityESP.class);
   }

   public EntityESP() {
      super("EntityESP", "Entity ESP", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.sizeSetting, this.entityType, this.playerSetting, this.tntSetting});
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      DrawContext context = e.context();
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(this.sizeSetting.getInt(), Fonts.Type.DEFAULT);
      FontRenderer bigFont = Fonts.getSize(this.sizeSetting.getInt() + 2, Fonts.Type.DEFAULT);
      EntityStory.INSTANCE.getEntities().forEach((ce) -> MathUtil.multAlpha(ce.animation().getOutput().floatValue(), () -> {
            Entity var10000 = ce.entity();
            Objects.requireNonNull(var10000);
            Entity entity = var10000;
            if (entity instanceof PlayerEntity player) {
               if (!this.entityType.isSelected("Player")) {
                  return;
               }

               Vector4d vec4d = ProjectionUtil.getVector4D(player);
               float distance = (float)mc.getEntityRenderDispatcher().camera.getCameraPos().distanceTo(player.getBoundingBox().getCenter());
               boolean friend = FriendUtils.isFriend((Entity)player);
               int color = friend ? ColorUtil.getColor(0, 25, 0, 125) : ColorUtil.HALF_BLACK;
               if (distance > 1.0F && !ProjectionUtil.cantSee(vec4d)) {
                  if (this.playerSetting.isSelected("Flat Box")) {
                     this.drawFlatBox(friend, vec4d);
                  }

                  if (this.playerSetting.isSelected("Armor")) {
                     this.drawArmor(context, player, vec4d, color);
                  }

                  if (this.playerSetting.isSelected("Hand Items")) {
                     this.drawHands(matrix, player, font, vec4d, color);
                  }

                  if (this.playerSetting.isSelected("Effects")) {
                     this.drawEffects(context, player, font, vec4d, color);
                  }

                  this.drawText(matrix, this.getTextPlayer(player), ProjectionUtil.centerX(vec4d), vec4d.y - (double)3.0F, font, color);
               }
            } else if (entity instanceof ItemEntity item) {
               if (!this.entityType.isSelected("Item")) {
                  return;
               }

               Vector4d vec4d = ProjectionUtil.getVector4D(item);
               if (!ProjectionUtil.cantSee(vec4d)) {
                  ItemStack stack = item.getStack();
                  List<ItemStack> list = ((ContainerComponent)stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT)).stream().toList();
                  Text text = item.getStack().getName();
                  int count = stack.getCount();
                  if (count > 1) {
                     MutableText var22 = text.copy();
                     String var23 = String.valueOf(Formatting.RESET);
                     text = var22.append(var23 + " [" + String.valueOf(Formatting.RED) + count + String.valueOf(Formatting.GRAY) + "x" + String.valueOf(Formatting.RESET) + "]");
                  }

                  if (!list.isEmpty()) {
                     this.drawShulkerBox(context, stack, list, vec4d);
                  } else {
                     this.drawText(matrix, text, ProjectionUtil.centerX(vec4d), vec4d.y, text.getContent().toString().equals("empty") ? bigFont : font, ColorUtil.HALF_BLACK);
                  }
               }
            } else if (entity instanceof TntEntity tnt) {
               if (!this.entityType.isSelected("TNT")) {
                  return;
               }

               Vector4d vec4d = ProjectionUtil.getVector4D(tnt);
               if (!ProjectionUtil.cantSee(vec4d)) {
                  MutableText text = Text.empty().append(tnt.getStyledDisplayName());
                  if (this.tntSetting.isSelected("Time")) {
                     String var10001 = String.valueOf(Formatting.RED);
                     text.append(" [" + var10001 + MathUtil.round((double)(((float)tnt.getFuse() - tickCounter.getTickProgress(true)) / 20.0F), 0.1) + String.valueOf(Formatting.RESET) + "]");
                  }

                  this.drawText(matrix, text, ProjectionUtil.centerX(vec4d), vec4d.y, font, ColorUtil.getColor(20, 0, 0, 125));
               }
            }
         }));
   }

   private void drawFlatBox(boolean friend, Vector4d vec) {
      int client = friend ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
      int black = ColorUtil.HALF_BLACK;
      float posX = (float)vec.x;
      float posY = (float)vec.y;
      float endPosX = (float)vec.z;
      float endPosY = (float)vec.w;
      float size = (endPosX - posX) / 3.0F;
      Render2DUtil.drawQuad(posX - 1.0F, posY - 1.0F, size + 1.0F, 1.5F, black);
      Render2DUtil.drawQuad(posX - 1.0F, posY + 0.5F, 1.5F, size + 0.5F, black);
      Render2DUtil.drawQuad(posX - 1.0F, endPosY - size - 1.0F, 1.5F, size, black);
      Render2DUtil.drawQuad(posX - 1.0F, endPosY - 1.0F, size + 1.0F, 1.5F, black);
      Render2DUtil.drawQuad(endPosX - size + 0.5F, posY - 1.0F, size + 1.0F, 1.5F, black);
      Render2DUtil.drawQuad(endPosX, posY + 0.5F, 1.5F, size + 0.5F, black);
      Render2DUtil.drawQuad(endPosX, endPosY - size - 1.0F, 1.5F, size, black);
      Render2DUtil.drawQuad(endPosX - size + 0.5F, endPosY - 1.0F, size + 1.0F, 1.5F, black);
      Render2DUtil.drawQuad(posX - 0.5F, posY - 0.5F, size, 0.5F, client);
      Render2DUtil.drawQuad(posX - 0.5F, posY, 0.5F, size + 0.5F, client);
      Render2DUtil.drawQuad(posX - 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
      Render2DUtil.drawQuad(posX - 0.5F, endPosY - 0.5F, size, 0.5F, client);
      Render2DUtil.drawQuad(endPosX - size + 1.0F, posY - 0.5F, size, 0.5F, client);
      Render2DUtil.drawQuad(endPosX + 0.5F, posY, 0.5F, size + 0.5F, client);
      Render2DUtil.drawQuad(endPosX + 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
      Render2DUtil.drawQuad(endPosX - size + 1.0F, endPosY - 0.5F, size, 0.5F, client);
   }

   private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec, int color) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      List<ItemStack> items = new ArrayList();
      for(EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = player.getEquippedStack(slot);
         if (!stack.isEmpty()) {
            items.add(stack);
         }
      }
      float posX = (float)(ProjectionUtil.centerX(vec) - (double)items.size() * (double)5.5F);
      float posY = (float)(vec.y - (double)this.sizeSetting.getInt() / (double)1.5F - (double)15.0F);
      float padding = 0.5F;
      float offset = -11.0F;
      if (!items.isEmpty()) {
         matrix.pushMatrix();
         matrix.translate(posX, posY);
         blur.render(ShapeProperties.create(matrix, (double)(-padding), (double)(-padding), (double)((float)(items.size() * 11 - 1) + padding * 2.0F), (double)(10.0F + padding * 2.0F)).round(2.5F).color(color).build());
         matrix.translate(0.0F, 0.0F);

         for(ItemStack stack : items) {
            Render2DUtil.defaultDrawStack(context, stack, offset += 11.0F, 0.0F, false, false, 0.5F);
         }

         matrix.popMatrix();
      }

   }

   private void drawHands(org.joml.Matrix3x2fStack matrix, PlayerEntity player, FontRenderer font, Vector4d vec, int color) {
      double posY = vec.w;

      for(ItemStack stack : List.of(player.getMainHandStack(), player.getOffHandStack())) {
         if (!stack.isEmpty()) {
            MutableText text = Text.empty().append(stack.getName());
            if (stack.getCount() > 1) {
               String var10001 = String.valueOf(Formatting.RESET);
               text.append(var10001 + " [" + String.valueOf(Formatting.RED) + stack.getCount() + String.valueOf(Formatting.GRAY) + "x" + String.valueOf(Formatting.RESET) + "]");
            }

            posY += (double)(font.getStringHeight((Text)text) / 2.0F + 3.0F);
            this.drawText(matrix, text, ProjectionUtil.centerX(vec), posY, font, color);
         }
      }

   }

   private void drawEffects(DrawContext context, PlayerEntity player, FontRenderer font, Vector4d vec, int color) {
      int offset = 0;
      for(StatusEffectInstance effect : player.getStatusEffects()) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(effect.getEffectType()), (int)vec.x + offset, (int)vec.y, 8, 8);
         offset += 9;
      }

   }

   private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      int width = 176;
      int height = 67;
      int color = ColorUtil.multBright(ColorUtil.replAlpha(((BlockItem)itemStack.getItem()).getBlock().getDefaultMapColor().color, 1.0F), 1.0F);
      matrix.pushMatrix();
      matrix.translate((float)(ProjectionUtil.centerX(vec) - (double)width / (double)4.0F), (float)(vec.w + (double)2.0F));
      matrix.scale(0.5F, 0.5F);
      context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height, color);
      int posX = 7;
      int posY = 6;

      for(ItemStack stack : stacks.stream().toList()) {
         Render2DUtil.defaultDrawStack(context, stack, (float)posX, (float)posY, false, true, 1.0F);
         posX += 18;
         if (posX >= 165) {
            posY += 18;
            posX = 7;
         }
      }

      matrix.popMatrix();
   }

   public void drawText(org.joml.Matrix3x2fStack matrix, Text text, double startX, double startY, FontRenderer font, int color) {
      int paddingX = 2;
      float paddingY = 0.75F;
      float height = (float)font.getFont().getSize() / 1.5F;
      float width = font.getStringWidth(text);
      float posX = (float)(startX - (double)(width / 2.0F));
      float posY = (float)startY - height;
      blur.render(ShapeProperties.create(matrix, (double)(posX - (float)paddingX), (double)(posY - paddingY), (double)(width + (float)(paddingX * 2)), (double)(height + paddingY * 2.0F)).round(height / 4.0F).color(color).build());
      font.drawText(matrix, text, (double)posX, (double)(posY + 3.0F));
   }

   private MutableText getTextPlayer(PlayerEntity player) {
      float health = PlayerIntersectionUtil.getHealth(player);
      Item offHandItem = player.getOffHandStack().getItem();
      MutableText text = Text.empty();
      if (AntiBot.getInstance().isBot(player)) {
         String var10001 = String.valueOf(Formatting.DARK_RED);
         text.append("[" + var10001 + "BOT" + String.valueOf(Formatting.RESET) + "] ");
      }

      if (this.playerSetting.isSelected("Prefix")) {
         text.append(player.getDisplayName());
      } else {
         text.append(player.getName());
      }

      if (offHandItem.equals(Items.PLAYER_HEAD) || offHandItem.equals(Items.TOTEM_OF_UNDYING)) {
         String var5 = String.valueOf(Formatting.RESET);
         text.append(var5 + this.getSphere(player.getOffHandStack()));
      }

      if (health >= 0.0F && health <= player.getMaxHealth() + player.getMaxAbsorption()) {
         String var6 = String.valueOf(Formatting.RESET);
         text.append(var6 + " [" + String.valueOf(Formatting.RED) + PlayerIntersectionUtil.getHealthString(player) + String.valueOf(Formatting.RESET) + "]");
      }

      return text;
   }

   private String getSphere(ItemStack stack) {
      NbtComponent component = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (ServerUtil.isFunTime() && component != null) {
         NbtCompound compound = component.copyNbt();
         if (compound.getInt("tslevel", 0) != 0) {
            String var10000 = String.valueOf(Formatting.GOLD);
            return " [" + var10000 + compound.getString("don-item", "").replace("sphere-", "").toUpperCase() + String.valueOf(Formatting.RESET) + "]";
         }
      }

      return "";
   }
}
