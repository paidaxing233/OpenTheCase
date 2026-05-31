package com.openthecase.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Adds tooltip information to container items that have stored contents.
 * Shows the first 5 item names and a total count if there are more.
 */
public class TooltipHandler {

    private static final int MAX_DISPLAY_ITEMS = 5;

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!hasStoredItems(stack)) return;
        if (!isContainerItem(stack)) return;

        List<Component> tooltip = event.getToolTip();
        ListTag items = getStoredItems(stack);

        if (items.isEmpty()) return;

        // Add a blank line separator
        tooltip.add(Component.empty());

        // Add header
        tooltip.add(Component.translatable("tooltip.openthecase.stored_items").withStyle(ChatFormatting.GRAY));

        // Show up to 5 item names
        int displayCount = Math.min(items.size(), MAX_DISPLAY_ITEMS);
        for (int i = 0; i < displayCount; i++) {
            CompoundTag itemTag = items.getCompound(i);
            ItemStack storedStack = ItemStack.of(itemTag);
            if (!storedStack.isEmpty()) {
                String countStr = storedStack.getCount() > 1 ? " x" + storedStack.getCount() : "";
                tooltip.add(Component.literal("  ")
                        .append(storedStack.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(countStr).withStyle(ChatFormatting.GRAY)));
            }
        }

        // If there are more items, show total count
        if (items.size() > MAX_DISPLAY_ITEMS) {
            int remaining = items.size() - MAX_DISPLAY_ITEMS;
            tooltip.add(Component.translatable("tooltip.openthecase.more_items", remaining)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
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

    private boolean isContainerItem(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && (blockItem.getBlock() instanceof ChestBlock
                || blockItem.getBlock() instanceof ShulkerBoxBlock);
    }
}
