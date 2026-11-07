package ru.zov.implement.features.modules.combat.killaura.ui.setting;

import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;
import java.util.Arrays;
import java.util.function.Supplier;

public class ModeSetting<T extends Enum<T>> extends ru.zov.api.feature.module.setting.implement.SelectSetting {
    private T[] options;
    private T currentValue;
    private Runnable action;

    public ModeSetting(Translation name, Translation description) {
        super(name.effective(), description == null ? "" : description.effective());
    }

    public ModeSetting(Translation name) {
        this(name, null);
    }

    public ModeSetting<T> values(Class<T> type) {
        options = type.getEnumConstants();
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("Enum class " + type.getSimpleName() + " has no constants.");
        }
        currentValue = options[0];
        value(Arrays.stream(options).map(this::displayName).toArray(String[]::new));
        selected(displayName(currentValue));
        return this;
    }

    @Override
    public ModeSetting<T> visible(Supplier<Boolean> visible) {
        super.visible(visible);
        return this;
    }

    public ModeSetting<T> select(T value) {
        if (!isValidOption(value)) {
            throw new IllegalArgumentException("Invalid enum value: " + value);
        }
        currentValue = value;
        selected(displayName(value));
        return this;
    }

    public boolean isSelected(T value) {
        syncFromUi();
        return currentValue == value;
    }

    public T currentValue() {
        syncFromUi();
        return currentValue;
    }

    public void setCurrentValue(T value) {
        select(value);
    }

    public T[] options() {
        return options;
    }

    public int selectedIndex() {
        syncFromUi();
        return Arrays.asList(options).indexOf(currentValue);
    }

    public ModeSetting<T> action(Runnable action) {
        this.action = action;
        return this;
    }

    public void startAction() {
        if (action != null) action.run();
    }

    private boolean isValidOption(T value) {
        return Arrays.asList(options).contains(value);
    }

    private void syncFromUi() {
        if (options == null) return;
        for (T option : options) {
            if (displayName(option).equalsIgnoreCase(getSelected())) {
                currentValue = option;
                return;
            }
        }
    }

    private String displayName(T value) {
        return value instanceof DisplayNamed named ? named.getDisplayName().effective() : value.name();
    }
}
