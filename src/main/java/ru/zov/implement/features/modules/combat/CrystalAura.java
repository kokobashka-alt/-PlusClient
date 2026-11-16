package ru.zov.implement.features.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.modules.combat.killaura.crystal.CrystalAuraCore;
import ru.zov.implement.features.modules.combat.killaura.crystal.McCrystalContext;

/**
 * Crystal Aura module. Two profiles (Legit / Rage) with independent presets.
 * All decision logic lives in {@link CrystalAuraCore}; all Yarn calls live in
 * {@link McCrystalContext}. This file only wires settings and forwards ticks.
 *
 * <p>Profile behaviour:
 * <ul>
 *   <li><b>Legit</b> — narrow FOV, long delays, per-tick pause chance, no
 *       SetDead / IDPredict / BasePlace by default. Rotations are smoothed
 *       toward the crystal, damage is gated on both sides, self-damage cap is
 *       strict.</li>
 *   <li><b>Rage</b> — wide FOV, short delays, instant rotations, optional
 *       BasePlace / SetDead / IDPredict. Still gated on anti-suicide and
 *       damage ratio; without those gates the module will kill you faster than
 *       the enemy.</li>
 * </ul>
 */
public class CrystalAura extends Module {

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    public final MultiSelectSetting profile = new MultiSelectSetting("Профиль", "Legit или Rage")
            .value("Legit", "Rage")
            .selected("Legit");

    // Ranges
    public final ValueSetting targetRange = new ValueSetting("Радиус цели", "Дистанция поиска игрока")
            .setValue(6.0f).range(3.0f, 12.0f);
    public final ValueSetting placeRange = new ValueSetting("Дальность установки", "На каком расстоянии ставить")
            .setValue(4.5f).range(2.0f, 6.0f);
    public final ValueSetting breakRange = new ValueSetting("Дальность взрыва", "На каком расстоянии взрывать")
            .setValue(4.5f).range(1.0f, 6.0f);
    public final ValueSetting fov = new ValueSetting("FOV", "Угол поиска, °")
            .setValue(55.0f).range(5.0f, 180.0f);
    public final ValueSetting minSelfDistance = new ValueSetting("Мин. дистанция до себя", "Не ставить ближе, блоков")
            .setValue(2.5f).range(0.0f, 8.0f);
    public final ValueSetting crystalTargetDistance = new ValueSetting("Кристалл↔цель", "Макс. дистанция кристалла до цели")
            .setValue(2.5f).range(0.5f, 6.0f);

    // Timings
    public final ValueSetting placeDelay = new ValueSetting("Задержка установки", "Мс между установками")
            .setValue(140.0f).range(0.0f, 1000.0f);
    public final ValueSetting breakDelay = new ValueSetting("Задержка взрыва", "Мс между взрывами")
            .setValue(110.0f).range(0.0f, 1000.0f);
    public final ValueSetting switchDelay = new ValueSetting("Задержка цели", "Мс после смены цели")
            .setValue(350.0f).range(0.0f, 2000.0f);
    public final ValueSetting basePlaceDelay = new ValueSetting("Задержка базы", "Мс между установкой базы и кристалла")
            .setValue(80.0f).range(20.0f, 500.0f);
    public final ValueSetting targetLockMs = new ValueSetting("Лок цели", "Мс удержания цели")
            .setValue(1200.0f).range(0.0f, 5000.0f);

    // Feature toggles
    public final BooleanSetting autoPlace = new BooleanSetting("Авто установка", "Ставить кристаллы")
            .setValue(true);
    public final BooleanSetting autoBreak = new BooleanSetting("Авто взрыв", "Взрывать кристаллы")
            .setValue(true);
    public final BooleanSetting basePlace = new BooleanSetting("BasePlace", "Ставить обсидиан под цель")
            .setValue(false);
    public final BooleanSetting setDead = new BooleanSetting("SetDead", "Клиентски удалять кристалл после удара")
            .setValue(false);
    public final BooleanSetting idPredict = new BooleanSetting("ID Predict", "Предсказывать ID кристалла")
            .setValue(false);
    public final BooleanSetting swing = new BooleanSetting("Размах руки", "Свинг после действия")
            .setValue(true);
    public final BooleanSetting restoreSlot = new BooleanSetting("Возвращать слот", "Возвращать слот после установки")
            .setValue(false);

    // Damage gates
    public final ValueSetting minEnemyDamage = new ValueSetting("Мин. урон по врагу", "Не ставить, если меньше")
            .setValue(3.0f).range(0.0f, 20.0f);
    public final ValueSetting maxSelfDamage = new ValueSetting("Макс. урон по себе", "Не ставить, если больше")
            .setValue(4.0f).range(0.0f, 20.0f);
    public final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide", "Не ставить смертельные кристаллы")
            .setValue(true);

    // Per-tick caps
    public final ValueSetting maxBreaksPerTick = new ValueSetting("Взрывов за тик", "Максимум")
            .setValue(1.0f).range(1.0f, 8.0f);
    public final ValueSetting maxPlacesPerTick = new ValueSetting("Установок за тик", "Максимум")
            .setValue(1.0f).range(1.0f, 8.0f);

    // Legit-only knobs
    public final ValueSetting legitPausePercent = new ValueSetting("Legit: пауза, %", "Пропуск действий")
            .setValue(5.0f).range(0.0f, 30.0f);

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private final CrystalAuraCore core = new CrystalAuraCore();
    private final McCrystalContext context = new McCrystalContext();

    private boolean lastRage = false;
    private CrystalAuraCore.Result state = CrystalAuraCore.Result.IDLE;

    public CrystalAura() {
        super("CrystalAura", "Автоматически ставит и взрывает кристаллы", ModuleCategory.COMBAT);
        this.setup(new Setting[] {
                profile,
                targetRange, placeRange, breakRange, fov, minSelfDistance, crystalTargetDistance,
                placeDelay, breakDelay, switchDelay, basePlaceDelay, targetLockMs,
                autoPlace, autoBreak, basePlace, setDead, idPredict, swing, restoreSlot,
                minEnemyDamage, maxSelfDamage, antiSuicide,
                maxBreaksPerTick, maxPlacesPerTick,
                legitPausePercent,
        });
    }

    public static CrystalAura getInstance() {
        return (CrystalAura) Instance.get(CrystalAura.class);
    }

    public PlayerEntity getTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        int id = this.core.targetId();
        if (client == null || client.world == null || id < 0) return null;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p != null && p.getId() == id) return p;
        }
        return null;
    }

    public CrystalAuraCore.Result getState() {
        return this.state;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void activate() {
        this.lastRage = this.profile.isSelected("Rage");
        this.applyRecommended();
        this.core.reset();
    }

    @Override
    public void deactivate() {
        this.core.reset();
        this.state = CrystalAuraCore.Result.IDLE;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    @EventHandler
    public void onTick(TickEvent e) {
        boolean rage = this.profile.isSelected("Rage");
        if (rage != this.lastRage) {
            this.lastRage = rage;
            this.applyRecommended();
        }

        CrystalAuraCore.Config cfg = this.core.config();
        cfg.rage = rage;
        cfg.targetRange = this.targetRange.getValue();
        cfg.placeRange = this.placeRange.getValue();
        cfg.breakRange = this.breakRange.getValue();
        cfg.fovDegrees = this.fov.getValue();
        cfg.minSelfDistance = this.minSelfDistance.getValue();
        cfg.maxCrystalTargetDistance = this.crystalTargetDistance.getValue();

        cfg.placeDelayMs = (long) this.placeDelay.getValue();
        cfg.breakDelayMs = (long) this.breakDelay.getValue();
        cfg.switchDelayMs = (long) this.switchDelay.getValue();
        cfg.basePlaceDelayMs = (long) this.basePlaceDelay.getValue();
        cfg.targetLockMs = (long) this.targetLockMs.getValue();

        cfg.autoPlace = this.autoPlace.isValue();
        cfg.autoBreak = this.autoBreak.isValue();
        cfg.basePlace = this.basePlace.isValue();
        cfg.setDead = this.setDead.isValue();
        cfg.idPredict = this.idPredict.isValue();
        cfg.swing = this.swing.isValue();
        cfg.restoreSlot = this.restoreSlot.isValue();

        cfg.minEnemyDamage = this.minEnemyDamage.getValue();
        cfg.maxSelfDamage = this.maxSelfDamage.getValue();
        cfg.antiSuicide = this.antiSuicide.isValue();

        cfg.maxCrystalsBrokenPerTick = Math.round(this.maxBreaksPerTick.getValue());
        cfg.maxCrystalsPlacedPerTick = Math.round(this.maxPlacesPerTick.getValue());
        cfg.legitPausePercent = Math.round(this.legitPausePercent.getValue());

        this.context.setRestoreSlot(this.restoreSlot.isValue());
        this.context.setRage(rage);

        this.state = this.core.tick(this.context);
    }

    // ------------------------------------------------------------------
    // Presets
    // ------------------------------------------------------------------

    private void applyRecommended() {
        if (this.profile.isSelected("Rage")) {
            this.targetRange.setValue(7.0f);
            this.placeRange.setValue(5.5f);
            this.breakRange.setValue(5.0f);
            this.placeDelay.setValue(60.0f);
            this.breakDelay.setValue(50.0f);
            this.switchDelay.setValue(80.0f);
            this.basePlaceDelay.setValue(40.0f);
            this.targetLockMs.setValue(600.0f);
            this.fov.setValue(180.0f);
            this.minSelfDistance.setValue(2.0f);
            this.crystalTargetDistance.setValue(3.0f);
            this.maxBreaksPerTick.setValue(2.0f);
            this.maxPlacesPerTick.setValue(2.0f);
            this.minEnemyDamage.setValue(2.0f);
            this.maxSelfDamage.setValue(5.0f);
            this.legitPausePercent.setValue(0.0f);
            this.basePlace.setValue(true);
            this.setDead.setValue(true);
            this.idPredict.setValue(true);
            this.restoreSlot.setValue(false);
        } else {
            this.targetRange.setValue(6.0f);
            this.placeRange.setValue(4.5f);
            this.breakRange.setValue(4.5f);
            this.placeDelay.setValue(140.0f);
            this.breakDelay.setValue(110.0f);
            this.switchDelay.setValue(350.0f);
            this.basePlaceDelay.setValue(80.0f);
            this.targetLockMs.setValue(1200.0f);
            this.fov.setValue(55.0f);
            this.minSelfDistance.setValue(2.5f);
            this.crystalTargetDistance.setValue(2.5f);
            this.maxBreaksPerTick.setValue(1.0f);
            this.maxPlacesPerTick.setValue(1.0f);
            this.minEnemyDamage.setValue(3.0f);
            this.maxSelfDamage.setValue(4.0f);
            this.legitPausePercent.setValue(5.0f);
            this.basePlace.setValue(false);
            this.setDead.setValue(false);
            this.idPredict.setValue(false);
            this.restoreSlot.setValue(false);
        }
    }
}
