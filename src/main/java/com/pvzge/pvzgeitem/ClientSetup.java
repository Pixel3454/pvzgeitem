package com.pvzge.pvzgeitem;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
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
 * 客户端：R 键 + 双截棍的三段式渲染。
 * 1.21.1 自带 BEWLR，不需要任何动画 mod 前置。
 */
@EventBusSubscriber(modid = PvzgeitemMod.MODID, value = Dist.CLIENT)
public class ClientSetup {

    /** 默认 R，可在 选项 → 控制 → 按键绑定 →「武林寺庙」里改。 */
    public static final KeyMapping KNOCKBACK_KEY = new KeyMapping(
            "key.pvzgeitem.knockback",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.pvzgeitem");

    private static NunchakuRenderer renderer;

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(KNOCKBACK_KEY);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        renderer = new NunchakuRenderer();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
            // 注意：这里故意不覆写 applyForgeHandTransform。
            // 交还原版摆右手位置，避免整体被转飞导致看不见 / 穿模。
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

    /**
     * 三段几何：
     *   握持节（固定在右手） → 3 节锁链 → 自由节（独立弹动）
     * 铰链在握持节末端，旋转 / 抽打都绕这个点做，所以自由节会甩出来。
     *
     * ModelPart 的 addBox 用「像素」（1/16 方块），PoseStack 的 translate 用「方块」，
     * 下面所有位移都按 /16 换算过。
     */
    private static final class NunchakuRenderer extends BlockEntityWithoutLevelRenderer {

        // 不加新贴图，复用原版：棍节用木棍，链节用锁链
        private static final Material STICK = new Material(
                TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath("minecraft", "item/stick"));
        private static final Material CHAIN = new Material(
                TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath("minecraft", "block/chain"));

        private static final float HINGE_Y = -10.0F / 16.0F;   // 握持节末端
        private static final float CHAIN_END_Y = -11.0F / 16.0F; // 链子末端

        private final ModelPart held;
        private final ModelPart chain;
        private final ModelPart free;

        NunchakuRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());

            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // 握持节：中心在手，长 20 像素
            root.addOrReplaceChild("held",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-1.5F, -10.0F, -1.5F, 3.0F, 20.0F, 3.0F),
                    PartPose.ZERO);

            // 锁链：3 个小链节，挂在铰链下方
            root.addOrReplaceChild("chain",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                            .addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                            .addBox(-1.0F, -9.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.ZERO);

            // 自由节：从链子末端往下延伸的第二根棍
            root.addOrReplaceChild("free",
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-1.5F, -20.0F, -1.5F, 3.0F, 20.0F, 3.0F),
                    PartPose.ZERO);

            ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
            held = baked.getChild("held");
            chain = baked.getChild("chain");
            free = baked.getChild("free");
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                                 MultiBufferSource buffer, int light, int overlay) {
            boolean inHand = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

            LocalPlayer player = Minecraft.getInstance().player;
            float t = player == null ? 0.0F : (float) player.tickCount;
            float attack = (inHand && player != null) ? player.getAttackAnim(0.0F) : 0.0F;
            boolean spinning = inHand && player != null && player.isUsingItem()
                    && player.getUseItem().is(ModItems.NUNCHAKU.get());

            float spin = 0.0F;   // 右键：整组绕右手铰链旋转
            float swing = 0.0F;  // 左键：向前抽打
            float flap;          // 自由节上下弹动

            if (spinning) {
                spin = t * 18.0F;
                flap = (float) Math.sin(t * 0.35F) * 24.0F;
            } else {
                flap = (float) Math.sin(t * 0.12F) * 6.0F; // 不按也轻轻晃，像没握住的那一节
            }
            if (attack > 0.0F) {
                swing = -78.0F * attack;
                // 自由节滞后一拍再甩出去，形成鞭打
                flap += -60.0F * Math.max(0.0F, attack - 0.18F) / 0.82F;
            }

            pose.pushPose();
            if (!inHand) {
                // 物品栏 / 地面 / 展示框：静态斜放，缩一点免得出格
                pose.scale(0.62F, 0.62F, 0.62F);
                pose.mulPose(Axis.ZP.rotationDegrees(32.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-18.0F));
            } else {
                // 稍微往前一点，别贴进手臂模型
                pose.translate(0.0F, 0.0F, -0.06F);
            }

            var stickVc = STICK.buffer(buffer, RenderType::entityCutout);
            var chainVc = CHAIN.buffer(buffer, RenderType::entityCutout);

            // ① 握持节：固定在右手，不跟着甩
            held.render(pose, stickVc, light, overlay);

            pose.pushPose();
            pose.translate(0.0F, HINGE_Y, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(spin));
            pose.mulPose(Axis.XP.rotationDegrees(swing));

            // ② 锁链：连在握持节末端
            chain.render(pose, chainVc, light, overlay);

            pose.translate(0.0F, CHAIN_END_Y, 0.0F);
            pose.mulPose(Axis.XP.rotationDegrees(flap));

            // ③ 自由节：独立弹动 / 滞后甩出
            free.render(pose, stickVc, light, overlay);

            pose.popPose();
            pose.popPose();
        }
    }
}
