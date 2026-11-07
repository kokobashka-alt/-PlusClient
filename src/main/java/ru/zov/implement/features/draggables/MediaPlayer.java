package ru.zov.implement.features.draggables;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.font.FontRenderer;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.other.BufferUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.other.StringUtil;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.implement.features.modules.render.Hud;

public class MediaPlayer extends AbstractDraggable {
   private final ExecutorService executor = Executors.newScheduledThreadPool(1);
   private MediaInfo mediaInfo = new MediaInfo("Название Трека", "Артист", new byte[0], 43L, 150L, false);
   private final Identifier artwork = Identifier.of("textures/xyu.png");
   private final StopWatch lastMedia = new StopWatch();
   public IMediaSession session;
   private float widthDuration;

   public static MediaPlayer getInstance() {
      return (MediaPlayer)Instance.getDraggable(MediaPlayer.class);
   }

   public MediaPlayer() {
      super("Media Player", 10, 400, 100, 40, true);
   }

   public boolean visible() {
      return !this.lastMedia.finished((double)2000.0F) || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   public void tick() {
      if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Media Player") && mc.player.age % 5 == 0) {
         this.executor.submit(() -> {
            this.session = (IMediaSession)MediaPlayerInfo.Instance.getMediaSessions().stream().max(Comparator.comparing((s) -> s.getMedia().getPlaying())).orElse(null);
            if (this.session != null) {
               MediaInfo info = this.session.getMedia();
               if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty()) {
                  if (this.mediaInfo.getTitle().equals("Название Трека") || !Arrays.toString(this.mediaInfo.getArtworkPng()).equals(Arrays.toString(info.getArtworkPng()))) {
                     BufferUtil.registerTexture(this.artwork, info.getArtworkPng());
                  }

                  this.mediaInfo = info;
                  this.lastMedia.reset();
               }
            }

         });
      }

   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      ScissorManager scissor = main.getScissorManager();
      FontRenderer big = Fonts.getSize(15, Fonts.Type.DEFAULT);
      FontRenderer time = Fonts.getSize(11, Fonts.Type.DEFAULT);
      int sizeArtwork = 32;
      int sizePausePlay = 4;
      int maxDurationWidth = this.getWidth() - (sizeArtwork + 12);
      int duration = (int)this.mediaInfo.getDuration();
      int position = MathHelper.clamp((int)this.mediaInfo.getPosition(), 0, duration);
      String timeDuration = StringUtil.getDuration(duration);
      this.widthDuration = MathHelper.clamp(MathUtil.interpolateSmooth((double)1.0F, this.widthDuration, (float)Math.round((float)position / (float)duration * (float)maxDurationWidth)), 1.0F, (float)maxDurationWidth);
      blur.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)this.getHeight()).thickness(2.25F).softness(1.0F).round(4.0F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
      scissor.push(matrix, (float)(this.getX() + sizeArtwork + 8), (float)this.getY(), (float)(this.getWidth() - sizeArtwork - 10), (float)this.getHeight());
      big.drawStringWithScroll(matrix, this.mediaInfo.getTitle(), (double)(this.getX() + sizeArtwork + 8), (double)(this.getY() + 7), 56.0F, ColorUtil.getText());
      Fonts.getSize(12, Fonts.Type.DEFAULT).drawStringWithScroll(matrix, this.mediaInfo.getArtist(), (double)(this.getX() + sizeArtwork + 8), (double)((float)this.getY() + 15.5F), 56.0F, ColorUtil.getText(0.75F));
      scissor.pop();
      Render2DUtil.drawTexture(matrix, this.artwork, (float)(this.getX() + 4), (float)(this.getY() + 4), (float)sizeArtwork, 3.0F, sizeArtwork, sizeArtwork, sizeArtwork, ColorUtil.getRect(1.0F));
      time.drawString(matrix, StringUtil.getDuration(position), (double)(this.getX() + 8 + sizeArtwork), (double)(this.getY() + 28), ColorUtil.getText());
      time.drawString(matrix, timeDuration, (double)((float)(this.getX() + this.getWidth() - 4) - time.getStringWidth(timeDuration)), (double)(this.getY() + 28), ColorUtil.getText());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 8 + sizeArtwork), (double)(this.getY() + this.getHeight() - 8), (double)maxDurationWidth, (double)2.0F).round(0.75F).color(ColorUtil.getRectDarker(0.75F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 8 + sizeArtwork), (double)(this.getY() + this.getHeight() - 8), (double)this.widthDuration, (double)2.0F).softness(4.0F).round(1.0F).color(ColorUtil.roundClientColor(0.2F)).build());
      rectangle.render(ShapeProperties.create(matrix, (double)(this.getX() + 8 + sizeArtwork), (double)(this.getY() + this.getHeight() - 8), (double)this.widthDuration, (double)2.0F).round(0.75F).color(ColorUtil.roundClientColor(1.0F)).build());
      Render2DUtil.drawTexture(matrix, Identifier.of("textures/" + (this.mediaInfo.getPlaying() ? "pause" : "play") + ".png"), (float)((double)this.getX() + (double)(this.getWidth() + sizeArtwork + 4 - sizePausePlay) / (double)2.0F), (float)(this.getY() + 26), sizePausePlay);
   }
}
