package com.pvzge.pvzgeitem;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端专用：R 键绑定 + 第一人称动画。
 * 1.21.1 的 @EventBusSubscriber 会自动分流：
 * RegisterKeyMappingsEvent / RegisterClientExtensionsEvent 进 mod 总线，ClientTickEvent 进游戏总线。
 * 不需要任何动画 mod 前置。
 */
@EventBusSubscriber(modid = PvzgeitemMod.MODID, value = Dist.CLIENT)
public class ClientSetup {

    /** 默认 R，可在 选项 → 控制 → 按键绑定 里改。 */
    public static final KeyMapping KNOCKBACK_KEY = new KeyMapping(
            "key.pvzgeitem.knockback",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.pvzgeitem");

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(KNOCKBACK_KEY);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player,
                                                   HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProgress,
                                                   float swingProgress) {
                // 左键「抽打」：挥动进度驱动一次快速甩棍
                if (swingProgress > 0.0F) {
                    float whip = (float) Math.sin(swingProgress * Math.PI);
                    poseStack.translate(0.0F, 0.0F, -0.22F * whip);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F * whip));
                    poseStack.mulPose(Axis.YP.rotationDegrees(75.0F * whip));
                }

                // 右键「持续旋转」：按住时每 tick 转 12°，约 1.5 秒一圈
                if (player.isUsingItem() && player.getUseItem().is(ModItems.NUNCHAKU.get())) {
                    float angle = (player.tickCount + partialTick) * 12.0F;
                    poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
                    poseStack.mulPose(Axis.YP.rotationDegrees(angle * 0.5F));
                }
                return true;
            }
        }, ModItems.NUNCHAKU.get());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (KNOCKBACK_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || Minecraft.getInstance().screen != null) continue;
            if (player.getMainHandItem().is(ModItems.NUNCHAKU.get())
                    || player.getOffhandItem().is(ModItems.NUNCHAKU.get())) {
                PacketDistributor.sendToServer(new PvzgeitemMod.NunchakuKnockbackPayload());
            }
        }
    }
}
