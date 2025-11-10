package com.diablo931.client;

import com.diablo931.block.PerfusionSystemBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;


public class PerfusionSystemRenderer implements BlockEntityRenderer<PerfusionSystemBlockEntity> {

    public PerfusionSystemRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(PerfusionSystemBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        ItemStack stack = be.getDisplayedItem();
        if (stack.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int growthTicks = be.getGrowthTicks(); // your BE field
        long worldTime = be.getWorld() == null ? 0L : be.getWorld().getTime();
        float max_ticks = be.MAX_GROWTH_TICKS;
        float progress = Math.max(0f, Math.min(1f, growthTicks / (float) max_ticks));

        matrices.push();
        matrices.translate(0.225, 0.16, 0.725); // position inside chamber
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90)); // lay flat
        matrices.scale(0.5f, 0.5f, 0.5f);

        // Make item grow based on progress
        float scale = 1.0f + 0.25f * progress;
        matrices.scale(scale, scale, scale);

        // Optional: make it float slightly
        float bob = (float)Math.sin((worldTime + tickDelta) / 10.0) * 0.1f * progress;
        matrices.translate(0.0, 0.0, bob);

        // If growth is near complete, use a brighter light
        int baseLight = 0xF000F0;

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack, ModelTransformationMode.GROUND,
                baseLight, overlay, // full brightness
                matrices, vcp, be.getWorld(), 0
        );

        matrices.pop();

    }
}
