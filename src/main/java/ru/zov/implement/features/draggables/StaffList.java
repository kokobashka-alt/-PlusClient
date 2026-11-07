package ru.zov.implement.features.draggables;

import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.animation.Animation;
import ru.zov.api.system.animation.Direction;
import ru.zov.api.system.animation.implement.DecelerateAnimation;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.implement.features.modules.render.Hud;

public class StaffList extends AbstractDraggable {
   public final Map<PlayerListEntry, Animation> list = new HashMap();
   private final List<String> staffPrefix = List.of("helper", "moder", "staff", "admin", "curator", "стажёр", "staff", "сотрудник", "помощник", "админ", "модер", "спектатор");

   public static StaffList getInstance() {
      return (StaffList)Instance.getDraggable(StaffList.class);
   }

   public StaffList() {
      super("Staff List", 130, 40, 80, 23, true);
   }

   public boolean visible() {
      return !this.list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   public void tick() {
      Collection<PlayerListEntry> playerList = ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).networkHandler.getPlayerList();

      for(PlayerListEntry entry : playerList) {
         GameProfile profile = entry.getProfile();
         Text displayName = entry.getDisplayName();
         Team scoreboard = entry.getScoreboardTeam();
         if (displayName != null && profile != null && scoreboard != null) {
            String prefix = scoreboard.getPrefix().getString();
            if (prefix.length() >= 5) {
               PlayerListEntry player = new PlayerListEntry(profile, false);
               player.setDisplayName(displayName);
               if (this.list.keySet().stream().noneMatch((p) -> Objects.equals(p.getDisplayName(), displayName))) {
                  this.staffPrefix.stream().filter((s) -> prefix.toLowerCase().contains(s)).findFirst().ifPresent((s) -> {
                     this.list.put(player, (new DecelerateAnimation()).setMs(150).setValue((double)1.0F));
                     if (Hud.getInstance().notificationSettings.isSelected("Staff Join")) {
                        Notifications.getInstance().addList((Text)Text.empty().append(displayName).append(" - Зашел на сервер!"), 5000L);
                     }

                  });
               }
            }
         }
      }

      this.list.entrySet().stream().filter((s) -> playerList.stream().noneMatch((p) -> Objects.equals(((PlayerListEntry)s.getKey()).getDisplayName(), p.getDisplayName()))).forEach((s) -> ((Animation)s.getValue()).setDirection(Direction.BACKWARDS));
      this.list.values().removeIf((s) -> s.isFinished(Direction.BACKWARDS));
   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
      FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.DEFAULT);
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)17.5F).round(4.0F, 0.0F, 4.0F, 0.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)(this.getY() + 17), (double)this.getWidth(), (double)(this.getHeight() - 17)).round(0.0F, 4.0F, 0.0F, 4.0F).softness(1.0F).thickness(2.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      float centerX = (float)this.getX() + (float)this.getWidth() / 2.0F;
      font.drawCenteredString(matrix, this.getName(), (double)centerX, (double)(this.getY() + 7), ColorUtil.getText());
      int offset = 23;
      int maxWidth = 80;

      for(Map.Entry<PlayerListEntry, Animation> staff : this.list.entrySet()) {
         PlayerListEntry player = (PlayerListEntry)staff.getKey();
         if (player != null) {
            Text text = player.getDisplayName();
            float centerY = (float)(this.getY() + offset);
            float width = fontPlayer.getStringWidth(text) + 25.0F;
            float animation = ((Animation)staff.getValue()).getOutput().floatValue();
            MathUtil.scale(matrix, centerX, centerY, 1.0F, animation, () -> {
               Render2DUtil.drawTexture(matrix, player.getSkinTextures().body().texturePath(), (float)(this.getX() + 5), centerY - 1.0F, 7.0F, 2.0F, 8, 8, 64, ColorUtil.getRect(1.0F));
               rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() + 15.5F), (double)(centerY - 1.0F), (double)0.5F, (double)7.0F).color(ColorUtil.getOutline(1.0F, 0.5F)).build());
               fontPlayer.drawText(matrix, text, (double)(this.getX() + 20), (double)(centerY + 1.0F));
            });
            offset += (int)(11.0F * animation);
            maxWidth = (int)Math.max(width, (float)maxWidth);
         }
      }

      this.setWidth(maxWidth);
      this.setHeight(offset);
   }
}
