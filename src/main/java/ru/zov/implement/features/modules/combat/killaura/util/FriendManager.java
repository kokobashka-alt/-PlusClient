package ru.zov.implement.features.modules.combat.killaura.util;

public final class FriendManager {
    public static boolean isFriend(String name) {
        return ru.zov.api.repository.FriendUtils.isFriend(name);
    }

    private FriendManager() {
    }
}
