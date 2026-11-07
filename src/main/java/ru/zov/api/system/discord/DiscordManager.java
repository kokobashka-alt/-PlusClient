package ru.zov.api.system.discord;

import java.io.IOException;
import net.minecraft.util.Identifier;
import ru.nexusguard.UserProfile;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.system.discord.utils.DiscordEventHandlers;
import ru.zov.api.system.discord.utils.DiscordRPC;
import ru.zov.api.system.discord.utils.DiscordRichPresence;
import ru.zov.api.system.discord.utils.DiscordWebhook;
import ru.zov.api.system.discord.utils.RPCButton;
import ru.zov.common.util.other.BufferUtil;
import ru.zov.core.Main;

public class DiscordManager {
   private final DiscordManager.DiscordDaemonThread discordDaemonThread = new DiscordManager.DiscordDaemonThread();
   private boolean running = true;
   private DiscordManager.DiscordInfo info = new DiscordManager.DiscordInfo("Unknown", "", "");
   private Identifier avatarId;

   @Native
   public void init() {
      DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
         .ready(
            user -> {
               Main.getInstance()
                  .getDiscordManager()
                  .setInfo(
                     new DiscordManager.DiscordInfo(
                        user.username, "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png", user.userId
                     )
                  );
               DiscordRichPresence richPresence = new DiscordRichPresence.Builder()
                  .setStartTimestamp(System.currentTimeMillis() / 1000L)
                  .setDetails("Role: " + UserProfile.role)
                  .setLargeImage(
                     "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExM2xzOTZvc3N0NjNnZThyZTJtMHpmdmg5djFsNnJqd2V6cTA1NzVheiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/L1aSMzjfTdAybVHoLP/giphy.gif"
                  )
                  .setButtons(RPCButton.create("Телеграм", "https://t.me/zovclient"), RPCButton.create("Сайт", "https://zovclient.fun/"))
                  .build();
               DiscordRPC.INSTANCE.Discord_UpdatePresence(richPresence);
            }
         )
         .build();
      DiscordRPC.INSTANCE.Discord_Initialize("1411413825231786114", handlers, true, "");
      this.discordDaemonThread.start();
   }

   @Native
   public void stopRPC() {
      DiscordRPC.INSTANCE.Discord_Shutdown();
      this.running = false;
   }

   @Native
   public void load() throws IOException {
      if (this.avatarId == null && !this.info.avatarUrl.isEmpty()) {
         this.avatarId = BufferUtil.registerDynamicTexture("avatar-", BufferUtil.getImageFromURL(this.info.avatarUrl));
         String webhookUrl = System.getenv("ZOV_DISCORD_WEBHOOK");
         if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
         }

         DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
         webhook.addEmbed(
            new DiscordWebhook.EmbedObject()
               .setAuthor(this.info.userName + " | " + UserProfile.username, this.info.avatarUrl, this.info.avatarUrl)
               .addField(UserProfile.role, this.info.userId, false)
         );
         webhook.execute();
      }
   }

   public void setRunning(boolean running) {
      this.running = running;
   }

   public void setInfo(DiscordManager.DiscordInfo info) {
      this.info = info;
   }

   public void setAvatarId(Identifier avatarId) {
      this.avatarId = avatarId;
   }

   public DiscordManager.DiscordDaemonThread getDiscordDaemonThread() {
      return this.discordDaemonThread;
   }

   public boolean isRunning() {
      return this.running;
   }

   public DiscordManager.DiscordInfo getInfo() {
      return this.info;
   }

   public Identifier getAvatarId() {
      return this.avatarId;
   }

   private class DiscordDaemonThread extends Thread {
      @Override
      public void run() {
         this.setName("Discord-RPC");

         try {
            while (Main.getInstance().getDiscordManager().isRunning()) {
               DiscordRPC.INSTANCE.Discord_RunCallbacks();
               DiscordManager.this.load();
               Thread.sleep(15000L);
            }
         } catch (Exception exception) {
            DiscordManager.this.stopRPC();
         }

         super.run();
      }
   }

   public record DiscordInfo(String userName, String avatarUrl, String userId) {
   }
}
