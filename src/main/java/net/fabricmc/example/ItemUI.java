package net.fabricmc.example;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.*;
import net.minecraft.util.registry.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;

@Environment(EnvType.CLIENT)
public class ItemUI extends Screen {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private final File FILE = new File("item_renders");

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;


    private final Deque<Item> queue = new ArrayDeque<>();
    private final int max;

    private float zOffset = 0;

    public ItemUI() {
        super(Text.of(""));
        if (!FILE.exists()) {
            if (!FILE.mkdirs()) {
                throw new Error("Failed to create folder for images...");
            }
        }
        for (Map.Entry<RegistryKey<Item>, Item> entry : Registry.ITEM.getEntries()) {
            queue.add(entry.getValue());
        }
        max = queue.size();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.enableDepthTest();

        if (queue.isEmpty()) {
            MC.setScreen(null);
            return;
        }
        innerRenderInGui(new ItemStack(queue.pop()));

        MC.textRenderer.draw(matrices, String.format("Loading... (%s/%s)", max - queue.size(), max), 300, 100, 0xFFFFFF);
    }

    private void innerRenderInGui(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }

        this.zOffset += 50.0F;
        this.renderGuiItemModel(itemStack);
        this.zOffset -= 50.0F;
    }

    @SuppressWarnings("deprecation")
    protected void renderGuiItemModel(ItemStack stack) {
        TextureManager manager = MC.getTextureManager();
        ItemRenderer renderer = MC.getItemRenderer();
        BakedModel model = renderer.getModel(stack, null, null, 0);

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        manager.bindTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        manager.getTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).setFilter(false, false);

        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.translate((float) WIDTH / 8, (float) HEIGHT / 8, 100.0F + this.zOffset);

        matrixStack.scale(1.0F, -1.0F, 1.0F);
        matrixStack.scale(16, 16, 16F);
        matrixStack.scale((float) WIDTH / 64, (float) HEIGHT / 64, (float) WIDTH / 64);
        RenderSystem.applyModelViewMatrix();
        MatrixStack matrixStack2 = new MatrixStack();

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        boolean doNotSideLight = !model.isSideLit();
        if (doNotSideLight) {
            DiffuseLighting.disableGuiDepthLighting();
        }

        renderer.renderItem(stack, ModelTransformation.Mode.GUI, false, matrixStack2, immediate, 15728880, OverlayTexture.DEFAULT_UV, model);
        immediate.draw();

        RenderSystem.enableDepthTest();
        if (doNotSideLight) {
            DiffuseLighting.enableGuiDepthLighting();
        }

        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();

        ByteBuffer buf = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        GL11.glReadPixels(MC.getWindow().getX(), MC.getWindow().getHeight() - HEIGHT, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);

        new Thread(() -> {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            try {
                int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
                for (int i = pixels.length - 1; i >= 0; i--) {
                    int x2 = i % WIDTH;
                    int y2 = i / WIDTH * WIDTH;

                    int r = buf.get();
                    int g = buf.get();
                    int b = buf.get();
                    int a = buf.get();

                    pixels[y2 + WIDTH - 1 - x2] = ((a & 0xFF) << 24) |
                            ((r & 0xFF) << 16) |
                            ((g & 0xFF) << 8) |
                            ((b & 0xFF) << 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    ImageIO.write(image, "png", new File(FILE, Registry.ITEM.getId(stack.getItem()).getPath().toUpperCase() + ".png"));
                } catch (Exception ignored) {}
            }
        }).start();
    }


}