package com.openthecase.menu;

import com.openthecase.OpenTheCaseMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Container menu for viewing and taking items from a portable chest item.
 */
public class PortableChestMenu extends AbstractContainerMenu {

    private final SimpleContainer container;
    private final ItemStack sourceStack;
    private final int containerRows;

    /**
     * Server-side constructor.
     */
    public PortableChestMenu(int containerId, Inventory playerInventory, ItemStack sourceStack, int containerSize, Component title) {
        super(OpenTheCaseMod.PORTABLE_CHEST_MENU.get(), containerId);
        this.sourceStack = sourceStack;
        this.containerRows = containerSize / 9;

        // Create container from stored items
        this.container = new SimpleContainer(containerSize);
        loadItemsFromNBT();

        // Add container slots
        for (int row = 0; row < this.containerRows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(this.container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory and hotbar slots
        // Vanilla GenericContainerMenu formula: offset = (rows - 4) * 36
        // Player inventory at y = 103 + offset, hotbar at y = 161 + offset
        // +18 to shift down by 1 slot to match our texture rendering
        int offset = (this.containerRows - 4) * 36 + 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + offset + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + offset));
        }
    }

    /**
     * Client-side constructor (from network).
     */
    public PortableChestMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readItem(), buf.readComponent());
    }

    /**
     * Intermediate constructor for client-side: calculates container size from stack.
     */
    public PortableChestMenu(int containerId, Inventory playerInventory, ItemStack sourceStack, Component title) {
        this(containerId, playerInventory, sourceStack, calculateSize(sourceStack), title);
    }

    /**
     * Calculates appropriate container size from stored items in the stack.
     */
    private static int calculateSize(ItemStack stack) {
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) return 27;
        ListTag items = blockEntityTag.getList("Items", 10);
        int maxSlot = -1;
        for (int i = 0; i < items.size(); i++) {
            int slot = items.getCompound(i).getByte("Slot") & 255;
            if (slot > maxSlot) maxSlot = slot;
        }
        return maxSlot >= 27 ? 54 : 27;
    }

    /**
     * Loads items from the source stack's NBT into the container.
     */
    private void loadItemsFromNBT() {
        CompoundTag blockEntityTag = sourceStack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) return;

        ListTag items = blockEntityTag.getList("Items", 10);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < container.getContainerSize()) {
                container.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }

    /**
     * Saves remaining items back to the source stack's NBT.
     */
    private void saveItemsToNBT() {
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

        CompoundTag blockEntityTag = sourceStack.getOrCreateTagElement("BlockEntityTag");
        if (items.isEmpty()) {
            blockEntityTag.remove("Items");
            blockEntityTag.remove("ItemsCount");
            // Remove the stored marker if empty
            sourceStack.getTag().remove("OpenTheCase_Stored");
        } else {
            blockEntityTag.put("Items", items);
            blockEntityTag.putInt("ItemsCount", items.size());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // Move from container to player inventory
            if (index < container.getContainerSize()) {
                if (!this.moveItemStackTo(slotStack, container.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Move from player inventory to container
            else {
                if (!this.moveItemStackTo(slotStack, 0, container.getContainerSize(), false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        // Check if the player is still holding the source item
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (ItemStack.isSameItemSameTags(player.getInventory().getItem(i), sourceStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Save items back to the source stack when the menu is closed
        saveItemsToNBT();
    }

    public int getContainerRows() {
        return containerRows;
    }

    public SimpleContainer getContainer() {
        return container;
    }

    /**
     * MenuProvider for opening the portable chest GUI.
     */
    public static class PortalChestMenuProvider implements net.minecraft.world.MenuProvider {
        private final int containerSize;
        private final Component title;
        private final ItemStack stack;

        public PortalChestMenuProvider(int containerSize, Component title, ItemStack stack) {
            this.containerSize = containerSize;
            this.title = title;
            this.stack = stack;
        }

        @Override
        public Component getDisplayName() {
            return title;
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new PortableChestMenu(containerId, playerInventory, stack, containerSize, title);
        }
    }
}
