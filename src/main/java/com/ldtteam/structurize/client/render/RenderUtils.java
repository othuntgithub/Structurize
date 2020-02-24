package com.ldtteam.structurize.client.render;

import java.nio.ByteBuffer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import org.lwjgl.system.MemoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.BufferBuilder.DrawState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.world.World;

public class RenderUtils
{
    private static World tileEntityRendererDispatcherWorld;
    private static ActiveRenderInfo tileEntityRendererDispatcherActiveRenderInfo;
    private static World entityRendererManagerWorld;
    private static ActiveRenderInfo entityRendererManagerActiveRenderInfo;

    public static void saveVanillaState(final WorldRenderer context, final MatrixStack matrixStack)
    {
        tileEntityRendererDispatcherWorld = TileEntityRendererDispatcher.instance.world;
        tileEntityRendererDispatcherActiveRenderInfo = TileEntityRendererDispatcher.instance.renderInfo;
        entityRendererManagerWorld = Minecraft.getInstance().getRenderManager().world;
        entityRendererManagerActiveRenderInfo = Minecraft.getInstance().getRenderManager().info;
    }

    public static void loadVanillaState(final WorldRenderer context, final MatrixStack matrixStack)
    {
        TileEntityRendererDispatcher.instance.world = tileEntityRendererDispatcherWorld;
        TileEntityRendererDispatcher.instance.renderInfo = tileEntityRendererDispatcherActiveRenderInfo;
        Minecraft.getInstance().getRenderManager().info = entityRendererManagerActiveRenderInfo;
        Minecraft.getInstance().getRenderManager().world = entityRendererManagerWorld;
    }

    public static BufferBuilder createAndBeginBuffer(final RenderType renderType)
    {
        final BufferBuilder bufferBuilder = new BufferBuilder(RenderType.getLines().getBufferSize());
        bufferBuilder.begin(RenderType.getLines().getDrawMode(), RenderType.getLines().getVertexFormat());
        return bufferBuilder;
    }

    /**
     * @see RenderType#finish()
     */
    public static BuiltBuffer finishBuffer(final BufferBuilder bufferBuilder, final RenderType renderType)
    {
        if (bufferBuilder.isDrawing())
        {
            if (renderType.needsSorting)
            {
                bufferBuilder.sortVertexData(0, 0, 0);
            }

            bufferBuilder.finishDrawing();
            return BuiltBuffer.of(bufferBuilder.getNextBuffer(), renderType);
        }
        throw new RuntimeException("bufferbuilder not in drawing state");
    }

    /**
     * @see RenderType#finish()
     */
    public static void drawBuiltBuffer(final BuiltBuffer builtBuffer)
    {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        builtBuffer.getRenderType().setupRenderState();
        builtBuffer.getByteBuffer().clear();
        if (builtBuffer.getDrawState().getVertexCount() > 0)
        {
            builtBuffer.getDrawState().getFormat().setupBufferState(MemoryUtil.memAddress(builtBuffer.getByteBuffer()));
            GlStateManager.drawArrays(builtBuffer.getDrawState().getDrawMode(), 0, builtBuffer.getDrawState().getVertexCount());
            builtBuffer.getDrawState().getFormat().clearBufferState();
        }
        builtBuffer.getRenderType().clearRenderState();
    }

    public static class BuiltBuffer extends Pair<DrawState, ByteBuffer>
    {
        private final RenderType renderType;

        public BuiltBuffer(final Pair<DrawState, ByteBuffer> pair, final RenderType renderTypeIn)
        {
            super(pair.getFirst(), pair.getSecond());
            renderType = renderTypeIn;
        }

        public static BuiltBuffer of(final Pair<DrawState, ByteBuffer> pair, final RenderType renderType)
        {
            return new BuiltBuffer(pair, renderType);
        }

        public RenderType getRenderType()
        {
            return renderType;
        }

        public ByteBuffer getByteBuffer()
        {
            return super.getSecond();
        }

        public DrawState getDrawState()
        {
            return super.getFirst();
        }
    }
}
