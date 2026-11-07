package ru.zov.api.repository;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import ru.nexusguard.protection.annotations.Native;

public final class FriendUtils {
   public static final List<String> friends = new ArrayList<>();

   @Native
   public static void addFriend(PlayerEntity player) {
      addFriend(player.getName().getString());
   }

   @Native
   public static void addFriend(String name) {
      friends.add(name);
   }

   @Native
   public static void removeFriend(PlayerEntity player) {
      removeFriend(player.getName().getString());
   }

   @Native
   public static void removeFriend(String name) {
      friends.removeIf(friend -> friend.equalsIgnoreCase(name));
   }

   public static boolean isFriend(Entity entity) {
      return entity instanceof PlayerEntity player ? isFriend(player.getName().getString()) : false;
   }

   public static boolean isFriend(String name) {
      return friends.stream().anyMatch(friend -> friend.equals(name));
   }

   @Native
   public static void clear() {
      friends.clear();
   }

   private FriendUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static List<String> getFriends() {
      return friends;
   }
}
