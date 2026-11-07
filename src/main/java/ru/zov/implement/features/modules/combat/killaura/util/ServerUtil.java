package ru.zov.implement.features.modules.combat.killaura.util;

public final class ServerUtil {
    public static float TPS = 20.0F;

    public static void sync() {
        TPS = ru.zov.common.util.world.ServerUtil.TPS;
    }

    public static int getAnarchy() {
        return ru.zov.common.util.world.ServerUtil.getAnarchy();
    }

    public static int getProtocolVersion() {
        return ru.zov.common.util.entity.PlayerIntersectionUtil.getProtocolVersion();
    }

    public static String server() {
        return ru.zov.common.util.world.ServerUtil.server;
    }

    private ServerUtil() {
    }
}
