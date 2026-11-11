package ru.zov.implement.features.modules.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.implement.events.render.ItemRendererEvent;

public class TalismanIndication extends Module {
   private static final Identifier CRUSHER_HELMET_MODEL = Identifier.of("aspectdlc", "crusher_helmet");
   private static final Identifier CRUSHER_CHESTPLATE_MODEL = Identifier.of("aspectdlc", "crusher_chestplate");
   private static final Identifier CRUSHER_LEGGINGS_MODEL = Identifier.of("aspectdlc", "crusher_leggings");
   private static final Identifier CRUSHER_BOOTS_MODEL = Identifier.of("aspectdlc", "crusher_boots");
   private static final Identifier THORNS_ARMOR_MODEL = Identifier.of("aspectdlc", "thorns_armor");
   private static final double VALUE_EPSILON = 1.0E-6;

   private record ModelModifierEntry(RegistryEntry<EntityAttribute> attribute, Operation operation, double value) {
      boolean sameAs(ModelModifierEntry other) {
         return this.attribute.equals(other.attribute) && this.operation == other.operation && Math.abs(this.value - other.value) < VALUE_EPSILON;
      }
   }

   private record CustomItemModel(Identifier fancyModel, Identifier simpleModel, List<ModelModifierEntry> modifiers) {
      Identifier getModel(boolean simple) {
         return simple ? this.simpleModel : this.fancyModel;
      }

      boolean matches(List<ModelModifierEntry> actual) {
         if (actual.size() != this.modifiers.size()) {
            return false;
         }

         for (ModelModifierEntry expected : this.modifiers) {
            if (actual.stream().noneMatch(expected::sameAs)) {
               return false;
            }
         }

         return true;
      }
   }

   private static final List<CustomItemModel> TALISMANS = List.of(
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_karatel0"),
         Identifier.of("aspectdlc", "tal_karatel0_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 7.0),
            new ModelModifierEntry(EntityAttributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.1),
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, -4.0)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_demon"),
         Identifier.of("aspectdlc", "tal_demon_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.ATTACK_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.1),
            new ModelModifierEntry(EntityAttributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 2.5)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_mraka"),
         Identifier.of("aspectdlc", "tal_mraka_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, 1.5),
            new ModelModifierEntry(EntityAttributes.ARMOR, Operation.ADD_VALUE, 1.5)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_vihrya"),
         Identifier.of("aspectdlc", "tal_vihrya_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.15),
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, 2.0),
            new ModelModifierEntry(EntityAttributes.ATTACK_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.15)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_razdora"),
         Identifier.of("aspectdlc", "tal_razdora_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.MOVEMENT_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.1),
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, 2.0),
            new ModelModifierEntry(EntityAttributes.ATTACK_SPEED, Operation.ADD_MULTIPLIED_BASE, 0.1),
            new ModelModifierEntry(EntityAttributes.ARMOR, Operation.ADD_VALUE, -3.0),
            new ModelModifierEntry(EntityAttributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 4.0)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_tirana"),
         Identifier.of("aspectdlc", "tal_tirana_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, -4.0),
            new ModelModifierEntry(EntityAttributes.ARMOR, Operation.ADD_VALUE, 2.0),
            new ModelModifierEntry(EntityAttributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 2.0)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_yarosti"),
         Identifier.of("aspectdlc", "tal_yarosti_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, -4.0),
            new ModelModifierEntry(EntityAttributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 5.0)
         )
      ),
      new CustomItemModel(
         Identifier.of("aspectdlc", "tal_krush0"),
         Identifier.of("aspectdlc", "tal_krush0_simple"),
         List.of(
            new ModelModifierEntry(EntityAttributes.ARMOR_TOUGHNESS, Operation.ADD_VALUE, 2.0),
            new ModelModifierEntry(EntityAttributes.ARMOR, Operation.ADD_VALUE, 2.0),
            new ModelModifierEntry(EntityAttributes.MAX_HEALTH, Operation.ADD_VALUE, 4.0),
            new ModelModifierEntry(EntityAttributes.ATTACK_DAMAGE, Operation.ADD_VALUE, 3.0)
         )
      )
   );

   private final SelectSetting mode = new SelectSetting("Мод", "Набор моделей сервера").value("SpookyTime", "ArtyGrief").selected("SpookyTime");
   private final BooleanSetting talismanIndication = new BooleanSetting("Индикация талисманов", "Подменять модель талисманов").setValue(true);
   private final SelectSetting indication = new SelectSetting("Выбор индикации", "Вид модели талисмана")
      .value("Красивый", "Простая")
      .selected("Красивый")
      .visible(this.talismanIndication::isValue);
   private final BooleanSetting crusherArmorIndication = new BooleanSetting("Индикация брони круша", "Подменять модель брони круша").setValue(true);
   private final BooleanSetting thornsArmorIndication = new BooleanSetting("Индикация шипов", "Подменять модель брони с шипами").setValue(true);

   public TalismanIndication() {
      super("TalismanIndication", ModuleCategory.RENDER);
      this.setup(this.mode, this.talismanIndication, this.indication, this.crusherArmorIndication, this.thornsArmorIndication);
   }

   @EventHandler
   public void onItemRenderer(ItemRendererEvent e) {
      e.setStack(this.getRenderStack(e.getStack()));
   }

   public ItemStack getRenderStack(ItemStack stack) {
      if (!this.isState() || stack.isEmpty()) {
         return stack;
      }

      ItemStack thorns = this.getThornsArmorStack(stack);
      if (thorns != stack) {
         return thorns;
      }

      ItemStack crusher = this.getCrusherArmorStack(stack);
      if (crusher != stack) {
         return crusher;
      }

      if (this.talismanIndication.isValue() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
         CustomItemModel talisman = this.getDefinition(stack);
         if (talisman == null) {
            return stack;
         }

         return this.withModel(stack, talisman.getModel(this.indication.isSelected("Простая")));
      }

      return stack;
   }

   private ItemStack getThornsArmorStack(ItemStack stack) {
      return this.thornsArmorIndication.isValue() && this.isThornsArmor(stack) ? this.withModel(stack, THORNS_ARMOR_MODEL) : stack;
   }

   private ItemStack getCrusherArmorStack(ItemStack stack) {
      if (!this.crusherArmorIndication.isValue() || !this.isCrusherArmor(stack)) {
         return stack;
      }

      Identifier model = this.getCrusherArmorModel(stack);
      return model == null ? stack : this.withModel(stack, model);
   }

   private ItemStack withModel(ItemStack stack, Identifier model) {
      if (model.equals(stack.get(DataComponentTypes.ITEM_MODEL))) {
         return stack;
      }

      ItemStack copy = stack.copy();
      copy.set(DataComponentTypes.ITEM_MODEL, model);
      return copy;
   }

   private Identifier getCrusherArmorModel(ItemStack stack) {
      EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
      if (equippable == null || equippable.slot() == null) {
         return null;
      }

      EquipmentSlot slot = equippable.slot();
      if (slot == EquipmentSlot.HEAD) {
         return CRUSHER_HELMET_MODEL;
      } else if (slot == EquipmentSlot.CHEST) {
         return CRUSHER_CHESTPLATE_MODEL;
      } else if (slot == EquipmentSlot.LEGS) {
         return CRUSHER_LEGGINGS_MODEL;
      } else {
         return slot == EquipmentSlot.FEET ? CRUSHER_BOOTS_MODEL : null;
      }
   }

   private CustomItemModel getDefinition(ItemStack stack) {
      List<ModelModifierEntry> actual = this.getOffhandAttributes(stack);
      for (CustomItemModel talisman : TALISMANS) {
         if (talisman.matches(actual)) {
            return talisman;
         }
      }

      return null;
   }

   private boolean isCrusherArmor(ItemStack stack) {
      if (mc.world == null || stack.get(DataComponentTypes.EQUIPPABLE) == null) {
         return false;
      }

      Registry<Enchantment> enchantments = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
      return EnchantmentHelper.getLevel(enchantments.getOrThrow(Enchantments.PROTECTION), stack) >= 5
         && EnchantmentHelper.getLevel(enchantments.getOrThrow(Enchantments.MENDING), stack) > 0;
   }

   private boolean isThornsArmor(ItemStack stack) {
      if (mc.world == null || !this.isNetheriteArmor(stack)) {
         return false;
      }

      Registry<Enchantment> enchantments = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
      return EnchantmentHelper.getLevel(enchantments.getOrThrow(Enchantments.PROTECTION), stack) >= 5
         && EnchantmentHelper.getLevel(enchantments.getOrThrow(Enchantments.MENDING), stack) > 0
         && EnchantmentHelper.getLevel(enchantments.getOrThrow(Enchantments.THORNS), stack) > 0;
   }

   private boolean isNetheriteArmor(ItemStack stack) {
      return stack.isOf(Items.NETHERITE_HELMET)
         || stack.isOf(Items.NETHERITE_CHESTPLATE)
         || stack.isOf(Items.NETHERITE_LEGGINGS)
         || stack.isOf(Items.NETHERITE_BOOTS);
   }

   private List<ModelModifierEntry> getOffhandAttributes(ItemStack stack) {
      AttributeModifiersComponent component = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (component == null) {
         return List.of();
      }

      List<ModelModifierEntry> result = new ArrayList<>();
      for (AttributeModifiersComponent.Entry entry : component.modifiers()) {
         if (entry.slot().matches(EquipmentSlot.OFFHAND)) {
            result.add(new ModelModifierEntry(entry.attribute(), this.normalizeOperation(entry), entry.modifier().value()));
         }
      }

      return result;
   }

   private Operation normalizeOperation(AttributeModifiersComponent.Entry entry) {
      boolean speedAttribute = entry.attribute().equals(EntityAttributes.MOVEMENT_SPEED) || entry.attribute().equals(EntityAttributes.ATTACK_SPEED);
      return speedAttribute && entry.modifier().operation() == Operation.ADD_MULTIPLIED_TOTAL ? Operation.ADD_MULTIPLIED_BASE : entry.modifier().operation();
   }
}
