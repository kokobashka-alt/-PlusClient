package ru.zov.api.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import ru.nexusguard.UserProfile;
import ru.zov.api.event.EventHandler;
import ru.zov.api.event.EventManager;
import ru.zov.api.feature.command.argument.IArgConsumer;
import ru.zov.api.feature.command.exception.CommandNotEnoughArgumentsException;
import ru.zov.common.QuickImports;
import ru.zov.common.util.math.StopWatch;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.player.TickEvent;

public class PartyRepository implements QuickImports {
   private static final String SERVER = "ws://c3.yukicore.org:25023";
   private static final String SECRET = "sosal_raz_dva_da_party";
   private final StopWatch stopWatch = new StopWatch();
   private static WebSocketClient socket;
   private static boolean running = false;
   public boolean isParty = false;
   public boolean isOwner = false;
   public String partyId = null;

   public PartyRepository(EventManager manager) {
      manager.register(this);
   }

   public void init() {
      if (!running) {
         running = true;
         Thread t = new Thread(this::loop, "Party-WS");
         t.setDaemon(true);
         t.start();
      }
   }

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

   private String signature() {
      return this.sha256(UserProfile.username + UserProfile.role + "sosal_raz_dva_da_party");
   }

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

   private void loop() {
      while (running) {
         try {
            if (socket == null || !socket.isOpen()) {
               this.connectBlocking();
            } else if (this.stopWatch.every(10000.0)) {
               this.sendPing();
            }

            Thread.sleep(3000L);
         } catch (Exception var2) {
         }
      }
   }

   private void connectBlocking() {
      try {
         socket = new WebSocketClient(new URI("ws://c3.yukicore.org:25023")) {
            @Override
            public void onOpen(ServerHandshake handshake) {
               PartyRepository.this.directSystem("Connected to party server");
               PartyRepository.this.stopWatch.reset();
            }

            @Override
            public void onMessage(String raw) {
               PartyRepository.this.handle(raw);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
               PartyRepository.this.directSystem("Connection closed: " + reason);
               PartyRepository.socket = null;
            }

            @Override
            public void onError(Exception ex) {
               PartyRepository.this.directSystem("Error: " + ex.getMessage());
               PartyRepository.socket = null;
            }
         };
         socket.connectBlocking();
      } catch (Exception e) {
         this.directSystem("Connect failed: " + e.getMessage());
         socket = null;
      }
   }

   private void send(JsonObject o) {
      try {
         if (socket != null && socket.isOpen()) {
            socket.send(o.toString());
         }
      } catch (Exception e) {
         this.directSystem("Send failed: " + e.getMessage());
      }
   }

   private void sendPing() {
      JsonObject o = this.base("ping");
      this.send(o);
   }

   private JsonObject base(String type) {
      JsonObject o = new JsonObject();
      o.addProperty("type", type);
      o.addProperty("username", UserProfile.username);
      o.addProperty("role", UserProfile.role);
      o.addProperty("signature", this.signature());
      return o;
   }

   public void createParty(IArgConsumer args) {
      JsonObject o = this.base("create");
      this.send(o);
   }

   public void joinParty(IArgConsumer args) throws CommandNotEnoughArgumentsException {
      JsonObject o = this.base("join");
      o.addProperty("partyId", args.getString());
      this.send(o);
   }

   public void deleteParty(IArgConsumer args) {
      JsonObject o = this.base("delete");
      this.send(o);
   }

   public void removePersonFromParty(IArgConsumer args) throws CommandNotEnoughArgumentsException {
      JsonObject o = this.base("kick");
      o.addProperty("target", args.getString());
      this.send(o);
   }

   public void acceptJoinToParty(IArgConsumer args) throws CommandNotEnoughArgumentsException {
      JsonObject o = this.base("accept");
      o.addProperty("target", args.getString());
      this.send(o);
   }

   public void membersInParty(IArgConsumer args) {
      JsonObject o = this.base("members");
      this.send(o);
   }

   public void leaveFromParty(IArgConsumer args) {
      JsonObject o = this.base("leave");
      this.send(o);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.isParty && socket != null && socket.isOpen()) {
         JsonObject state = new JsonObject();
         state.addProperty("x", mc.player.getX());
         state.addProperty("y", mc.player.getY());
         state.addProperty("z", mc.player.getZ());
         state.addProperty("hp", mc.player.getHealth() + mc.player.getAbsorptionAmount());
         state.addProperty("an", ServerUtil.getAnarchy());
         state.addProperty("world", ServerUtil.getWorldType());
         state.addProperty("server", ServerUtil.server);
         JsonObject o = this.base("update");
         o.add("state", state);
         this.send(o);
      }
   }

   public void wayParty(IArgConsumer args) {
      JsonObject o = this.base("way");
      o.addProperty("server", ServerUtil.server);
      o.addProperty("world", ServerUtil.getWorldType());
      o.addProperty("an", ServerUtil.getAnarchy());
      o.addProperty("x", mc.player.getX());
      o.addProperty("y", mc.player.getY());
      o.addProperty("z", mc.player.getZ());
      this.send(o);
   }

   private void handle(String raw) {
      try {
         JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
         String type = o.get("type").getAsString();
         switch (type) {
            case "system":
               this.directSystem(o.get("text").getAsString());
               break;
            case "error":
               this.directError(UserProfile.username, o.get("text").getAsString());
               break;
            case "message":
               this.directMsg(o.get("username").getAsString(), o.get("text").getAsString());
               break;
            case "state":
               this.isParty = o.get("isParty").getAsBoolean();
               this.isOwner = o.get("isOwner").getAsBoolean();
               if (o.get("partyId").isJsonNull()) {
                  this.partyId = null;
               } else {
                  this.partyId = o.get("partyId").getAsString();
               }
               break;
            case "way":
               this.handleWaypointPacket(o);
               break;
            case "party_update":
               this.handlePartyUpdatePacket(o);
         }
      } catch (Exception e) {
         this.directSystem("Handle error: " + e.getMessage());
      }
   }

   private void handleWaypointPacket(JsonObject o) {
      JsonObject wp = o.getAsJsonObject("waypoint");
      int id = wp.get("id").getAsInt();
      int an = wp.get("an").getAsInt();
      double x = wp.get("x").getAsDouble();
      double y = wp.get("y").getAsDouble();
      double z = wp.get("z").getAsDouble();
      String owner = o.get("username").getAsString();
   }

   private void handlePartyUpdatePacket(JsonObject o) {
      String from = o.get("username").getAsString();
      JsonObject state = o.getAsJsonObject("state");
   }

   private void directError(String name, String msg) {
      this.addMessage(Text.literal(Formatting.RED + "ERROR " + Formatting.WHITE + name + ": " + msg));
   }

   private void directMsg(String name, String msg) {
      this.addMessage(Text.literal(Formatting.WHITE + name + Formatting.GRAY + ": " + msg));
   }

   public void directSystem(String msg) {
      this.addMessage(Text.literal(Formatting.YELLOW + msg));
   }

   private void addMessage(Text text) {
      mc.inGameHud
         .getChatHud()
         .addMessage(Text.literal(Formatting.WHITE + "[P" + Formatting.BLUE + "AR" + Formatting.RED + "TY" + Formatting.WHITE + "] ").append(text));
   }
}
