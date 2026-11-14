package ru.zov.implement.features.modules.misc;

import net.minecraft.entity.LivingEntity;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.common.util.other.Instance;

public class HPFixer extends Module {
    public static HPFixer getInstance() {
        return (HPFixer) Instance.get(HPFixer.class);
    }

    public HPFixer() {
        super("HPFixer", "HP из скорборда/ника для TargetHud", ModuleCategory.MISC);
    }

    public static float getHealth(LivingEntity entity) {
        HPFixer self = getInstance();
        return HpReader.health(entity, self != null && self.isState());
    }

    public static float getMaxHealth(LivingEntity entity) {
        return entity == null ? 0f : entity.getMaxHealth();
    }

    public static Float parseHealthValue(String raw) {
        return HpParser.parse(raw);
    }
}
