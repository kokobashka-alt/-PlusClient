package ru.zov.common.util.color;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import java.awt.Color;
import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;
import ru.zov.implement.features.modules.render.Hud;

public final class ColorUtil {
   public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");
   public static Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   public static final int RED;
   public static final int GREEN;
   public static final int BLUE;
   public static final int YELLOW;
   public static final int WHITE;
   public static final int BLACK;
   public static final int HALF_BLACK;
   public static final int HALF_LIGHT_RED;
   public static final int LIGHT_RED;
   public static final int LIGHT_BLUE;

   public static int red(int c) {
      return c >> 16 & 255;
   }

   public static int green(int c) {
      return c >> 8 & 255;
   }

   public static int blue(int c) {
      return c & 255;
   }

   public static int alpha(int c) {
      return c >> 24 & 255;
   }

   public static float redf(int c) {
      return (float)red(c) / 255.0F;
   }

   public static float greenf(int c) {
      return (float)green(c) / 255.0F;
   }

   public static float bluef(int c) {
      return (float)blue(c) / 255.0F;
   }

   public static float alphaf(int c) {
      return (float)alpha(c) / 255.0F;
   }

   public static int[] getRGBA(int c) {
      return new int[]{red(c), green(c), blue(c), alpha(c)};
   }

   public static int[] getRGB(int c) {
      return new int[]{red(c), green(c), blue(c)};
   }

   public static float[] getRGBAf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
   }

   public static float[] getRGBf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c)};
   }

   public static int getColor(float red, float green, float blue, float alpha) {
      return getColor(Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
   }

   public static int getColor(int red, int green, int blue, float alpha) {
      return getColor(red, green, blue, Math.round(alpha * 255.0F));
   }

   public static int getColor(float red, float green, float blue) {
      return getColor(red, green, blue, 1.0F);
   }

   public static int getColor(int brightness, int alpha) {
      return getColor(brightness, brightness, brightness, alpha);
   }

   public static int getColor(int brightness, float alpha) {
      return getColor(brightness, Math.round(alpha * 255.0F));
   }

   public static int getColor(int brightness) {
      return getColor(brightness, brightness, brightness);
   }

   public static int replAlpha(int color, int alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int replAlpha(int color, float alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int multAlpha(int color, float percent01) {
      return getColor(red(color), green(color), blue(color), Math.round((float)alpha(color) * percent01));
   }

   public static int multColor(int colorStart, int colorEnd, float progress) {
      return getColor(Math.round((float)red(colorStart) * redf(colorEnd) * progress), Math.round((float)green(colorStart) * greenf(colorEnd) * progress), Math.round((float)blue(colorStart) * bluef(colorEnd) * progress), Math.round((float)alpha(colorStart) * alphaf(colorEnd) * progress));
   }

   public static int multRed(int colorStart, int colorEnd, float progress) {
      return getColor(Math.round((float)red(colorStart) * redf(colorEnd) * progress), Math.round((float)green(colorStart) * greenf(colorEnd) * progress), Math.round((float)blue(colorStart) * bluef(colorEnd) * progress), Math.round((float)alpha(colorStart) * alphaf(colorEnd) * progress));
   }

   public static int multDark(int color, float percent01) {
      return getColor(Math.round((float)red(color) * percent01), Math.round((float)green(color) * percent01), Math.round((float)blue(color) * percent01), alpha(color));
   }

   public static int multBright(int color, float percent01) {
      return getColor(Math.min(255, Math.round((float)red(color) / percent01)), Math.min(255, Math.round((float)green(color) / percent01)), Math.min(255, Math.round((float)blue(color) / percent01)), alpha(color));
   }

   public static int overCol(int color1, int color2, float percent01) {
      float percent = MathHelper.clamp(percent01, 0.0F, 1.0F);
      return getColor(MathHelper.lerp(percent, red(color1), red(color2)), MathHelper.lerp(percent, green(color1), green(color2)), MathHelper.lerp(percent, blue(color1), blue(color2)), MathHelper.lerp(percent, alpha(color1), alpha(color2)));
   }

   public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
      return new Vector4i(multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha));
   }

   public static int multRedAndAlpha(int color, float red, float alpha) {
      return getColor(red(color), Math.min(255, Math.round((float)green(color) / red)), Math.min(255, Math.round((float)blue(color) / red)), Math.round((float)alpha(color) * alpha));
   }

   public static int multRed(int color, float percent01) {
      return getColor(red(color), Math.min(255, Math.round((float)green(color) / percent01)), Math.min(255, Math.round((float)blue(color) / percent01)), alpha(color));
   }

   public static int multGreen(int color, float percent01) {
      return getColor(Math.min(255, Math.round((float)red(color) / percent01)), green(color), Math.min(255, Math.round((float)blue(color) / percent01)), alpha(color));
   }

   public static int multBlue(int color, float percent01) {
      return getColor(Math.min(255, Math.round((float)red(color) / percent01)), Math.min(255, Math.round((float)green(color) / percent01)), blue(color), alpha(color));
   }

   public static int[] genGradientForText(int color1, int color2, int length) {
      int[] gradient = new int[length];

      for(int i = 0; i < length; ++i) {
         float pc = (float)i / (float)(length - 1);
         gradient[i] = overCol(color1, color2, pc);
      }

      return gradient;
   }

   public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      float hue = (float)angle / 360.0F;
      int color = Color.HSBtoRGB(hue, saturation, brightness);
      return getColor(red(color), green(color), blue(color), Math.round(opacity * 255.0F));
   }

   public static int fade(int speed, int index, int first, int second) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      angle = angle >= 180 ? 360 - angle : angle;
      return overCol(first, second, (float)angle / 180.0F);
   }

   public static int fade(int index) {
      int clientColor = getClientColor();
      return multAlpha(fade(8, index, brighter(clientColor), darker(clientColor)), (float)alpha(clientColor));
   }

   public static Vector4i roundClientColor(float alpha) {
      return new Vector4i(multAlpha(fade(270), alpha), multAlpha(fade(0), alpha), multAlpha(fade(180), alpha), multAlpha(fade(90), alpha));
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      return computeColor(red, green, blue, alpha);
   }

   public static int getColor(int red, int green, int blue) {
      return getColor(red, green, blue, 255);
   }

   private static int computeColor(int red, int green, int blue, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24 | MathHelper.clamp(red, 0, 255) << 16 | MathHelper.clamp(green, 0, 255) << 8 | MathHelper.clamp(blue, 0, 255);
   }

   private static int darker(int color) {
      return computeColor((int)((float)red(color) * 0.7F), (int)((float)green(color) * 0.7F), (int)((float)blue(color) * 0.7F), 255);
   }

   private static int brighter(int color) {
      int red = red(color);
      int green = green(color);
      int blue = blue(color);
      int threshold = 3;
      if (red == 0 && green == 0 && blue == 0) {
         return computeColor(threshold, threshold, threshold, 255);
      }

      if (red > 0 && red < threshold) red = threshold;
      if (green > 0 && green < threshold) green = threshold;
      if (blue > 0 && blue < threshold) blue = threshold;
      return computeColor(Math.min((int)((float)red / 0.7F), 255), Math.min((int)((float)green / 0.7F), 255), Math.min((int)((float)blue / 0.7F), 255), 255);
   }

   public static String formatting(int color) {
      return "⏏" + color + "⏏";
   }

   public static MutableText text2Gradient(String text, int start, int end) {
      MutableText mutable = Text.empty();
      char[] chars = text.toCharArray();
      int i = 0;

      for(int length = chars.length; i < length; ++i) {
         char c = chars[i];
         float t = (float)i / (float)(length - 1);
         int color = overCol(start, end, t);
         mutable.append(Text.literal(String.valueOf(c)).withColor(color));
      }

      return mutable;
   }

   public static String removeFormatting(String text) {
      return text != null && !text.isEmpty() ? FORMATTING_CODE_PATTERN.matcher(text).replaceAll("") : null;
   }

   public static int getMainGuiColor() {
      return 0xFF18181D;
   }

   public static int getGuiRectColor(float alpha) {
      return multAlpha(0xFF1A1A1F, alpha);
   }

   public static int getGuiRectColor2(float alpha) {
      return multAlpha(0xFF1E1E26, alpha);
   }

   public static int getRect(float alpha) {
      return multAlpha(0xFF16161A, alpha);
   }

   public static int getRectDarker(float alpha) {
      return multAlpha(0xFF18181D, alpha);
   }

   public static int getText(float alpha) {
      return multAlpha(getText(), alpha);
   }

   public static int getText() {
      return 0xFFE6E6E6;
   }

   public static int getClientColor() {
      return Hud.getInstance().colorSetting.getColor();
   }

   public static int getClientColor(float alpha) {
      return multAlpha(getClientColor(), alpha);
   }

   public static int getClientColorSecondary() {
      return darker(getClientColor());
   }

   public static int getClientColorSecondary(float alpha) {
      return multAlpha(getClientColorSecondary(), alpha);
   }

   public static int getFriendColor() {
      return 0xFF55FF55;
   }

   public static int getFriendColor2() {
      return -2147474688;
   }

   public static int getOutline(float alpha, float bright) {
      return multBright(multAlpha(getOutline(), alpha), bright);
   }

   public static int getOutline(float alpha) {
      return multAlpha(getOutline(), alpha);
   }

   public static int getOutline() {
      return 0xFF373746;
   }

   private ColorUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      RED = getColor(255, 0, 0);
      GREEN = getColor(0, 255, 0);
      BLUE = getColor(0, 0, 255);
      YELLOW = getColor(255, 255, 0);
      WHITE = getColor(255);
      BLACK = getColor(0);
      HALF_BLACK = getColor(0, 0.5F);
      HALF_LIGHT_RED = getColor(255, 85, 85, 125);
      LIGHT_RED = getColor(255, 85, 85);
      LIGHT_BLUE = getColor(85, 85, 255);
   }

}
