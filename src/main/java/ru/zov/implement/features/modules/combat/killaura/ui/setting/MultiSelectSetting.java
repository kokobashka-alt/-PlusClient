package ru.zov.implement.features.modules.combat.killaura.ui.setting;

import ru.zov.implement.features.modules.combat.killaura.model.DisplayNamed;
import ru.zov.implement.features.modules.combat.killaura.model.Translation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class MultiSelectSetting<T extends Enum<T>> extends ru.zov.api.feature.module.setting.implement.MultiSelectSetting {
    private final Set<T> selectedValues = new LinkedHashSet<>();
    private T[] options;

    public MultiSelectSetting(Translation name, Translation description) {
        super(name.effective(), description == null ? "" : description.effective());
    }

    public MultiSelectSetting(Translation name) {
        this(name, null);
    }

    public MultiSelectSetting<T> values(Class<T> type) {
        options = type.getEnumConstants();
        value(Arrays.stream(options).map(this::displayName).toArray(String[]::new));
        return this;
    }

    @Override
    public MultiSelectSetting<T> visible(Supplier<Boolean> visible) {
        super.visible(visible);
        return this;
    }

    @SafeVarargs
    public final MultiSelectSetting<T> select(T... values) {
        selectedValues.addAll(Arrays.asList(values));
        selected(selectedValues.stream().map(this::displayName).toArray(String[]::new));
        return this;
    }

    public boolean isSelected(T value) {
        syncFromUi();
        return selectedValues.contains(value);
    }

    public Set<T> selectedValues() {
        syncFromUi();
        return selectedValues;
    }

    public T[] options() {
        return options;
    }

    private void syncFromUi() {
        selectedValues.clear();
        if (options == null) return;
        for (T option : options) {
            if (getSelected().contains(displayName(option))) selectedValues.add(option);
        }
    }

    private String displayName(T value) {
        return value instanceof DisplayNamed named ? named.getDisplayName().effective() : value.name();
    }
}
