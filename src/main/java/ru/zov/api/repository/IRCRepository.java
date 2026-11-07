package ru.zov.api.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import ru.nexusguard.UserProfile;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventManager;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.StopWatch;

public class IRCRepository implements QuickImports {
   public final StopWatch stopWatch = new StopWatch();
   public final StopWatch message = new StopWatch();
   private static volatile boolean running = false;
   private static volatile WebSocketClient socket;

   public IRCRepository(EventManager manager) {
      manager.register(this);
   }

   @Native
   public void init() {
      if (!running) {
         running = true;
         Thread supervisorThread = new Thread(this::supervisorLoop, "IRC-WS");
         supervisorThread.setDaemon(true);
         supervisorThread.start();
      }
   }

   @Native
   public void shutdown() {
      running = false;

      try {
         if (socket != null) {
            socket.close();
         }
      } catch (Exception var2) {
      }

      socket = null;
   }

   @Native
   public void send(String text) {
      if (socket != null && socket.isOpen()) {
         try {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "message");
            obj.addProperty("username", UserProfile.username);
            obj.addProperty("role", UserProfile.role);
            obj.addProperty("message", text);
            obj.addProperty("signature", this.signature());
            socket.send(obj.toString());
         } catch (Exception e) {
            this.addMessage("Send failed: " + e.getMessage());
            socket = null;
         }

         this.message.reset();
      } else {
         this.addMessage("Cannot send, socket not open");
      }
   }

   @Native
   private void tryConnectBlocking() {
      try {
         this.addMessage("Connecting to server");
         WebSocketClient client = new WebSocketClient(new URI("ws://c3.yukicore.org:25009")) {
            @Override
            public void onOpen(ServerHandshake data) {
               IRCRepository.this.addMessage("Server connected");
               IRCRepository.this.stopWatch.reset();
            }

            @Override
            public void onMessage(String message) {
               IRCRepository.this.handleIncoming(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
               IRCRepository.this.addMessage("Server closed: " + code + "\nReason: " + reason);
               IRCRepository.socket = null;
            }

            @Override
            public void onError(Exception e) {
               IRCRepository.this.addMessage("Server error: " + e.getMessage());
               IRCRepository.socket = null;
            }
         };
         client.connectBlocking();
         if (client.isOpen()) {
            socket = client;
         } else {
            socket = null;
            this.addMessage("Failed to connect");
         }
      } catch (Exception e) {
         socket = null;
         this.addMessage("Exception while connecting: " + e.getMessage());
      }
   }

   @Native
   private void handleIncoming(String message) {
      try {
         JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
         if (obj.has("type")) {
            switch (obj.get("type").getAsString()) {
               case "message":
                  String name = obj.get("username").getAsString();
                  String role = obj.get("role").getAsString();
                  String msg = obj.get("message").getAsString();
                  this.direct(Formatting.WHITE + name, role, msg);
                  break;
               case "error": {
                  String text = obj.get("error").getAsString();
                  this.addMessage(text);
                  break;
               }
               case "system": {
                  String text = obj.get("text").getAsString();
                  this.addMessage(text);
               }
            }
         }
      } catch (Exception e) {
         this.addMessage("Failed to handle message: " + e.getMessage());
      }
   }

   @Native
   private void sendPing() {
      if (socket != null && socket.isOpen()) {
         try {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "ping");
            obj.addProperty("username", UserProfile.username);
            obj.addProperty("role", UserProfile.role);
            obj.addProperty("signature", this.signature());
            socket.send(obj.toString());
         } catch (Exception e) {
            this.addMessage("Ping failed: " + e.getMessage());
            socket = null;
         }
      }
   }

   @Native
   private void supervisorLoop() {
      while (running) {
         try {
            if (socket == null || !socket.isOpen()) {
               this.tryConnectBlocking();
            } else if (this.stopWatch.every(10000.0)) {
               this.sendPing();
            }

            Thread.sleep(5000L);
         } catch (InterruptedException var2) {
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   @Native
   private String sha256(String s) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
         StringBuilder out = new StringBuilder();

         for (byte b : hash) {
            out.append(String.format("%02x", b));
         }

         return out.toString();
      } catch (Exception e) {
         return "";
      }
   }

   @Native
   private String signature() {
      return this.sha256(UserProfile.username + UserProfile.role + "sosal_raz_dva_da");
   }

   @Native
   private void direct(String name, String role, String msg) {
      switch (role) {
         case "ADMIN":
            this.addMessage(this.addPrefix(role, ColorUtil.RED, ColorUtil.LIGHT_RED).append(Text.literal(name + Formatting.RESET + " -> " + msg)));
            break;
         case "BETA":
            this.addMessage(
               this.addPrefix(role, ColorUtil.getColor(170, 55, 255), ColorUtil.getColor(170, 110, 255))
                  .append(Text.literal(name + Formatting.RESET + " -> " + msg))
            );
            break;
         case "ALPHA":
            this.addMessage(
               this.addPrefix(role, ColorUtil.getColor(120, 120, 255), ColorUtil.getColor(160, 160, 255))
                  .append(Text.literal(name + Formatting.RESET + " -> " + msg))
            );
            break;
         default:
            this.addMessage(Text.literal(name + Formatting.RESET + " -> " + msg));
      }
   }

   @Native
   private MutableText addPrefix(String role, int start, int end) {
      MutableText text = Text.literal("");
      text.append(Formatting.WHITE + "[");
      text.append(ColorUtil.text2Gradient(role, start, end));
      text.append(Formatting.WHITE + "] " + Formatting.RESET);
      return text;
   }

   @Native
   private void addMessage(String text) {
      this.addMessage(Text.literal(text));
   }

   @Native
   private void addMessage(Text text) {
      mc.inGameHud
         .getChatHud()
         .addMessage(
            Text.literal(Formatting.WHITE + "[I" + Formatting.BLUE + "R" + Formatting.RED + "C" + Formatting.WHITE + "] ")
               .append(text)
               .formatted(Formatting.GRAY)
         );
   }
}
