package com.cinemamod.mcef.addon;

import com.cinemamod.mcef.IMCEFBrowser;
import com.cinemamod.mcef.MCEFRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public class BrowserBlockEntityRenderer implements BlockEntityRenderer<BrowserBlockEntity> {
    public BrowserBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(BrowserBlockEntity entity, float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        IMCEFBrowser browser = entity.getBrowser();
        if (browser == null) return;

        MCEFRenderer renderer = browser.getRenderer();
        if (renderer == null || renderer.getTextureID() == 0) return;

        poseStack.pushPose();
        
        poseStack.translate(0.5, 0.5, 0.5);
        
        Direction facing = entity.getBlockState().getValue(BrowserScreenBlock.FACING);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
        }
        
        poseStack.translate(-0.5, -0.5, -0.501);

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShaderTexture(0, renderer.getTextureID());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        
        float uMin = (float) entity.getGridX() / entity.getGridWidth();
        float uMax = (float) (entity.getGridX() + 1) / entity.getGridWidth();
        float vMin = (float) (entity.getGridHeight() - entity.getGridY() - 1) / entity.getGridHeight();
        float vMax = (float) (entity.getGridHeight() - entity.getGridY()) / entity.getGridHeight();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        buffer.addVertex(matrix, 0, 0, 0).setUv(uMin, vMax);
        buffer.addVertex(matrix, 1, 0, 0).setUv(uMax, vMax);
        buffer.addVertex(matrix, 1, 1, 0).setUv(uMax, vMin);
        buffer.addVertex(matrix, 0, 1, 0).setUv(uMin, vMin);
        
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        poseStack.popPose();
    }
}
