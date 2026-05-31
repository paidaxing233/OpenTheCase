package com.openthecase.handler;

import com.openthecase.menu.PortableChestMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

/**
 * Handles right-click events on container items to open their stored inventory GUI.
 */
public class ItemUseHandler {

    /**
     * Intercepts right-click with a container item that has stored contents.
     * On both sides: cancels the event to prevent block placement animation.
     * On server side: opens the portable chest GUI.
     */
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!hasStoredItems(stack)) return;
        if (!isContainerItem(stack)) return;

        // Cancel on BOTH client and server to prevent placement animation
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        // Only open GUI on server side
        if (!player.level().isClientSide()) {
            openPortableChestGUI(player, stack);
        }
    }

    /**
     * Opens the portable chest GUI for the player.
     */
    private void openPortableChestGUI(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ListTag items = getStoredItems(stack);
        int containerSize = calculateContainerSize(items, stack);
        Component title = getContainerTitle(stack);

        NetworkHooks.openScreen(serverPlayer,
                new PortableChestMenu.PortalChestMenuProvider(containerSize, title, stack),
                buf -> {
                    buf.writeItem(stack);
                    buf.writeComponent(title);
                });
    }

    private boolean hasStoredItems(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("OpenTheCase_Stored");
    }

    private ListTag getStoredItems(ItemStack stack) {
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) return new ListTag();
        return blockEntityTag.getList("Items", 10);
    }

    private int calculateContainerSize(ListTag items, ItemStack stack) {
        int maxSlot = -1;
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot > maxSlot) maxSlot = slot;
        }
        return maxSlot >= 27 ? 54 : 27;
    }

    private Component getContainerTitle(ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            return stack.getHoverName();
        }

        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ChestBlock) return Component.translatable("container.chest");
            if (block instanceof ShulkerBoxBlock) return Component.translatable("container.shulkerBox");
        }

        return Component.translatable("container.chest");
    }

    private boolean isContainerItem(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && (blockItem.getBlock() instanceof ChestBlock
                || blockItem.getBlock() instanceof ShulkerBoxBlock);
    }
}
