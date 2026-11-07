package ru.zov.implement.features.modules.misc.auction;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.core.Main;

public interface AuctionItems extends QuickImports {
   List<ItemStack> pvpItems = List.of(stackBuilder(new ItemStack(Items.NETHERITE_SWORD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовый меч")).build()), stackBuilder(new ItemStack(Items.NETHERITE_PICKAXE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовая кирка")).build()), stackBuilder(new ItemStack(Items.NETHERITE_SHOVEL), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовая кирка")).build()), stackBuilder(new ItemStack(Items.TRIDENT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("трезубец")).build()), stackBuilder(new ItemStack(Items.CROSSBOW), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("арбалет")).build()), stackBuilder(new ItemStack(Items.BOW), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("лук")).build()), stackBuilder(new ItemStack(Items.NETHERITE_HELMET), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовый шлем")).build()), stackBuilder(new ItemStack(Items.NETHERITE_CHESTPLATE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовый нагрудник")).build()), stackBuilder(new ItemStack(Items.NETHERITE_LEGGINGS), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовые поножи")).build()), stackBuilder(new ItemStack(Items.NETHERITE_BOOTS), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("незеритовые ботинки")).build()), stackBuilder(new ItemStack(Items.ELYTRA), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("элитры")).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("тотем бессмертия")).build()), stackBuilder(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зачарованное золотое яблоко")).build()));
   List<ItemStack> consumablesItems = List.of(stackBuilder(new ItemStack(Items.ENDER_PEARL, 16), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("эндер-жемчуг")).build()), stackBuilder(new ItemStack(Items.GOLDEN_APPLE, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("золотое яблоко")).build()), stackBuilder(new ItemStack(Items.GOLDEN_CARROT, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("золотая морковь")).build()), stackBuilder(new ItemStack(Items.CHORUS_FRUIT, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("плод хоруса")).build()), stackBuilder(new ItemStack(Items.POTION, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("исцеление")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Зелье Исцеления").setStyle(Style.EMPTY.withItalic(false))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_HEALING)).add(DataComponentTypes.MAX_STACK_SIZE, 64).build()), stackBuilder(new ItemStack(Items.EXPERIENCE_BOTTLE, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("пузырёк опыта")).build()), stackBuilder(new ItemStack(Items.FIREWORK_ROCKET, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("фейерверк")).build()));
   List<ItemStack> holyItems = Holder.holyItems;
   List<ItemStack> holyConsumables = Holder.holyConsumables;
   List<ItemStack> holyAutoBuyItems = Holder.holyAutoBuyItems;
   List<ItemStack> funtimeItems = Holder.funtimeItems;

   default List<ItemStack> getAutoBuyItems() {
      List<ItemStack> list = new ArrayList(pvpItems);
      switch (ServerUtil.server) {
         case "FunTime":
            list.addAll(funtimeItems);
            break;
         case "HolyWorld":
            list.addAll(holyItems);
            list.addAll(holyAutoBuyItems);
      }

      while(list.size() < 54) {
         list.add(ItemStack.EMPTY);
      }

      return list;
   }

   default List<ItemStack> getCollectorItems() {
      List<ItemStack> list = new ArrayList(consumablesItems);
      list.addAll(pvpItems);
      switch (ServerUtil.server) {
         case "FunTime":
            list.addAll(funtimeItems);
            break;
         case "HolyWorld":
            list.addAll(holyItems);
            list.addAll(holyConsumables);
      }

      while(list.size() < 54) {
         list.add(ItemStack.EMPTY);
      }

      return list;
   }

   default List<ItemStack> getEnchantedBooks(ItemStack stack) {
      List<ItemStack> list = new ArrayList();
      Item item = stack.getItem();
      Objects.requireNonNull(item);
      if (stack.isIn(ItemTags.SWORDS)) {
         list.addAll(booksBuilder((RegistryKey)Enchantments.SHARPNESS, 7));
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.FIRE_ASPECT, 2));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
         switch (ServerUtil.server) {
            case "HolyWorld":
               list.addAll(booksBuilder((RegistryKey)Enchantments.LOOTING, 6));
               list.addAll(booksBuilder("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439", 2));
               list.addAll(booksBuilder("\u0420\u0430\u0437\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c", 2));
               list.addAll(booksBuilder("\u0411\u043e\u0433\u0430\u0447", 1));
               break;
            case "FunTime":
               list.addAll(booksBuilder((RegistryKey)Enchantments.LOOTING, 5));
               list.addAll(booksBuilder("\u042f\u0434", 3));
               list.addAll(booksBuilder("\u041e\u043f\u044b\u0442\u043d\u044b\u0439", 3));
               list.addAll(booksBuilder("\u0414\u0435\u0442\u0435\u043a\u0446\u0438\u044f", 3));
               list.addAll(booksBuilder("\u041e\u043a\u0438\u0441\u043b\u0435\u043d\u0438\u0435", 2));
               list.addAll(booksBuilder("\u0412\u0430\u043c\u043f\u0438\u0440\u0438\u0437\u043c", 2));
         }
      } else if (stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.SHOVELS)) {
         list.addAll(booksBuilder((RegistryKey)Enchantments.EFFICIENCY, 10));
         list.addAll(booksBuilder((RegistryKey)Enchantments.FORTUNE, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
         if (ServerUtil.isHolyWorld()) {
            list.addAll(booksBuilder("\u041e\u043f\u044b\u0442\u043d\u044b\u0439", 3));
            list.addAll(booksBuilder("\u0411\u0443\u0440", 2));
            list.addAll(booksBuilder("\u041d\u0435\u0440\u0430\u0437\u0440\u0443\u0448\u0438\u043c\u043e\u0441\u0442\u044c", 1));
            list.addAll(booksBuilder("\u0410\u0432\u0442\u043e\u043f\u043b\u0430\u0432\u043a\u0430", 1));
            list.addAll(booksBuilder("\u041c\u0430\u0433\u043d\u0435\u0442\u0438\u0437\u043c", 1));
            list.addAll(booksBuilder("\u0424\u0438\u043b\u044c\u0442\u0440", 1));
         }
      } else if (stack.isIn(ItemTags.HEAD_ARMOR) || stack.isIn(ItemTags.CHEST_ARMOR) || stack.isIn(ItemTags.LEG_ARMOR) || stack.isIn(ItemTags.FOOT_ARMOR)) {
         EquipmentSlot slot = stack.getComponents().get(DataComponentTypes.EQUIPPABLE).slot();
         list.addAll(booksBuilder((RegistryKey)Enchantments.PROTECTION, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.BLAST_PROTECTION, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.PROJECTILE_PROTECTION, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.FIRE_PROTECTION, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.THORNS, 3));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
         if (ServerUtil.isHolyWorld()) {
            list.addAll(booksBuilder("\u041d\u0435\u043f\u0440\u043e\u0431\u0438\u0432\u0430\u0435\u043c\u044b\u0439", 2));
         }

         switch (slot) {
            case FEET:
               list.addAll(booksBuilder((RegistryKey)Enchantments.DEPTH_STRIDER, 3));
               break;
            case HEAD:
               list.addAll(booksBuilder((RegistryKey)Enchantments.AQUA_AFFINITY, 1));
               list.addAll(booksBuilder((RegistryKey)Enchantments.RESPIRATION, 3));
         }
      } else if (item instanceof TridentItem) {
         list.addAll(booksBuilder((RegistryKey)Enchantments.SHARPNESS, 7));
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.LOOTING, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.FIRE_ASPECT, 2));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
         list.addAll(booksBuilder((RegistryKey)Enchantments.LOYALTY, 1));
         list.addAll(booksBuilder((RegistryKey)Enchantments.CHANNELING, 1));
         switch (ServerUtil.server) {
            case "HolyWorld":
               list.addAll(booksBuilder("\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439", 2));
               list.addAll(booksBuilder("\u0420\u0430\u0437\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c", 2));
               list.addAll(booksBuilder("\u0411\u043e\u0433\u0430\u0447", 1));
               list.addAll(booksBuilder("\u0421\u0430\u043c\u043e\u043d\u0430\u0432\u043e\u0434\u043a\u0430", 5));
               break;
            case "FunTime":
               list.addAll(booksBuilder("\u041f\u043e\u0434\u0440\u044b\u0432\u043d\u0438\u043a", 1));
               list.addAll(booksBuilder("\u0421\u043a\u0430\u0443\u0442", 3));
               list.addAll(booksBuilder("\u0421\u0442\u0443\u043f\u043e\u0440", 3));
               list.addAll(booksBuilder("\u041f\u0440\u0438\u0442\u044f\u0436\u0435\u043d\u0438\u0435", 1));
               list.addAll(booksBuilder("\u042f\u0434", 3));
               list.addAll(booksBuilder("\u041e\u043f\u044b\u0442\u043d\u044b\u0439", 3));
               list.addAll(booksBuilder("\u0414\u0435\u0442\u0435\u043a\u0446\u0438\u044f", 3));
               list.addAll(booksBuilder("\u041e\u043a\u0438\u0441\u043b\u0435\u043d\u0438\u0435", 2));
               list.addAll(booksBuilder("\u0412\u0430\u043c\u043f\u0438\u0440\u0438\u0437\u043c", 2));
         }
      } else if (item instanceof CrossbowItem) {
         list.addAll(booksBuilder((RegistryKey)Enchantments.MULTISHOT, 1));
         list.addAll(booksBuilder((RegistryKey)Enchantments.PIERCING, 1));
         list.addAll(booksBuilder((RegistryKey)Enchantments.QUICK_CHARGE, 3));
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 3));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
         if (ServerUtil.isHolyWorld()) {
            list.addAll(booksBuilder("\u041e\u0433\u043b\u0443\u0448\u0435\u043d\u0438\u0435", 2));
         }
      } else if (item instanceof BowItem) {
         list.addAll(booksBuilder((RegistryKey)Enchantments.POWER, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 3));
         list.addAll(booksBuilder((RegistryKey)Enchantments.PUNCH, 2));
         list.addAll(booksBuilder((RegistryKey)Enchantments.FLAME, 1));
         list.addAll(booksBuilder((RegistryKey)Enchantments.INFINITY, 1));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
         switch (ServerUtil.server) {
            case "HolyWorld":
               list.addAll(booksBuilder("\u041e\u0433\u043b\u0443\u0448\u0435\u043d\u0438\u0435", 2));
               break;
            case "FunTime":
               list.addAll(booksBuilder("\u041f\u043e\u0434\u0440\u044b\u0432\u043d\u0438\u043a", 1));
               list.addAll(booksBuilder("\u0421\u043d\u0430\u0439\u043f\u0435\u0440", 2));
         }
      } else if (item.equals(Items.ELYTRA)) {
         list.addAll(booksBuilder((RegistryKey)Enchantments.UNBREAKING, 5));
         list.addAll(booksBuilder((RegistryKey)Enchantments.MENDING, 1));
      }

      while(list.size() < 54) {
         list.add(ItemStack.EMPTY);
      }

      return list;
   }

   static ItemStack stackBuilder(ItemStack stack, ComponentMap map) {
      stack.applyComponentsFrom(map);
      setPrice(stack, 10);
      return stack;
   }

   static void setPrice(ItemStack stack, int price) {
      List<Text> list = new ArrayList(((LoreComponent)stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)).lines());
      if (Main.getInstance().isInitialized()) {
         list.remove(list.getLast());
      }

      String var10001 = String.valueOf(Formatting.GRAY);
      list.add(Text.literal(var10001 + "Цена: " + String.valueOf(Formatting.GREEN) + MathHelper.clamp(price, 0, 500000000) + String.valueOf(Formatting.GRAY) + "$").setStyle(Style.EMPTY.withItalic(false)));
      stack.set(DataComponentTypes.LORE, new LoreComponent(list));
   }

   static List<ItemStack> booksBuilder(String text, int maxLvl) {
      return IntStream.range(0, maxLvl).mapToObj((i) -> enchantedBookBuilder(Text.literal(text), maxLvl - i)).toList();
   }

   static List<ItemStack> booksBuilder(RegistryKey<Enchantment> key, int maxLvl) {
      return IntStream.range(0, maxLvl).mapToObj((i) -> enchantedBookBuilder(Text.translatable("enchantment.minecraft." + key.getValue().getPath()), maxLvl - i)).toList();
   }

   static ItemStack enchantedBookBuilder(Text text, int lvl) {
      ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
      stack.applyComponentsFrom(ComponentMap.builder().add(DataComponentTypes.STORED_ENCHANTMENTS, enchantedBuilder(text, lvl)).build());
      return stack;
   }

   static RegistryEntry<Enchantment> enchantBuilder(Text text) {
      return RegistryEntry.of(new Enchantment(text, Enchantment.definition(RegistryEntryList.empty(), 0, 1, new Enchantment.Cost(0, 0), new Enchantment.Cost(0, 0), 1, new AttributeModifierSlot[0]), RegistryEntryList.of(new RegistryEntry[0]), ComponentMap.builder().build()));
   }

   static ItemEnchantmentsComponent enchantedBuilder(Text text, int lvl) {
      ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
      builder.add(enchantBuilder(text), lvl);
      return builder.build();
   }

   static ProfileComponent profileBuilder(String texture) {
      com.google.common.collect.Multimap<String, Property> map = com.google.common.collect.LinkedHashMultimap.create();
      map.put("textures", new Property("textures", texture, (String)null));
      com.mojang.authlib.properties.PropertyMap properties = new com.mojang.authlib.properties.PropertyMap(map);
      GameProfile profile = new GameProfile(UUID.randomUUID(), "", properties);
      return ProfileComponent.ofStatic(profile);
   }

   static MutableText funtimeSphere(String name) {
      return Text.literal("[★]").setStyle(Style.EMPTY.withItalic(false)).withColor(-3407872).append(Text.literal(" " + name).setStyle(Style.EMPTY.withItalic(false).withColor(-28672)));
   }

   static MutableText obfuscatedSymbol(String str) {
      return Text.literal(str).setStyle(Style.EMPTY.withBold(true).withItalic(false).withStrikethrough(false).withUnderline(false).withObfuscated(true));
   }

   static MutableText holyEnchant(String text) {
      String var10000 = String.valueOf(Formatting.GRAY);
      MutableText var1 = Text.literal(var10000 + "• " + String.valueOf(Formatting.RESET)).setStyle(Style.EMPTY.withItalic(false));
      String var10001 = String.valueOf(Formatting.WHITE);
      return var1.append(var10001 + text);
   }

   static AttributeModifiersComponent.Entry attributeBuilder(RegistryEntry<EntityAttribute> attributes, double value, EntityAttributeModifier.Operation operation) {
      return attributeBuilder(attributes, value, operation, AttributeModifierSlot.OFFHAND);
   }

   static AttributeModifiersComponent.Entry attributeBuilder(RegistryEntry<EntityAttribute> attributes, double value, EntityAttributeModifier.Operation operation, AttributeModifierSlot slot) {
      return new AttributeModifiersComponent.Entry(attributes, new EntityAttributeModifier(Identifier.of("minecraft:d5675201-8223-4192-b8ba-01b6f877c367"), value, operation), slot);
   }

   static NbtComponent itemBuilder(String message) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("message", message);
      return NbtComponent.of(nbtCompound);
   }

   static NbtComponent itemBuilder(String message, String id) {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("message", message);
      nbtCompound.putString("id", id);
      return NbtComponent.of(nbtCompound);
   }

   static int getPrice(ItemStack stack) {
      return (Integer)((LoreComponent)stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)).lines().stream().map(Text::getString).filter((s) -> s.contains("§7Цена: §a")).findFirst().map((string) -> Integer.parseInt(StringUtils.substringBetween(string, "§7Цена: §a", "§7$"))).orElse(-1);
   }

   static void updateScreenSlots(ScreenHandler screenHandler, List<ItemStack> stacks) {
      screenHandler.updateSlotStacks(screenHandler.getRevision(), stacks, screenHandler.getCursorStack());
   }

   static String getMessage(ItemStack stack) {
      NbtComponent component = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (component != null) {
         NbtCompound compound = component.copyNbt();
         return compound.getString("message", "");
      }

      return "";
   }

   final class Holder {
      static final List<ItemStack> holyItems;
      static final List<ItemStack> holyConsumables;
      static final List<ItemStack> holyAutoBuyItems;
      static final List<ItemStack> funtimeItems;

   static {
      ItemStack[] var10000 = new ItemStack[]{stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера цербера", "itemServiceId:{name:\"сфера_цербера\"}")).add(DataComponentTypes.PROFILE, profileBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0=")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера Цербера").setStyle(Style.EMPTY.withItalic(false).withBold(true)).withColor(-7536640)).add(DataComponentTypes.LORE, new LoreComponent(List.of(holyEnchant("Урон V"), holyEnchant("Спешка I")))).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера флеша", "itemServiceId:{name:\"сфера_флеша\"}")).add(DataComponentTypes.PROFILE, profileBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc0MDBlYTE5ZGJkODRmNzVjMzlhZDY4MjNhYzRlZjc4NmYzOWY0OGZjNmY4NDYwMjM2NmFjMjliODM3NDIyIn19fQ==")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера Флеша").setStyle(Style.EMPTY.withItalic(false).withBold(true)).withColor(-7291964)).add(DataComponentTypes.LORE, new LoreComponent(List.of(holyEnchant("Скорость III"), holyEnchant("Броня I")))).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера арморталити", "itemServiceId:{name:\"сфера_арморталити\"}")).add(DataComponentTypes.PROFILE, profileBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE2MmI5ZGU2YTI2Yjg2ODY5Y2EyMmVhNDBmMWJkZTgwYTA0MzBhNTQ1NDdiZWNjZThmZGE4NzA3Nzc3MjU4ZiJ9fX0=")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ").setStyle(Style.EMPTY.withItalic(false).withBold(true)).withColor(-13483936)).add(DataComponentTypes.LORE, new LoreComponent(List.of(holyEnchant("Макс здоровье II"), holyEnchant("Броня II"), holyEnchant("Урон II")))).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера имморталити", "itemServiceId:{name:\"сфера_имморталити\"}")).add(DataComponentTypes.PROFILE, profileBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Сфера ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ").setStyle(Style.EMPTY.withItalic(false)).withColor(-8912670)).add(DataComponentTypes.LORE, new LoreComponent(List.of(holyEnchant("Скорость II"), holyEnchant("Урон III")))).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман этернити", "itemServiceId:{name:\"сфера_eternity\"")).add(DataComponentTypes.CUSTOM_NAME, obfuscatedSymbol("-").append(Text.literal(" Талисман ᴇᴛᴇʀɴɪᴛʏ ").setStyle(Style.EMPTY.withObfuscated(false).withBold(true))).append(obfuscatedSymbol("-")).withColor(-3538826)).add(DataComponentTypes.LORE, new LoreComponent(List.of(holyEnchant("Броня II"), holyEnchant("Урон II"), holyEnchant("Скорость II")))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман инфинити", "itemServiceId:{name:\"талисман_infinity\"}")).add(DataComponentTypes.CUSTOM_NAME, obfuscatedSymbol("-").append(Text.literal(" Талисман ɪɴғɪɴɪᴛʏ ").setStyle(Style.EMPTY.withObfuscated(false).withBold(true))).append(obfuscatedSymbol("-")).withColor(-16730606)).add(DataComponentTypes.LORE, new LoreComponent(List.of(holyEnchant("Макс здоровье II"), holyEnchant("Урон II"), holyEnchant("Скорость II"), holyEnchant("Броня II")))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.GOLDEN_HELMET), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("шлем солнца", "kringeItems:{type:\"SunHelmet\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Шлем Солнца").setStyle(Style.EMPTY.withItalic(false).withColor(-14336))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.PRISMARINE_SHARD, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("взрывная трапка", "pyrotechnic-item:{name:\"EXPLOSIVE_TRAP\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Взрывная Трапка").setStyle(Style.EMPTY.withItalic(false).withColor(-4472591))).build()), stackBuilder(new ItemStack(Items.POPPED_CHORUS_FRUIT, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("трапка", "pyrotechnic-item:{name:\"ALTERNATIVE_TRAP\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Трапка").setStyle(Style.EMPTY.withItalic(false).withColor(-3576104))).build()), stackBuilder(new ItemStack(Items.NETHER_STAR, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("стан", "pyrotechnic-item:{name:\"STUN_STAR\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Стан").setStyle(Style.EMPTY.withItalic(false).withColor(-1052689))).build()), stackBuilder(new ItemStack(Items.FIRE_CHARGE, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("взрывная штучка", "kringeItems:{type:\"ExplosiveStuff\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Взрывная Штучка").setStyle(Style.EMPTY.withItalic(false).withColor(-3660032))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.SNOWBALL, 16), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("ком снега", "kringeItems:{type:\"SnowBall\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Ком Снега").setStyle(Style.EMPTY.withItalic(false).withColor(-16725027))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.EXPERIENCE_BOTTLE, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("пузырь опыта", "kringeItems:{type:\"ExpBottle\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Пузырь Опыта").setStyle(Style.EMPTY.withItalic(false).withColor(-16728554))).build()), null, null, null, null, null};
      ItemStack var10003 = new ItemStack(Items.PINK_SHULKER_BOX);
      ComponentMap.Builder var10004 = ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("рюкзак 1 уровень", "\"litebackpacks:backpack\":\"mini\""));
      ComponentType var10005 = DataComponentTypes.CUSTOM_NAME;
      String var10006 = String.valueOf(Formatting.LIGHT_PURPLE);
      var10000[13] = stackBuilder(var10003, var10004.add(var10005, Text.literal(var10006 + "Рюкзак" + String.valueOf(Formatting.GRAY) + " (I уровень)").setStyle(Style.EMPTY.withItalic(false))).build());
      var10003 = new ItemStack(Items.LIGHT_BLUE_SHULKER_BOX);
      var10004 = ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("рюкзак 2 уровень", "\"litebackpacks:backpack\":\"normal\""));
      var10005 = DataComponentTypes.CUSTOM_NAME;
      var10006 = String.valueOf(Formatting.AQUA);
      var10000[14] = stackBuilder(var10003, var10004.add(var10005, Text.literal(var10006 + "Рюкзак" + String.valueOf(Formatting.GRAY) + " (II уровень)").setStyle(Style.EMPTY.withItalic(false))).build());
      var10003 = new ItemStack(Items.RED_SHULKER_BOX);
      var10004 = ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("рюкзак 3 уровень", "\"litebackpacks:backpack\":\"big\""));
      var10005 = DataComponentTypes.CUSTOM_NAME;
      var10006 = String.valueOf(Formatting.RED);
      var10000[15] = stackBuilder(var10003, var10004.add(var10005, Text.literal(var10006 + "Рюкзак" + String.valueOf(Formatting.GRAY) + " (III уровень)").setStyle(Style.EMPTY.withItalic(false))).build());
      var10003 = new ItemStack(Items.MAGENTA_SHULKER_BOX);
      var10004 = ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("рюкзак 4 уровень", "\"litebackpacks:backpack\":\"huge\""));
      var10005 = DataComponentTypes.CUSTOM_NAME;
      var10006 = String.valueOf(Formatting.DARK_PURPLE);
      var10000[16] = stackBuilder(var10003, var10004.add(var10005, Text.literal(var10006 + "Рюкзак" + String.valueOf(Formatting.GRAY) + " (IV уровень)").setStyle(Style.EMPTY.withItalic(false))).build());
      var10000[17] = stackBuilder(new ItemStack(Items.POTION, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье победителя")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Зелье победителя").setStyle(Style.EMPTY.withItalic(false).withColor(-16737834))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.STRENGTH, 3600, 2), new StatusEffectInstance(StatusEffects.SPEED, 9600, 2), new StatusEffectInstance(StatusEffects.INVISIBILITY, 9600), new StatusEffectInstance(StatusEffects.HASTE, 1800, 1), new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1), new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 1), new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 0, 1)}))), Optional.of(33461), new ArrayList(), Optional.empty())).add(DataComponentTypes.MAX_STACK_SIZE, 64).build());
      holyItems = List.of(var10000);
      holyConsumables = List.of(stackBuilder(new ItemStack(Items.POTION, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье скорости")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Улучшенное зелье скорости").setStyle(Style.EMPTY.withItalic(false).withColor(-16716805))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.SPEED, 3600, 2)})))).add(DataComponentTypes.MAX_STACK_SIZE, 64).build()), stackBuilder(new ItemStack(Items.POTION, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье силы")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Улучшенное зелье силы").setStyle(Style.EMPTY.withItalic(false).withColor(-655103))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.STRENGTH, 3600, 2)})))).add(DataComponentTypes.MAX_STACK_SIZE, 64).build()));
      holyAutoBuyItems = List.of(stackBuilder(new ItemStack(Items.EXPERIENCE_BOTTLE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("15", "holy-exp-bottle-value:315")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Бутылек с 15ур. опыта").setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(-16385280))).build()), stackBuilder(new ItemStack(Items.EXPERIENCE_BOTTLE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("30", "holy-exp-bottle-value:1395")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Бутылек с 30ур. опыта").setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(-14080))).build()), stackBuilder(new ItemStack(Items.EXPERIENCE_BOTTLE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("50", "holy-exp-bottle-value:5345")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Бутылек с 50ур. опыта (5345)").setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(-36864))).build()), stackBuilder(new ItemStack(Items.EXPERIENCE_BOTTLE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("100", "holy-exp-bottle-value:30971")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Бутылек с 100ур. опыта").setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(-16385280))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("динамит А", "pyrotechnic-item:{name:\"A\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Динамит A", 16615936, 16465408).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("динамит Б2", "pyrotechnic-item:{name:\"B2\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Динамит Б2", 16711680, 11993088).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("динамит Б", "pyrotechnic-item:{name:\"B\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Динамит Б", 12386557, 16580849).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("С4 взрывчатка", "pyrotechnic-item:{name:\"C4\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("С4 ВзРыВчАтКа", 15551227, 4128755).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("Разрывная волна", "pyrotechnic-item:{name:\"WAVE\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Разрывная волна", 16588800, 16580804).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("стиллер", "pyrotechnic-item:{name:\"SPAWNER\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Стиллер", 13241517, 16125914).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.TNT), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("надёжный стиллер", "pyrotechnic-item:{name:\"SPAWNER2\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Надёжный стиллер", 779007, 565943).setStyle(Style.EMPTY.withItalic(false))).build()), stackBuilder(new ItemStack(Items.PRISMARINE_CRYSTALS), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("боевой фрагмент", "kringeItems:{type:\"BattleFragment\"}")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Боевой ", 10037504, 15154688).append(ColorUtil.text2Gradient("фрагмент", 16204288, 10037504)).setStyle(Style.EMPTY.withItalic(false))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.GOLDEN_PICKAXE), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("золотая кирка джейка", "kringeItems:{type:\"jake-pickaxe\"}")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Золотая кирка Джейка").setStyle(Style.EMPTY.withItalic(false).withColor(14527744))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.COMPASS), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("особый компас", "kringeItems:{UUID:")).add(DataComponentTypes.CUSTOM_NAME, ColorUtil.text2Gradient("Особый ", 4770494, 5963246).append(ColorUtil.text2Gradient("Компасс", 5963246, 4969671)).setStyle(Style.EMPTY.withItalic(false))).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.SPAWNER), ComponentMap.builder().build()));
      funtimeItems = List.of(stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера андромеды")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)-4.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)3.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR, (double)2.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.PROFILE, profileBuilder("ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTEwODQzNywKICAicHJvZmlsZUlkIiA6ICIzMjNiYjlkYzkwZWU0Nzk5YjUxYzE3NjRmZDRhNjI3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOcGllIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ0ZmZlM2YzNThmMjA5YmFkOGZmZjRkYzQ4MjQ1ZDliYWYwYTAzMWIzYzFlZTZiNzU4NDYwYTMzOWIxNTE5ZTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Сфера Андромеды")).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера пандоры")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)0.25F, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ARMOR, -0.1, Operation.ADD_MULTIPLIED_BASE)))).add(DataComponentTypes.PROFILE, profileBuilder("ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTY2NTExNCwKICAicHJvZmlsZUlkIiA6ICJkNzJlNGJjZDIyZGI0NjQ4OTUxNTc0M2UyYTRmMWFjMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJhdnZheSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZTUxZTY1ZWI0MDUyNzcyMzgyYzllNTA3YTU0YmRlZDQzZTM5Zjc1NWI1ZGRmNTViM2YzOTQ0M2NlZDQ2N2Y0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Сфера Пандоры")).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера химеры")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)-2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)3.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.PROFILE, profileBuilder("ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjE4MTEwOSwKICAicHJvZmlsZUlkIiA6ICJiNzRiMGQzNTBkNTk0NTU4YmYyYjBlMDJlYmE4NjE4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCcmFuZG9uYnBtMjg0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzlmYWJlZWQ0MjRiMjUyYTg5NDVhNjQ0MmI0NjJkNWYzMTQ3MDFhODE2ZGEyZDBhNjljY2RmY2ZkNzQ2ZTU4OGUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Сфера Химеры")).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера осириса")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.ATTACK_KNOCKBACK, 0.15, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ARMOR, (double)2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.KNOCKBACK_RESISTANCE, -0.15, Operation.ADD_MULTIPLIED_BASE)))).add(DataComponentTypes.PROFILE, profileBuilder("ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjY2Mzg3NiwKICAicHJvZmlsZUlkIiA6ICI3NGEwMzQxNWY1OTI0ZTA4YjMyMGM2MmU1NGE3ZjJhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZXp6aXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDgxMzYzNWJkODZiMTcxYmJlMTQzYWQ3MWUwOTAyMjkyNjQ5Y2IzYWI4NDQwZWQwMGY4NWNhNmNhMzgyOTkzNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Сфера Осириса")).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера титана")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MOVEMENT_SPEED, -0.15, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ARMOR_TOUGHNESS, (double)3.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR, (double)2.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.PROFILE, profileBuilder("ewogICJ0aW1lc3RhbXAiIDogMTc1MDM1NDQ1NTE5MiwKICAicHJvZmlsZUlkIiA6ICJkOTcwYzEzZTM4YWI0NzlhOTY1OGM1ZDQ1MjZkMTM0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmltcHlMYWNlODUxMjciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Сфера Титана")).build()), stackBuilder(new ItemStack(Items.PLAYER_HEAD), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сфера аполлона")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MOVEMENT_SPEED, -0.1, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)4.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.PROFILE, profileBuilder("ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjYyNTM0NywKICAicHJvZmlsZUlkIiA6ICJhMjk1ODZmYmU1ZDk0Nzk2OWZjOGQ4ZGE0NzlhNDNlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJMZXZlMjQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQxMTdiNjAxOGZlZjBkNTE1NjcyMTczZTNiMjZlNjYwZDY1MWU1ODc2YmE2ZDAzZTUzNDIyNzBjNDliZWM4MCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Сфера Аполлона")).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман карателя")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)7.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE), attributeBuilder(EntityAttributes.MAX_HEALTH, (double)-4.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, obfuscatedSymbol("xxx").append(Text.literal(" Талисман Карателя ").setStyle(Style.EMPTY.withItalic(false).withBold(true).withObfuscated(false))).append(obfuscatedSymbol("xxx")).withColor(-7536640)).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман крушителя")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)4.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)3.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR_TOUGHNESS, (double)2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR, (double)2.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, obfuscatedSymbol("xxx").append(Text.literal(" Талисман Крушителя ").setStyle(Style.EMPTY.withItalic(false).withBold(true).withObfuscated(false))).append(obfuscatedSymbol("xxx")).withColor(-7536640)).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман дедала")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)-4.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)5.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Талисман Дедала")).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман ехидны")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)-4.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)6.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR_TOUGHNESS, (double)-2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR, (double)-2.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Талисман Ехидны")).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман гармонии")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR, (double)2.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Талисман Гармонии")).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман тритона")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR_TOUGHNESS, (double)-2.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ARMOR, (double)2.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Талисман Тритона")).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман грани")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)-4.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.MOVEMENT_SPEED, 0.15, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_DAMAGE, (double)3.0F, Operation.ADD_VALUE)))).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Талисман Грани")).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.TOTEM_OF_UNDYING), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("талисман феникса")).add(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersComponent(List.of(attributeBuilder(EntityAttributes.MAX_HEALTH, (double)6.0F, Operation.ADD_VALUE), attributeBuilder(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE)))).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Талисман Феникса")).add(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true).build()), stackBuilder(new ItemStack(Items.NETHERITE_SCRAP, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("трапка")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Трапка")).build()), stackBuilder(new ItemStack(Items.DRIED_KELP, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("пласт")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Пласт")).build()), stackBuilder(new ItemStack(Items.ENDER_EYE, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("дезориентация")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Дезориентация")).build()), stackBuilder(new ItemStack(Items.SUGAR, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("явная пыль")).add(DataComponentTypes.CUSTOM_NAME, funtimeSphere("Явная пыль")).build()), stackBuilder(new ItemStack(Items.POTION, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("сила")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("Несоздаваемое зелье").setStyle(Style.EMPTY.withItalic(false))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.SPEED, 3600, 2), new StatusEffectInstance(StatusEffects.STRENGTH, 3600, 2)}))), Optional.empty(), new ArrayList(), Optional.empty())).add(DataComponentTypes.MAX_STACK_SIZE, 64).build()), stackBuilder(new ItemStack(Items.SPLASH_POTION), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье киллера")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.DARK_RED)).append(Text.literal(" Зелье Киллера").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.RED)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.STRENGTH, 1800, 3)}))), Optional.of(13369344), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.SPLASH_POTION), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье медика")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.DARK_PURPLE)).append(Text.literal(" Зелье Медика").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.LIGHT_PURPLE)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 900, 2), new StatusEffectInstance(StatusEffects.REGENERATION, 900, 2)}))), Optional.of(16711935), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.SPLASH_POTION), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье победителя")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.DARK_GREEN)).append(Text.literal(" Зелье Победителя").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.GREEN)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 1800, 1), new StatusEffectInstance(StatusEffects.INVISIBILITY, 12000), new StatusEffectInstance(StatusEffects.REGENERATION, 1800, 1), new StatusEffectInstance(StatusEffects.RESISTANCE, 900, 1)}))), Optional.of(65280), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.SPLASH_POTION), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье агента")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.GOLD)).append(Text.literal(" Зелье Агента").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.YELLOW)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000), new StatusEffectInstance(StatusEffects.HASTE, 2400), new StatusEffectInstance(StatusEffects.INVISIBILITY, 18000, 1), new StatusEffectInstance(StatusEffects.SPEED, 6000), new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 2)}))), Optional.of(16776960), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.SPLASH_POTION), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("моча флеша")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.DARK_AQUA)).append(Text.literal(" Моча Флеша").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.DARK_AQUA)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200), new StatusEffectInstance(StatusEffects.SPEED, 1200, 2)}))), Optional.of(65535), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.SPLASH_POTION), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("зелье отрыжки")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.RED)).append(Text.literal(" Зелье Отрыжки").setStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.GOLD)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.BLINDNESS, 100), new StatusEffectInstance(StatusEffects.GLOWING, 1200), new StatusEffectInstance(StatusEffects.HUNGER, 900, 10), new StatusEffectInstance(StatusEffects.SLOWNESS, 1800, 1), new StatusEffectInstance(StatusEffects.WITHER, 400, 1)}))), Optional.of(16737792), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.TIPPED_ARROW, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("ледяная стрела")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withItalic(false).withColor(-16750900)).append(Text.literal(" Ледяная Стрела").setStyle(Style.EMPTY.withItalic(false).withColor(-13382401)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 50), new StatusEffectInstance(StatusEffects.WEAKNESS, 120)}))), Optional.of(65535), new ArrayList(), Optional.empty())).build()), stackBuilder(new ItemStack(Items.TIPPED_ARROW, 64), ComponentMap.builder().add(DataComponentTypes.CUSTOM_DATA, itemBuilder("проклятая стрела")).add(DataComponentTypes.CUSTOM_NAME, Text.literal("[★]").setStyle(Style.EMPTY.withItalic(false).withColor(-6750208)).append(Text.literal(" Проклятая Стрела").setStyle(Style.EMPTY.withItalic(false).withColor(-3407872)))).add(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.of(RegistryEntry.of(new Potion("", new StatusEffectInstance[]{new StatusEffectInstance(StatusEffects.BLINDNESS, 40), new StatusEffectInstance(StatusEffects.NAUSEA, 100), new StatusEffectInstance(StatusEffects.WEAKNESS, 200)}))), Optional.of(0), new ArrayList(), Optional.empty())).build()));
   }
   }
}
