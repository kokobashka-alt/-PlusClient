package ru.zov.implement.features.modules.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.common.util.entity.PlayerInventoryComponent;
import ru.zov.common.util.entity.PlayerIntersectionUtil;
import ru.zov.common.util.item.ItemUtil;
import ru.zov.common.util.item.PlayerInventoryUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.rotation.Angle;
import ru.zov.common.util.rotation.AngleUtil;
import ru.zov.implement.events.keyboard.KeyEvent;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.features.draggables.Notifications;

/**
 * FTHelper: быстрое использование расходников на FunTime / SpookyTime / HolyWorld.
 *
 * <ul>
 *   <li>Бинды предметов: нашли предмет (офхенд, инвентарь, мешок), проверили КД, использовали через свап в руку и обратно.</li>
 *   <li>Разбафф (бинд «Разбафф»): дебафф-зелье, затем (если есть слабость) фантомная мембрана или бафф-зелье по таймингу.</li>
 *   <li>Арбалет: «По бинду» — стрельба любым арбалетом; «Умный» — перезарядка световой стрелой и зарядка пустых арбалетов.</li>
 *   <li>Авто питч: расходники, бросаемые вниз, использует с углом 90° (камера не двигается).</li>
 * </ul>
 *
 * Перенесено из клиента evo-подобного формата. Сопоставление с нашим API: InventorySelection заменён на
 * PlayerInventoryUtil.swapAndUse, ClientChat и NotificationQueue — на чат и Notifications, BotSoundEvents опущен.
 * Кастомные предметы FunTime (заряд ветра, трапки и т.п.) в исходнике идентифицированы только по названию — здесь так же.
 */
public class FTHelper extends Module {
    private static final long ALERT_COOLDOWN_MS = 500L;
    private static final long BUFF_AURA_DELAY_MS = 500L;
    private static final long BUFF_BUFF_DELAY_MS = 100L;
    private static final long AFTER_CHARGE_MS = 500L;
    private static final int HOTBAR_LAST = 8;
    private static final int SLOT_OFFHAND = 45;
    private static final int SLOT_HOTBAR_FIRST = 36;
    private static final int SLOT_MAIN_FIRST = 9;
    private static final int SLOT_MAIN_LAST = 35;

    public static FTHelper getInstance() {
        return (FTHelper) Instance.get(FTHelper.class);
    }

    // ---- настройки ----

    public final BooleanSetting cooldownCheck = new BooleanSetting("Кулдауны противников",
            "Проверять КД предмета перед использованием и сообщать, если он ещё не готов").setValue(true);
    public final BooleanSetting alert = new BooleanSetting("Оповещение",
            "Показывать уведомление поверх чата при КД").setValue(true).visible(cooldownCheck::isValue);
    public final SelectSetting server = new SelectSetting("Сервер", "Какой сервер: меняет способ свапа")
            .value("FunTime", "SpookyTime", "HolyWorld").selected("FunTime");
    public final BooleanSetting autoPitch = new BooleanSetting("Авто питч",
            "Бросаемые зелья использовать с углом 90° (камера не двигается)").setValue(false).visible(() -> !isHolyWorld());
    public final SelectSetting crossbowMode = new SelectSetting("Режим арбалета", "Как работать с арбалетом")
            .value("По бинду", "Умный").selected("По бинду");

    public final BindSetting windCharge = bind("Заряд ветра", () -> !isHolyWorld());
    public final BindSetting fireTornado = bind("Огненный смерч", () -> !isHolyWorld());
    public final BindSetting disorient = bind("Дезориентация", () -> !isHolyWorld());
    public final BindSetting trap = bind("Трапка", () -> !isHolyWorld());
    public final BindSetting plast = bind("Пласт", () -> !isHolyWorld());
    public final BindSetting dust = bind("Явная пыль", () -> !isHolyWorld());
    public final BindSetting godAura = bind("Божья аура", () -> !isHolyWorld());
    public final BindSetting snowball = bind("Снежок", () -> !isHolyWorld());
    public final BindSetting explosiveTrap = bind("Взрывная трапка", this::isHolyWorld);
    public final BindSetting simpleTrap = bind("Обычная трапка", this::isHolyWorld);
    public final BindSetting stun = bind("Стан", this::isHolyWorld);
    public final BindSetting explosiveItem = bind("Взрывная штучка", this::isHolyWorld);
    public final BindSetting snowballStack = bind("Ком снега", this::isHolyWorld);
    public final BindSetting crossbow = bind("Арбалет", () -> true);
    public final BindSetting reload = bind("Перезарядка", () -> crossbowMode.isSelected("Умный"));
    public final BindSetting assassin = bind("Зелье ассасина", () -> !isHolyWorld());
    public final BindSetting paladin = bind("Зелье палладина", () -> !isHolyWorld());
    public final BindSetting sleep = bind("Снотворное", () -> !isHolyWorld());
    public final BindSetting wrath = bind("Зелье гнева", () -> !isHolyWorld());
    public final BindSetting cracker = bind("Хлопушка", () -> !isHolyWorld());
    public final BindSetting radiation = bind("Зелье радиации", () -> !isHolyWorld());
    public final BindSetting holyWater = bind("Святая вода", () -> !isHolyWorld());
    public final BindSetting unbuff = bind("Разбафф", () -> !isHolyWorld());
    public final SelectSetting debuff = new SelectSetting("Дебафф", "Что кидать первым при «Разбаффе»")
            .value("Снотворное", "Радиация").selected("Снотворное").visible(() -> !isHolyWorld());
    public final SelectSetting buff = new SelectSetting("Бафф", "Что кидать вторым при «Разбаффе»")
            .value("Зелье гнева", "Зелье ассасина").selected("Зелье гнева").visible(() -> !isHolyWorld());

    // ---- состояние ----

    private final Map<Identifier, Long> lastAlert = new HashMap<>();
    private BuffState buffState = BuffState.IDLE;
    private FTBuff pendingBuff;
    private long buffTimer;
    private ChargeState chargeState = ChargeState.IDLE;
    private int arrowSlot = -1;
    private int crossbowSlot = -1;
    private int hotbarTemp = -1;
    private int previousSelected = -1;
    private long chargeTimer;
    private boolean sprintStopped;

    public FTHelper() {
        super("FTHelper", "Быстрые расходники и арбалет для FunTime", ModuleCategory.COMBAT);
        this.setup(new Setting[]{
                cooldownCheck, alert, server, autoPitch, crossbowMode,
                windCharge, fireTornado, disorient, trap, plast, dust, godAura, snowball,
                explosiveTrap, simpleTrap, stun, explosiveItem, snowballStack,
                crossbow, reload, assassin, paladin, sleep, wrath, cracker, radiation, holyWater, unbuff,
                debuff, buff
        });
    }

    private static BindSetting bind(String name, java.util.function.Supplier<Boolean> visible) {
        return new BindSetting(name, "Бинд: " + name).visible(visible);
    }

    private boolean isHolyWorld() {
        return server.isSelected("HolyWorld");
    }

    // ---- события ----

    @Override
    public void deactivate() {
        cancelCharge();
        resetBuff();
        lastAlert.clear();
        if (mc.player != null) {
            restoreSprintKey();
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null) return;

        if (crossbowMode.isSelected("Умный")) {
            if (e.isKeyDown(reload)) {
                toggleSmartCharge();
                return;
            }
            if (e.isKeyDown(crossbow)) {
                useItem("Арбалет", stack -> stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack), false);
                return;
            }
        } else if (e.isKeyDown(crossbow)) {
            useItem("Арбалет", stack -> stack.isOf(Items.CROSSBOW), false);
            return;
        }

        if (e.isKeyDown(unbuff)) {
            startBuffCombo();
            return;
        }

        for (ItemBind bind : binds()) {
            if (e.isKeyDown(bind.key())) {
                useItem(bind.key().getName(), bind.match(), bind.pitchAllowed() && autoPitch.isValue());
                return;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;
        updateBuff();
        updateCharge();
    }

    // ---- таблица предметов ----

    private record ItemBind(BindSetting key, Predicate<ItemStack> match, boolean pitchAllowed) {
    }

    private List<ItemBind> binds() {
        return List.of(
                new ItemBind(windCharge, nameIs("Заряд ветра"), false),
                new ItemBind(fireTornado, nameIs("Огненный смерч"), false),
                new ItemBind(disorient, nameIs("Дезориентация"), false),
                new ItemBind(trap, nameIs("Трапка"), false),
                new ItemBind(plast, nameIs("Пласт"), false),
                new ItemBind(dust, nameIs("Явная пыль"), false),
                new ItemBind(godAura, stack -> stack.isOf(Items.PHANTOM_MEMBRANE), false),
                new ItemBind(snowball, nameIs("Снежок"), false),
                new ItemBind(explosiveTrap, nameIs("Взрывная трапка"), false),
                new ItemBind(simpleTrap, nameIs("Обычная трапка"), false),
                new ItemBind(stun, nameIs("Стан"), false),
                new ItemBind(explosiveItem, nameIs("Взрывная штучка"), false),
                new ItemBind(snowballStack, nameIs("Ком снега"), false),
                new ItemBind(assassin, stack -> matchesBuff(stack, FTBuff.ASSASSIN), true),
                new ItemBind(paladin, stack -> matchesBuff(stack, FTBuff.PALADIN), true),
                new ItemBind(sleep, stack -> matchesBuff(stack, FTBuff.SLEEPING), false),
                new ItemBind(wrath, stack -> matchesBuff(stack, FTBuff.WRATH), true),
                new ItemBind(cracker, stack -> matchesBuff(stack, FTBuff.CRACKER), true),
                new ItemBind(radiation, stack -> matchesBuff(stack, FTBuff.RADIATION), false),
                new ItemBind(holyWater, stack -> matchesBuff(stack, FTBuff.HOLY_WATER), true)
        );
    }

    private static Predicate<ItemStack> nameIs(String label) {
        String needle = label.toLowerCase(Locale.ROOT);
        return stack -> !stack.isEmpty() && plainName(stack).contains(needle);
    }

    private static String plainName(ItemStack stack) {
        return Formatting.strip(stack.getName().getString()).toLowerCase(Locale.ROOT);
    }

    // ---- использование предмета ----

    /** Ищет предмет (офхенд, затем инвентарь с конца, затем мешки) и использует его; true если запущено. */
    private boolean useItem(String label, Predicate<ItemStack> match, boolean pitchDown) {
        if (mc.player == null || mc.interactionManager == null) return false;
        if (busy()) return false;

        Slot found = findSlot(match);
        if (found == null) {
            Notifications.getInstance().addList(label + " нет в инвентаре", 2000L);
            return false;
        }

        ItemStack stack = found.getStack();
        if (onCooldown(stack)) return false;

        Angle angle = pitchDown ? AngleUtil.pitch(90.0F) : AngleUtil.cameraAngle();
        if (found.id == SLOT_OFFHAND) {
            PlayerIntersectionUtil.interactItem(Hand.OFF_HAND, angle, true);
        } else {
            useFromInventory(found, angle);
        }
        return true;
    }

    private void useFromInventory(Slot slot, Angle angle) {
        if (server.isSelected("HolyWorld")) {
            // Тот же порядок, что у HolyWorld в исходнике: свап, закрыть экран и повернуть, использовать, вернуть.
            PlayerInventoryComponent.script.addTickStep(1, () -> {
                PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
                PlayerInventoryUtil.closeScreen(true);
                PlayerInventoryComponent.rotateToAngle(angle);
            }).addTickStep(2, () -> PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, angle))
              .addTickStep(3, () -> {
                  PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false, true);
                  PlayerInventoryUtil.closeScreen(true);
              });
        } else {
            PlayerInventoryUtil.swapAndUse(slot, angle, Hand.MAIN_HAND);
        }
    }

    /** Порядок поиска: офхенд, основной инвентарь с конца, хотбар. */
    private Slot findSlot(Predicate<ItemStack> match) {
        Slot offhand = mc.player.playerScreenHandler.getSlot(SLOT_OFFHAND);
        if (match.test(offhand.getStack())) return offhand;
        for (int id = SLOT_MAIN_LAST; id >= SLOT_MAIN_FIRST; id--) {
            Slot slot = mc.player.playerScreenHandler.getSlot(id);
            if (match.test(slot.getStack())) return slot;
        }
        for (int id = SLOT_HOTBAR_FIRST + HOTBAR_LAST; id >= SLOT_HOTBAR_FIRST; id--) {
            Slot slot = mc.player.playerScreenHandler.getSlot(id);
            if (match.test(slot.getStack())) return slot;
        }
        return null;
    }

    // ---- КД ----

    private boolean onCooldown(ItemStack stack) {
        if (!cooldownCheck.isValue()) return false;
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        if (!manager.isCoolingDown(stack)) return false;

        Identifier group = manager.getGroup(stack);
        long now = System.currentTimeMillis();
        Long last = lastAlert.get(group);
        if (last != null && now - last < ALERT_COOLDOWN_MS) return true;
        lastAlert.put(group, now);

        String name = Formatting.strip(stack.getName().getString());
        String time = String.format(Locale.ROOT, "%.1fs", ItemUtil.getCooldownProgress(stack));
        chat(name + " в КД ещё " + time);
        if (alert.isValue()) {
            Notifications.getInstance().addList(name + " в КД ещё " + time, 2000L);
        }
        return true;
    }

    private void chat(String text) {
        mc.player.sendMessage(Text.literal("[FTHelper] ").formatted(Formatting.AQUA).append(Text.literal(text)), false);
    }

    // ---- разбафф: дебафф -> (если слабость) мембрана / бафф ----

    private void startBuffCombo() {
        if (mc.player == null || busy() || buffState != BuffState.IDLE) return;
        FTBuff first = debuff.isSelected("Радиация") ? FTBuff.RADIATION : FTBuff.SLEEPING;
        FTBuff second = buff.isSelected("Зелье ассасина") ? FTBuff.ASSASSIN : FTBuff.WRATH;
        if (useItem(first.label, stack -> matchesBuff(stack, first), false)) {
            pendingBuff = second;
            buffTimer = System.currentTimeMillis();
            buffState = BuffState.WAITING_DEBUFF;
        }
    }

    private void updateBuff() {
        if (buffState == BuffState.IDLE) return;
        long elapsed = System.currentTimeMillis() - buffTimer;

        switch (buffState) {
            case WAITING_DEBUFF -> {
                if (!busy()) {
                    buffState = BuffState.WAITING_AURA;
                    buffTimer = System.currentTimeMillis();
                }
            }
            case WAITING_AURA -> {
                if (elapsed < BUFF_AURA_DELAY_MS) return;
                if (hasWeaknessAmp()) {
                    if (!useItem("Божья аура", stack -> stack.isOf(Items.PHANTOM_MEMBRANE), false)) {
                        resetBuff();
                        return;
                    }
                    buffState = BuffState.WAITING_AURA_ACTION;
                } else {
                    if (!useItem(pendingBuff.label, stack -> matchesBuff(stack, pendingBuff), autoPitch.isValue())) {
                        resetBuff();
                        return;
                    }
                    buffState = BuffState.WAITING_BUFF_ACTION;
                }
                buffTimer = System.currentTimeMillis();
            }
            case WAITING_AURA_ACTION -> {
                if (!busy()) {
                    buffState = BuffState.WAITING_BUFF;
                    buffTimer = System.currentTimeMillis();
                }
            }
            case WAITING_BUFF -> {
                if (elapsed < BUFF_BUFF_DELAY_MS) return;
                if (!useItem(pendingBuff.label, stack -> matchesBuff(stack, pendingBuff), autoPitch.isValue())) {
                    resetBuff();
                } else {
                    buffState = BuffState.WAITING_BUFF_ACTION;
                    buffTimer = System.currentTimeMillis();
                }
            }
            case WAITING_BUFF_ACTION -> {
                if (!busy()) resetBuff();
            }
            default -> {
            }
        }
    }

    private void resetBuff() {
        buffState = BuffState.IDLE;
        pendingBuff = null;
        buffTimer = System.currentTimeMillis();
    }

    private boolean hasWeaknessAmp() {
        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        return weakness != null && weakness.getAmplifier() >= 1;
    }

    /** «Занят»: идёт умная перезарядка арбалета (разбафф использует предметы сам, поэтому тут не учитывается). */
    private boolean busy() {
        return chargeState != ChargeState.IDLE;
    }

    // ---- умный арбалет ----

    private void toggleSmartCharge() {
        if (chargeState == ChargeState.IDLE) {
            chat("Начинается перезарядка...");
            chargeState = ChargeState.FINDING_ARROW;
        } else {
            chat("Остановка перезарядки!");
            cancelCharge();
        }
    }

    private void updateCharge() {
        if (chargeState == ChargeState.IDLE) return;

        switch (chargeState) {
            case FINDING_ARROW -> {
                int id = findLightArrow();
                if (id == -1) {
                    chat("Световая стрела не найдена!");
                    resetCharge();
                    return;
                }
                arrowSlot = id;
                chargeState = ChargeState.SWAPPING_ARROW;
            }
            case SWAPPING_ARROW -> {
                if (arrowSlot != 44) swapToHotbar(arrowSlot, HOTBAR_LAST);
                chargeState = ChargeState.FINDING_CROSSBOW;
            }
            case FINDING_CROSSBOW -> {
                int id = findUnchargedCrossbow();
                if (id == -1) {
                    chargeState = ChargeState.RESTORING_ARROW;
                    return;
                }
                crossbowSlot = id;
                hotbarTemp = freeHotbarSlot(HOTBAR_LAST);
                chargeState = ChargeState.SWAPPING_CROSSBOW;
            }
            case SWAPPING_CROSSBOW -> {
                stopSprint();
                previousSelected = mc.player.getInventory().selectedSlot;
                swapToHotbar(crossbowSlot, hotbarTemp);
                selectHotbar(hotbarTemp);
                chargeState = ChargeState.START_CHARGING;
            }
            case START_CHARGING -> {
                mc.options.useKey.setPressed(true);
                PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, AngleUtil.cameraAngle(), true);
                chargeTimer = System.currentTimeMillis();
                chargeState = ChargeState.CHARGING;
            }
            case CHARGING -> {
                ItemStack held = mc.player.getInventory().getStack(hotbarTemp);
                if (!(held.getItem() instanceof CrossbowItem)) {
                    cancelCharge();
                    return;
                }
                long needMs = (long) CrossbowItem.getPullTime(held, mc.player) * 50L + 50L;
                if (System.currentTimeMillis() - chargeTimer >= needMs) {
                    releaseUse();
                    chargeTimer = System.currentTimeMillis();
                    chargeState = ChargeState.WAITING_AFTER_CHARGE;
                }
            }
            case WAITING_AFTER_CHARGE -> {
                if (System.currentTimeMillis() - chargeTimer >= AFTER_CHARGE_MS) {
                    chargeState = ChargeState.RESTORING_CROSSBOW;
                }
            }
            case RESTORING_CROSSBOW -> {
                restoreSelected();
                swapToHotbar(crossbowSlot, hotbarTemp);
                restoreSprintKey();
                chargeState = ChargeState.FINDING_CROSSBOW;
            }
            case RESTORING_ARROW -> {
                if (arrowSlot != -1 && arrowSlot != 44) swapToHotbar(arrowSlot, HOTBAR_LAST);
                chat("Все арбалеты заряжены!");
                resetCharge();
            }
            default -> {
            }
        }
    }

    /** Остановка в середине перезарядки: отпустить лук/арбалет, вернуть слоты и стрелу. */
    private void cancelCharge() {
        if (mc.player != null && (chargeState == ChargeState.START_CHARGING
                || chargeState == ChargeState.CHARGING || chargeState == ChargeState.WAITING_AFTER_CHARGE)) {
            releaseUse();
            restoreSelected();
            swapToHotbar(crossbowSlot, hotbarTemp);
            restoreSprintKey();
        }
        if (mc.player != null && chargeState != ChargeState.IDLE && chargeState != ChargeState.FINDING_ARROW
                && arrowSlot != -1 && arrowSlot != 44) {
            swapToHotbar(arrowSlot, HOTBAR_LAST);
        }
        resetCharge();
    }

    private void resetCharge() {
        chargeState = ChargeState.IDLE;
        arrowSlot = -1;
        crossbowSlot = -1;
        hotbarTemp = -1;
        previousSelected = -1;
        chargeTimer = 0L;
        if (mc.player != null) {
            mc.options.useKey.setPressed(false);
        }
    }

    private void releaseUse() {
        mc.options.useKey.setPressed(false);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
    }

    private int findLightArrow() {
        for (int id = SLOT_MAIN_FIRST; id <= SLOT_HOTBAR_FIRST + HOTBAR_LAST; id++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(id).getStack();
            if (stack.isOf(Items.TIPPED_ARROW) && plainName(stack).contains("световая")) return id;
        }
        ItemStack offhand = mc.player.currentScreenHandler.getSlot(SLOT_OFFHAND).getStack();
        return offhand.isOf(Items.TIPPED_ARROW) && plainName(offhand).contains("световая") ? SLOT_OFFHAND : -1;
    }

    private int findUnchargedCrossbow() {
        for (int id = SLOT_MAIN_FIRST; id <= SLOT_MAIN_LAST; id++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(id).getStack();
            if (stack.getItem() instanceof CrossbowItem && !CrossbowItem.isCharged(stack)) return id;
        }
        return -1;
    }

    private int freeHotbarSlot(int avoid) {
        for (int i = 0; i < 9; i++) {
            if (i != avoid && mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return avoid == HOTBAR_LAST ? 7 : HOTBAR_LAST;
    }

    /** Свап слота экрана с ячейкой хотбара (0..8) через ClickSlot и закрытие экрана. */
    private void swapToHotbar(int slotId, int hotbarIndex) {
        if (mc.player == null || slotId < 0 || hotbarIndex < 0) return;
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slotId, hotbarIndex, SlotActionType.SWAP, mc.player);
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    private void selectHotbar(int slot) {
        if (mc.player == null || slot < 0) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void restoreSelected() {
        if (previousSelected >= 0) selectHotbar(previousSelected);
    }

    private void stopSprint() {
        mc.options.sprintKey.setPressed(false);
        mc.player.setSprinting(false);
        if (!sprintStopped) {
            sprintStopped = true;
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private void restoreSprintKey() {
        sprintStopped = false;
        mc.options.sprintKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sprintKey.getDefaultKey().getCode()));
    }

    // ---- сопоставление зелий ----

    private boolean matchesBuff(ItemStack stack, FTBuff buff) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.SPLASH_POTION)) return false;

        String name = normalize(stack.getName().getString());
        if (!name.isEmpty() && name.contains(normalize(buff.label))) return true;

        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;

        Map<RegistryEntry<StatusEffect>, Integer> have = new HashMap<>();
        for (StatusEffectInstance instance : contents.getEffects()) {
            have.put(instance.getEffectType(), instance.getAmplifier());
        }
        if (have.size() != buff.effects.size()) return false;
        for (Effect wanted : buff.effects) {
            Integer amp = have.get(wanted.effect());
            if (amp == null || amp != wanted.amplifier()) return false;
        }
        return true;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT).replace("[+]", "").replace("[-]", "").replace("[*]", "").replace("[#]", "")
                .replaceAll("\\s+", " ").trim();
    }

    // ---- типы состояний ----

    record Effect(RegistryEntry<StatusEffect> effect, int amplifier) {
    }

    enum FTBuff {
        ASSASSIN(List.of(new Effect(StatusEffects.STRENGTH, 3), new Effect(StatusEffects.SPEED, 2),
                new Effect(StatusEffects.HASTE, 0), new Effect(StatusEffects.INSTANT_DAMAGE, 1)), "Зелье ассасина"),
        PALADIN(List.of(new Effect(StatusEffects.RESISTANCE, 0), new Effect(StatusEffects.FIRE_RESISTANCE, 0),
                new Effect(StatusEffects.HEALTH_BOOST, 2), new Effect(StatusEffects.INVISIBILITY, 2)), "Зелье палладина"),
        SLEEPING(List.of(new Effect(StatusEffects.WEAKNESS, 1), new Effect(StatusEffects.MINING_FATIGUE, 1),
                new Effect(StatusEffects.WITHER, 1), new Effect(StatusEffects.BLINDNESS, 0)), "Снотворное"),
        WRATH(List.of(new Effect(StatusEffects.STRENGTH, 4), new Effect(StatusEffects.SLOWNESS, 3)), "Зелье гнева"),
        CRACKER(List.of(new Effect(StatusEffects.SLOWNESS, 8), new Effect(StatusEffects.SPEED, 4),
                new Effect(StatusEffects.BLINDNESS, 9), new Effect(StatusEffects.GLOWING, 0)), "Хлопушка"),
        RADIATION(List.of(new Effect(StatusEffects.POISON, 1), new Effect(StatusEffects.WITHER, 1),
                new Effect(StatusEffects.SLOWNESS, 2), new Effect(StatusEffects.HUNGER, 4),
                new Effect(StatusEffects.GLOWING, 0)), "Зелье радиации"),
        HOLY_WATER(List.of(new Effect(StatusEffects.REGENERATION, 1), new Effect(StatusEffects.INVISIBILITY, 1),
                new Effect(StatusEffects.INSTANT_HEALTH, 1)), "Святая вода");

        final List<Effect> effects;
        final String label;

        FTBuff(List<Effect> effects, String label) {
            this.effects = effects;
            this.label = label;
        }
    }

    enum BuffState {
        IDLE, WAITING_DEBUFF, WAITING_AURA, WAITING_AURA_ACTION, WAITING_BUFF, WAITING_BUFF_ACTION
    }

    enum ChargeState {
        IDLE, FINDING_ARROW, SWAPPING_ARROW, FINDING_CROSSBOW, SWAPPING_CROSSBOW, START_CHARGING, CHARGING,
        WAITING_AFTER_CHARGE, RESTORING_CROSSBOW, RESTORING_ARROW
    }
}
