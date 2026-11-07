package ru.zov.mixins.render;

import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={HeldItemRenderer.class})
public interface HeldItemRendererAccessor {
    @Accessor(value="mainHand")
    public ItemStack getMainHand();

    @Accessor(value="offHand")
    public ItemStack getOffHand();

    @Accessor(value="equipProgressMainHand")
    public float getEquipProgressMainHand();

    @Accessor(value="lastEquipProgressMainHand")
    public float getPrevEquipProgressMainHand();

    @Accessor(value="equipProgressOffHand")
    public float getEquipProgressOffHand();

    @Accessor(value="lastEquipProgressOffHand")
    public float getPrevEquipProgressOffHand();

    @Accessor(value="client")
    public MinecraftClient getClient();

    @Invoker(value="renderFirstPersonItem")
    public void invokeRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light);

    @Invoker(value="applyEquipOffset")
    public void invokeApplyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

    @Invoker(value="swingArm")
    public void invokeSwingArm(float swingProgress, MatrixStack matrices, int armX, Arm arm);

    @Invoker(value="renderArmHoldingItem")
    public void invokeRenderArmHoldingItem(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, float equipProgress, float swingProgress, Arm arm);

    @Invoker(value="renderMapInBothHands")
    public void invokeRenderMapInBothHands(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, float pitch, float equipProgress, float swingProgress);

    @Invoker(value="renderMapInOneHand")
    public void invokeRenderMapInOneHand(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);

    @Invoker(value="renderItem")
    public void invokeRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light);
}
