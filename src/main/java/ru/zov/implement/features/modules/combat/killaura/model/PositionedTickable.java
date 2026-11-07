package ru.zov.implement.features.modules.combat.killaura.model;

import net.minecraft.util.math.Vec3d;

public interface PositionedTickable {
    Vec3d pos();

    void tick();
}
