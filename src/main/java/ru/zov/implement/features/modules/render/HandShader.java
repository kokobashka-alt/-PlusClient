package ru.zov.implement.features.modules.render;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;

/**
 * Огненно-дымный шлейф вокруг рук/предмета от первого лица.
 * Порт модуля HandShader (Wyvern → sky.core): захват рук в текстуру + feedback-шлейф + kawase-размытие.
 * Рендерер (HandShaderRenderer) в репозитории отсутствует: модуль пока только хранит настройки и нигде не зарегистрирован.
 * Отличия от референса: нет цвета темы (в базе нет ThemeManager) — вместо него фиксированный фиолетовый.
 */
public final class HandShader extends Module {
   private static final HandShader INSTANCE = new HandShader();

   public static HandShader getInstance() {
      return INSTANCE;
   }

   public final SelectSetting mode = new SelectSetting("Режим", "Тип эффекта на руках").value("Дым", "Красивый").selected("Дым");

   public final ValueSetting intensity = new ValueSetting("Интенсивность", "Общая сила эффекта").setValue(0.85F).range(0.1F, 1.5F);
   public final ValueSetting speed = new ValueSetting("Скорость", "Скорость анимации огня/дыма").setValue(1.15F).range(0.2F, 3.0F);
   public final ValueSetting length = new ValueSetting("Длина", "Длина шлейфа").setValue(0.72F).range(0.1F, 1.0F)
      .visible(() -> this.mode.isSelected("Дым"));

   public final ValueSetting prettyGlow = new ValueSetting("Глоу", "Радиус свечения силуэта").setValue(2.0F).range(0.5F, 5.0F)
      .visible(() -> this.mode.isSelected("Красивый"));
   public final ValueSetting prettyHeight = new ValueSetting("Высота", "Высота подъёма пламени").setValue(0.06F).range(0.02F, 0.6F)
      .visible(() -> this.mode.isSelected("Красивый"));
   public final ValueSetting prettyWind = new ValueSetting("Ветер", "Сила ветра пламени").setValue(1.0F).range(0.0F, 3.0F)
      .visible(() -> this.mode.isSelected("Красивый"));
   public final ValueSetting prettyWave = new ValueSetting("Волны", "Сила волн пламени").setValue(1.0F).range(0.0F, 3.0F)
      .visible(() -> this.mode.isSelected("Красивый"));

   public final ValueSetting trailFade = new ValueSetting("Затухание шлейфа", "Сохранение шлейфа между кадрами").setValue(0.91F).range(0.55F, 0.98F)
      .visible(() -> this.mode.isSelected("Дым"));
   public final ValueSetting trailSoftness = new ValueSetting("Мягкость шлейфа", "Контраст накопленного шлейфа").setValue(1.35F).range(0.45F, 2.5F)
      .visible(() -> this.mode.isSelected("Дым"));
   public final ValueSetting trailBlur = new ValueSetting("Размытие шлейфа", "Радиус размытия дыма").setValue(1.55F).range(0.2F, 3.0F)
      .visible(() -> this.mode.isSelected("Дым"));
   public final ValueSetting handSoftness = new ValueSetting("Мягкость рук", "Контраст свечения вокруг рук").setValue(1.3F).range(0.45F, 2.5F)
      .visible(() -> this.mode.isSelected("Дым"));
   public final ValueSetting handBlur = new ValueSetting("Размытие рук", "Радиус размытия свечения рук").setValue(1.45F).range(0.2F, 3.0F)
      .visible(() -> this.mode.isSelected("Дым"));
   public final ValueSetting smoke = new ValueSetting("Дым", "Доля дымной составляющей").setValue(0.55F).range(0.0F, 0.8F)
      .visible(() -> this.mode.isSelected("Дым"));

   public final BooleanSetting itemColor = new BooleanSetting("Цвет предмета", "Окрашивать эффект в цвет материала предмета").setValue(true);

   private HandShader() {
      super("HandShader", "Hand Shader", ModuleCategory.RENDER);
      this.setup(
         this.mode, this.intensity, this.speed, this.length,
         this.prettyGlow, this.prettyHeight, this.prettyWind, this.prettyWave,
         this.trailFade, this.trailSoftness, this.trailBlur, this.handSoftness, this.handBlur, this.smoke,
         this.itemColor
      );
   }

   public boolean isPretty() {
      return this.mode.isSelected("Красивый");
   }

   /** Нормализованный цвет свечения (RGB 0..1) для рендерера. */
   public float[] glowColor() {
      Color color = this.itemColor.isValue() ? this.heldItemColor() : new Color(0x6633FF);
      return new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
   }

   private Color heldItemColor() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return Color.WHITE;
      }

      ItemStack stack = mc.player.getMainHandStack();
      if (stack.isEmpty()) {
         stack = mc.player.getOffHandStack();
      }
      if (stack.isEmpty()) {
         return Color.WHITE;
      }

      String path = Registries.ITEM.getId(stack.getItem()).getPath();
      if (path.contains("diamond")) {
         return new Color(0x55DDE0);
      }
      if (path.contains("netherite")) {
         return new Color(0x5B4A67);
      }
      if (path.contains("gold")) {
         return new Color(0xFFD45A);
      }
      if (path.contains("iron")) {
         return new Color(0xD8DEE8);
      }
      if (path.contains("emerald")) {
         return new Color(0x35D06F);
      }
      if (path.contains("redstone")) {
         return new Color(0xE23B3B);
      }
      if (path.contains("lapis")) {
         return new Color(0x3156D4);
      }
      return new Color(0xE6E6E6);
   }
}
