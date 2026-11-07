package ru.zov.implement.features.modules.combat.killaura;

import ru.zov.implement.features.modules.combat.killaura.model.Translation;

public final class Lang {
    private static Translation text(String value) {
        return Translation.clearText(value);
    }

    public static final Translation ATTACKAURA_ROTATION_MODE = text("Rotation Mode");
    public static final Translation ATTACKAURA_ROTATION_MODE_FUNTIME = text("FunTime");
    public static final Translation ATTACKAURA_ROTATION_MODE_SPOOKYTIME = text("SpookyTime");
    public static final Translation ATTACKAURA_ROTATION_MODE_GRIM = text("Grim");
    public static final Translation ATTACKAURA_ROTATION_MODE_HOLYWORLD = text("HolyWorld");
    public static final Translation SELECTTARGETS = text("Select Target");
    public static final Translation SELECTTARGETS_PLAYERS = text("Players");
    public static final Translation SELECTTARGETS_MOBS = text("Mobs");
    public static final Translation SELECTTARGETS_ANIMALS = text("Animals");
    public static final Translation SELECTTARGETS_FRIENDS = text("Friends");
    public static final Translation ATTACKAURA_SORT_TYPE = text("Target Sort");
    public static final Translation ATTACKAURA_SORT_BY_FOV = text("By FOV");
    public static final Translation ATTACKAURA_SORT_BY_HEALTH = text("By Health");
    public static final Translation ATTACKAURA_SORT_BY_DISTANCE = text("By Distance");
    public static final Translation ATTACKAURA_MOVE_CORRECTION = text("Move Correction");
    public static final Translation ATTACKAURA_MOVE_CORRECTION_DESC = text("Correct movement based on view");
    public static final Translation ATTACKAURA_MOVE_CORRECTION_NONE = text("No correction");
    public static final Translation ATTACKAURA_MOVE_CORRECTION_FREE = text("Free");
    public static final Translation ATTACKAURA_MOVE_CORRECTION_TARGET = text("Target");
    public static final Translation ATTACKAURA_MOVE_CORRECTION_FOCUS = text("Focus");
    public static final Translation ATTACKAURA_ROTATION_RETURN = text("Rotation Return");
    public static final Translation ATTACKAURA_ROTATION_RETURN_DESC = text("Return rotation after losing the target");
    public static final Translation ATTACKAURA_EXTRA_DISTANCE = text("Pre-distance");
    public static final Translation ATTACKAURA_EXTRA_DISTANCE_DESC = text("Additional target selection and aiming distance");
    public static final Translation ATTACKAURA_MAX_DISTANCE = text("Max Distance");
    public static final Translation ATTACKAURA_AUTO_MACE = text("Auto Mace");
    public static final Translation ATTACKAURA_AUTO_MACE_DESC = text("Prepare the mace above a target and restore the previous slot");
    public static final Translation ATTACKAURA_AUTO_MACE_HEIGHT = text("Mace Height");
    public static final Translation ATTACKAURA_AUTO_MACE_HEIGHT_DESC = text("Minimum height before preparing the mace");
    public static final Translation ATTACKAURA_MAX_DAMAGE = text("Max Damage");
    public static final Translation ATTACKAURA_MAX_DAMAGE_DESC = text("Temporarily equip a damage item for a real critical");
    public static final Translation ATTACKAURA_MAX_DAMAGE_ITEM = text("Damage Item");
    public static final Translation ATTACKAURA_MAX_DAMAGE_ITEM_PUNISHER = text("Punisher Talisman");
    public static final Translation ATTACKAURA_MAX_DAMAGE_ITEM_ARES = text("Ares Sphere");
    public static final Translation ATTACKAURA_MAX_DAMAGE_ITEM_RAGE = text("Rage Talisman");
    public static final Translation ATTACKAURA_MAX_DAMAGE_ITEM_DISCORD = text("Discord Talisman");
    public static final Translation ATTACKAURA_MAX_DAMAGE_ITEM_TOTEM = text("Regular Totem");
    public static final Translation TRIGGERBOT_ONLY_CRITICAL_HIT = text("Only Critical Hit");
    public static final Translation ATTACKAURA_CRITS_WITH_SPACE = text("Smart Critical Hit");
    public static final Translation ATTACKAURA_CRITS_WITH_SPACE_DESC = text("Use sweep attacks when critical hits are impossible");
    public static final Translation ATTACKAURA_KEEP_SPRINT_WHEN_POSSIBLE = text("Keep Sprint When Possible");
    public static final Translation ATTACKAURA_KEEP_SPRINT_WHEN_POSSIBLE_DESC = text("Stop sprinting only for a real critical hit");
    public static final Translation ATTACKAURA_IGNORE_WALLS = text("Ignore Walls");
    public static final Translation ATTACKAURA_IGNORE_WALLS_DESC = text("Ignore walls during raytrace checks");
    public static final Translation ATTACKAURA_IGNORE_NAKED = text("Ignore Naked");
    public static final Translation ATTACKAURA_NO_HIT_WHILE_EATING_OFFHAND = text("Don't hit while eating");
    public static final Translation ATTACKAURA_UNPRESS_SHIELD = text("Unpress Shield");
    public static final Translation ATTACKAURA_BREAK_SHIELD = text("Break Shield");
    public static final Translation ATTACKAURA_TPS_SYNC = text("TPS Sync");
    public static final Translation UNIT_UNITS = text("units");
    public static final Translation UNIT_BLOCKS = text("blocks");
    public static final Translation UNIT_TICKS = text("ticks");
    public static final Translation UNIT_MILLISECONDS = text("ms");
    public static final Translation UNIT_SECONDS = text("sec");
    public static final Translation UNIT_HITPOINTS = text("hp");
    public static final Translation UNIT_PERCENTS = text("%");
    public static final Translation UNIT_DEGREES = text("deg");
    public static final Translation UNIT_PIXELS = text("px");
    public static final Translation SWAP_METHOD_AUTO = text("Auto");
    public static final Translation SWAP_METHOD_VANILLA = text("Vanilla");
    public static final Translation SWAP_METHOD_GRIM = text("Grim");
    public static final Translation SWAP_METHOD_DELAYED = text("Delayed");
    public static final Translation SWAP_METHOD_SEQUENTIAL = text("Sequential");

    private Lang() {
    }
}
