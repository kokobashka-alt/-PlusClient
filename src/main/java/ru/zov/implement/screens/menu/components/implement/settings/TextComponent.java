package ru.zov.implement.screens.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import ru.zov.api.feature.module.setting.implement.TextSetting;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.StringUtil;
import ru.zov.common.util.render.ScissorManager;

public class TextComponent extends AbstractSettingComponent {
   public static boolean typing;
   private final TextSetting setting;
   private float rectX;
   private float rectY;
   private float rectWidth;
   private float rectHeight;
   private boolean dragging;
   private int cursorPosition = 0;
   private int selectionStart = -1;
   private int selectionEnd = -1;
   private long lastClickTime = 0L;
   private float xOffset = 0.0F;
   private String text = "";

   public TextComponent(TextSetting setting) {
      super(setting);
      this.setting = setting;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      String wrapped = StringUtil.wrap(this.setting.getDescription(), 70, 12);
      FontRenderer font = Fonts.getSize(12);
      this.height = (float)((int)(18.0F + font.getStringHeight(wrapped) / 3.0F));
      this.rectX = this.x + this.width - 61.5F;
      this.rectY = this.y + 6.0F;
      this.rectWidth = 53.0F;
      this.rectHeight = 12.0F;
      rectangle.render(ShapeProperties.create(matrix, (double)this.rectX, (double)this.rectY, (double)this.rectWidth, (double)this.rectHeight).round(2.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getGuiRectColor(1.0F)).build());
      Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), this.setting.getName(), (double)(this.x + 9.0F), (double)(this.y + 6.0F), -2828575);
      font.drawString(context.getMatrices(), wrapped, (double)(this.x + 9.0F), (double)(this.y + 15.0F), -7894892);
      this.updateXOffset(font, this.cursorPosition);
      ScissorManager scissor = main.getScissorManager();
      scissor.push(matrix, this.rectX + 1.0F, (float)window.getScaledHeight() / 2.0F - 96.0F, this.rectWidth - 3.0F, 220.0F);
      if (typing && this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd) {
         int start = Math.max(0, Math.min(this.getStartOfSelection(), this.text.length()));
         int end = Math.max(0, Math.min(this.getEndOfSelection(), this.text.length()));
         if (start < end) {
            float selectionXStart = this.rectX + 3.0F - this.xOffset + font.getStringWidth(this.text.substring(0, start));
            float selectionXEnd = this.rectX + 3.0F - this.xOffset + font.getStringWidth(this.text.substring(0, end));
            float selectionWidth = selectionXEnd - selectionXStart;
            rectangle.render(ShapeProperties.create(matrix, (double)selectionXStart, (double)(this.rectY + this.rectHeight / 2.0F - 4.0F), (double)selectionWidth, (double)8.0F).color(-11172376).build());
         }
      }

      font.drawString(context.getMatrices(), this.text, (double)(this.rectX + 3.0F - this.xOffset), (double)(this.rectY + this.rectHeight / 2.0F - 1.0F), typing ? -1 : -7894892);
      if (!typing && this.text.isEmpty()) {
         font.drawString(context.getMatrices(), this.text = this.setting.getText(), (double)(this.rectX + 3.0F), (double)(this.rectY + this.rectHeight / 2.0F - 1.0F), -7894892);
      }

      scissor.pop();
      long currentTime = System.currentTimeMillis();
      boolean focused = typing && currentTime % 1000L < 500L;
      if (focused && (this.selectionStart == -1 || this.selectionStart == this.selectionEnd)) {
         float cursorX = font.getStringWidth(this.text.substring(0, this.cursorPosition));
         rectangle.render(ShapeProperties.create(matrix, (double)(this.rectX + 3.0F - this.xOffset + cursorX), (double)(this.rectY + this.rectHeight / 2.0F - 3.5F), (double)0.5F, (double)7.0F).color(-1).build());
      }

      if (this.dragging) {
         this.cursorPosition = this.getCursorIndexAt((double)mouseX);
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition + 1;
         }

         this.selectionEnd = this.cursorPosition;
      }

   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      this.dragging = true;
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, (double)this.rectX, (double)this.rectY, (double)this.rectWidth, (double)this.rectHeight) && button == 0) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastClickTime < 250L) {
            this.selectionStart = 0;
            this.selectionEnd = this.text.length();
         } else {
            typing = true;
            this.dragging = true;
            this.lastClickTime = currentTime;
            this.cursorPosition = this.getCursorIndexAt(mouseX);
            this.selectionStart = this.cursorPosition;
            this.selectionEnd = this.cursorPosition;
         }
      } else {
         typing = false;
         this.clearSelection();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (typing && this.text.length() < this.setting.getMax()) {
         this.deleteSelectedText();
         this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
         ++this.cursorPosition;
         this.clearSelection();
      }

      return super.charTyped(chr, modifiers);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (typing) {
         if ((org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == 1 || org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == 1)) {
            switch (keyCode) {
               case 65 -> this.selectAllText();
               case 67 -> this.copyToClipboard();
               case 86 -> this.pasteFromClipboard();
            }
         } else {
            switch (keyCode) {
               case 257:
               case 259:
                  this.handleTextModification(keyCode);
               case 258:
               case 260:
               case 261:
               default:
                  break;
               case 262:
               case 263:
                  this.moveCursor(keyCode);
            }
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void pasteFromClipboard() {
      String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
      if (clipboardText != null) {
         this.replaceText(this.cursorPosition, this.cursorPosition, clipboardText);
      }

   }

   private void copyToClipboard() {
      if (this.hasSelection()) {
         GLFW.glfwSetClipboardString(window.getHandle(), this.getSelectedText());
      }

   }

   private void selectAllText() {
      this.selectionStart = 0;
      this.selectionEnd = this.text.length();
   }

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.hasSelection()) {
            this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
         } else if (this.cursorPosition > 0) {
            this.replaceText(this.cursorPosition - 1, this.cursorPosition, "");
         }
      } else if (keyCode == 257 && this.text.length() >= this.setting.getMin() && this.text.length() <= this.setting.getMax()) {
         this.setting.setText(this.text);
         typing = false;
      }

   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         --this.cursorPosition;
      } else if (keyCode == 262 && this.cursorPosition < this.text.length()) {
         ++this.cursorPosition;
      }

      this.updateSelectionAfterCursorMove();
   }

   private void updateSelectionAfterCursorMove() {
      if ((org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == 1 || org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == 1)) {
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition;
         }

         this.selectionEnd = this.cursorPosition;
      } else {
         this.clearSelection();
      }

   }

   private void replaceText(int start, int end, String replacement) {
      if (start < 0) {
         start = 0;
      }

      if (end > this.text.length()) {
         end = this.text.length();
      }

      if (start > end) {
         start = end;
      }

      this.text = this.text.substring(0, start) + replacement + this.text.substring(end);
      this.cursorPosition = start + replacement.length();
      this.clearSelection();
   }

   private boolean hasSelection() {
      return this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd;
   }

   private String getSelectedText() {
      return this.text.substring(this.getStartOfSelection(), this.getEndOfSelection());
   }

   private int getStartOfSelection() {
      return Math.min(this.selectionStart, this.selectionEnd);
   }

   private int getEndOfSelection() {
      return Math.max(this.selectionStart, this.selectionEnd);
   }

   private void clearSelection() {
      this.selectionStart = -1;
      this.selectionEnd = -1;
   }

   private int getCursorIndexAt(double mouseX) {
      FontRenderer font = Fonts.getSize(12, Fonts.Type.BOLD);
      float relativeX = (float)mouseX - this.rectX - 3.0F + this.xOffset;

      int position;
      for(position = 0; position < this.text.length(); ++position) {
         float textWidth = font.getStringWidth(this.text.substring(0, position + 1));
         if (textWidth > relativeX) {
            break;
         }
      }

      return position;
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth(this.text.substring(0, cursorPosition));
      if (cursorX < this.xOffset) {
         this.xOffset = cursorX;
      } else if (cursorX - this.xOffset > this.rectWidth - 7.0F) {
         this.xOffset = cursorX - (this.rectWidth - 7.0F);
      }

   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      }

   }
}
