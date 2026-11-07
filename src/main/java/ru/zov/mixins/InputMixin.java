package ru.zov.mixins;

import net.minecraft.client.input.Input;
import org.spongepowered.asm.mixin.Mixin;
import ru.zov.common.QuickImports;

@Mixin({Input.class})
public class InputMixin implements QuickImports {
}
