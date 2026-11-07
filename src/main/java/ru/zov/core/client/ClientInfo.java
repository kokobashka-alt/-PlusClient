package ru.zov.core.client;

import java.io.File;
import ru.nexusguard.UserProfile;

public record ClientInfo(String clientName, String userName, String role, File clientDir, File filesDir, File configsDir) implements ClientInfoProvider {
   @Override
   public String getFullInfo() {
      return String.format("Welcome! Client: %s User: %s Version: %s", this.clientName, UserProfile.username, UserProfile.role);
   }
}
