package ru.zov.implement.features.modules.render;

import net.minecraft.util.hit.EntityHitResult;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render2DUtil;

public class CrossHair extends Module {
   private float red = 0.0F;
   private final ValueSetting attackSetting = (new ValueSetting("Attack Indent", "Item Cooldown Indent")).setValue(10.0F).range(0, 20);
   private final ValueSetting indentSetting = (new ValueSetting("Indent", "Indent from the center of the screen")).setValue(0.0F).range(0, 5);
   private final ValueSetting size1Setting = (new ValueSetting("Width", "Width Cross Hair")).setValue(4.0F).range(2, 10);
   private final ValueSetting size2Setting = (new ValueSetting("Height", "Height Cross Hair")).setValue(1.0F).range(1, 4);

   public static CrossHair getInstance() {
      return (CrossHair)Instance.get(CrossHair.class);
   }

   public CrossHair() {
      super("CrossHair", "Cross Hair", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.attackSetting, this.indentSetting, this.size1Setting, this.size2Setting});
   }

   public void onRenderCrossHair() {
      if (mc.player != null && mc.currentScreen == null) {
         this.red = MathUtil.interpolateSmooth((double)1.5F, this.red, mc.crosshairTarget instanceof EntityHitResult ? 5.0F : 1.0F);
         float x = (float)window.getScaledWidth() / 2.0F;
         float y = (float)window.getScaledHeight() / 2.0F;
         float cooldown = (float)this.attackSetting.getInt() - (float)this.attackSetting.getInt() * mc.player.getAttackCooldownProgress(tickCounter.getTickProgress(true));
         float size = this.size1Setting.getValue();
         float size2 = this.size2Setting.getValue();
         float offset = size2 / 2.0F;
         float indent = (float)this.indentSetting.getInt() + cooldown;
         this.renderMain(x, y, size, size2, 1.0F, indent, offset, ColorUtil.BLACK);
         this.renderMain(x, y, size, size2, 0.0F, indent, offset, ColorUtil.multRed(ColorUtil.WHITE, this.red));
      }
   }

   private void renderMain(float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
      Render2DUtil.drawQuad(x - offset - padding / 2.0F, y - size - indent - padding / 2.0F, size2 + padding, size + padding, color);
      Render2DUtil.drawQuad(x - offset - padding / 2.0F, y + indent - padding / 2.0F, size2 + padding, size + padding, color);
      Render2DUtil.drawQuad(x - size - indent - padding / 2.0F, y - offset - padding / 2.0F, size + padding, size2 + padding, color);
      Render2DUtil.drawQuad(x + indent - padding / 2.0F, y - offset - padding / 2.0F, size + padding, size2 + padding, color);
   }
}
