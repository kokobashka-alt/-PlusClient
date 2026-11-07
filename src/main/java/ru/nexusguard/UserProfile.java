package ru.nexusguard;

public class UserProfile implements IGuard {
   public static String username = "@y4kudaw";
   public static String hwid = "123";
   public static int uid = 1;
   public static String role = "DEV+";

   @Override
   public String username() {
      return username;
   }

   @Override
   public String hwid() {
      return hwid;
   }

   @Override
   public String role() {
      return role;
   }

   @Override
   public int uid() {
      return uid;
   }

   @Override
   public String roleName() {
      return switch (role) {
         case "DEV+", "DEV" -> "Разработчик";
         case "ADMIN" -> "Администратор";
         case "MEDIA" -> "Медиа";
         case "BETA" -> "Бета";
         default -> "Пользователь";
      };
   }
}
