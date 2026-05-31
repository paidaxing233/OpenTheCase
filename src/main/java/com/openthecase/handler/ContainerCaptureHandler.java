package com.openthecase.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles block break events to capture container inventories
 * and attach them to dropped item entities.
 */
public class ContainerCaptureHandler {

    // Stores captured inventory NBT data keyed by block position
    private static final Map<BlockPos, ListTag> capturedInventories = new HashMap<>();

    /**
     * Captures container inventory before the block is broken.
     * Clears the block entity so vanilla doesn't drop items separately.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getState();
        Block block = state.getBlock();
        BlockPos pos = event.getPos();

        if (!isSupportedContainer(block)) return;

        ListTag items;

        BlockEntity blockEntity = event.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof BaseContainerBlockEntity container)) return;

        if (container instanceof ChestBlockEntity) {
            items = captureChestInventory((Level) event.getLevel(), pos, state, container);
        } else {
            // Shulker box or other container
            items = captureInventory(container);
        }

        capturedInventories.put(pos, items);
        clearInventory(container);

        // For double chests, also clear the connected chest
        if (container instanceof ChestBlockEntity) {
            clearConnectedChest((Level) event.getLevel(), pos, state);
        }
    }

    /**
     * Intercepts spawned ItemEntities to attach captured inventory data.
     */
    @SubscribeEvent
    public void onItemEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        ItemStack stack = itemEntity.getItem();
        if (!isContainerItem(stack)) return;

        BlockPos entityPos = itemEntity.blockPosition();
        ListTag items = findCapturedInventory(entityPos);
        if (items == null) return;

        // Attach the captured inventory to the item's NBT
        CompoundTag blockEntityTag = stack.getOrCreateTagElement("BlockEntityTag");
        blockEntityTag.put("Items", items);
        blockEntityTag.putInt("ItemsCount", items.size());

        // Mark this item as having stored contents (for GUI detection)
        CompoundTag modTag = stack.getOrCreateTag();
        modTag.putBoolean("OpenTheCase_Stored", true);

        itemEntity.setItem(stack);
    }

    /**
     * Captures inventory from a chest, handling double chests.
     */
    private ListTag captureChestInventory(Level level, BlockPos pos, BlockState state, BaseContainerBlockEntity container) {
        ListTag items = new ListTag();

        // Capture this chest's inventory
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                items.add(itemTag);
            }
        }

        // Check if this is a double chest and capture the connected half
        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType != ChestType.SINGLE) {
            Direction connectedDir = getConnectedDirection(chestType);
            BlockPos connectedPos = pos.relative(connectedDir);
            BlockEntity connectedEntity = level.getBlockEntity(connectedPos);

            if (connectedEntity instanceof ChestBlockEntity connectedChest) {
                for (int i = 0; i < connectedChest.getContainerSize(); i++) {
                    ItemStack stack = connectedChest.getItem(i);
                    if (!stack.isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putByte("Slot", (byte) (i + 27));
                        stack.save(itemTag);
                        items.add(itemTag);
                    }
                }
            }
        }

        return items;
    }

    /**
     * Captures inventory from any container block entity.
     */
    private ListTag captureInventory(BaseContainerBlockEntity container) {
        ListTag items = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                items.add(itemTag);
            }
        }
        return items;
    }

    /**
     * Clears all inventory slots in a container.
     */
    private void clearInventory(BaseContainerBlockEntity container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
        container.setChanged();
    }

    /**
     * Clears the connected chest's inventory for double chests.
     */
    private void clearConnectedChest(Level level, BlockPos pos, BlockState state) {
        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.SINGLE) return;

        Direction connectedDir = getConnectedDirection(chestType);
        BlockPos connectedPos = pos.relative(connectedDir);
        BlockEntity connectedEntity = level.getBlockEntity(connectedPos);

        if (connectedEntity instanceof BaseContainerBlockEntity container) {
            clearInventory(container);
        }
    }

    /**
     * Gets the direction to the connected chest half.
     */
    private Direction getConnectedDirection(ChestType chestType) {
        return switch (chestType) {
            case LEFT -> Direction.EAST;
            case RIGHT -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /**
     * Finds captured inventory by checking nearby positions.
     */
    private ListTag findCapturedInventory(BlockPos entityPos) {
        ListTag items = capturedInventories.get(entityPos);
        if (items != null) {
            capturedInventories.remove(entityPos);
            return items;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos nearby = entityPos.offset(dx, dy, dz);
                    items = capturedInventories.get(nearby);
                    if (items != null) {
                        capturedInventories.remove(nearby);
                        return items;
                    }
                }
            }
        }

        return null;
    }

    private boolean isSupportedContainer(Block block) {
        return block instanceof ChestBlock
                || block instanceof ShulkerBoxBlock;
    }

    private boolean isContainerItem(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && isSupportedContainer(blockItem.getBlock());
    }
}
