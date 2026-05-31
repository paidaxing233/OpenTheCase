package com.openthecase.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.openthecase.menu.PortableChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side screen for the portable chest GUI.
 * Renders the chest inventory using vanilla textures.
 */
public class PortableChestScreen extends AbstractContainerScreen<PortableChestMenu> {

    private static final ResourceLocation CONTAINER_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    private final int containerRows;

    public PortableChestScreen(PortableChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.containerRows = menu.getContainerRows();
        this.imageHeight = 114 + this.containerRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CONTAINER_TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Draw the top part (container title area)
        guiGraphics.blit(CONTAINER_TEXTURE, x, y, 0, 0, this.imageWidth, this.containerRows * 18 + 17);

        // Draw the bottom part (player inventory)
        guiGraphics.blit(CONTAINER_TEXTURE, x, y + this.containerRows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    /**
     * Registers the screen with the menu type.
     */
    public static void register() {
        net.minecraft.client.gui.screens.MenuScreens.register(
                com.openthecase.OpenTheCaseMod.PORTABLE_CHEST_MENU.get(),
                PortableChestScreen::new
        );
    }
}
