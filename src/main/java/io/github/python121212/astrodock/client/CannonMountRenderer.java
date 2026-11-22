package io.github.python121212.astrodock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.python121212.astrodock.ship.CannonMount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;

public class CannonMountRenderer implements BlockEntityRenderer<CannonMount> {
    public CannonMountRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(CannonMount mount, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var state = mount.getBlockState();
        BakedModel model = dispatcher.getBlockModel(state);

        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-mount.yaw()));
        pose.scale(1.8f, 1.8f, 1.8f);
        pose.translate(-0.5, 0.0, -0.5);

        dispatcher.getModelRenderer().tesselateBlock(
                mount.getLevel(),
                model,
                state,
                mount.getBlockPos(),
                pose,
                buffer.getBuffer(RenderType.cutout()),
                false,
                RandomSource.create(),
                state.getSeed(mount.getBlockPos()),
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
        pose.popPose();
    }
}
