package dev.celli.enderutilities.menu;

import dev.celli.enderutilities.inventory.ModularModuleHandler;
import dev.celli.enderutilities.inventory.SelectedModuleInventoryHandler;
import dev.celli.enderutilities.item.ItemHandyBag;
import dev.celli.enderutilities.item.module.ModularHelper;
import dev.celli.enderutilities.item.module.ModuleType;
import dev.celli.enderutilities.registry.ModMenus;
import dev.celli.enderutilities.util.InventoryHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import dev.celli.enderutilities.inventory.CardBackedSlot;
import net.neoforged.neoforge.items.SlotItemHandler;

public class HandyBagMenu extends AbstractContainerMenu implements ItemGuiActionMenu {
    public static final int MODULE_SLOTS = 4;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final int bagSlot;
    private final int bagSlots;
    private final int bagTier;
    private final int craftResultIndex;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;

    public HandyBagMenu(int id, Inventory inventory, int bagSlot) {
        super(ModMenus.HANDY_BAG.get(), id);
        this.bagSlot = bagSlot;
        this.player = inventory.player;
        ItemStack bag = inventory.getItem(bagSlot);
        ItemHandyBag item = bag.getItem() instanceof ItemHandyBag handy ? handy : null;
        this.bagTier = item != null && item.tier() == ItemHandyBag.TIER_2 ? 1 : 0;
        this.bagSlots = item == null ? ItemHandyBag.INV_SIZE_TIER_1 : item.inventorySize();
        int max = item == null ? 64 : item.maxStackSize(bag);
        SelectedModuleInventoryHandler items = new SelectedModuleInventoryHandler(
                () -> inventory.getItem(this.bagSlot),
                stack -> inventory.setItem(this.bagSlot, stack),
                ModuleType.MEMORY_CARD_ITEMS,
                bagSlots,
                max
        );
        int xOff = bagTier == 1 ? 40 : 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new CardBackedSlot(items, row * 9 + col, 8 + xOff + col * 18, 102 + row * 18));
            }
        }
        if (bagTier == 1) {
            for (int i = 0; i < 7; i++) {
                addSlot(new CardBackedSlot(items, 27 + i * 2, 8, 102 + i * 18));
                addSlot(new CardBackedSlot(items, 28 + i * 2, 26, 102 + i * 18));
            }
            for (int i = 0; i < 7; i++) {
                addSlot(new CardBackedSlot(items, 41 + i * 2, 214, 102 + i * 18));
                addSlot(new CardBackedSlot(items, 42 + i * 2, 232, 102 + i * 18));
            }
        }
        ModularModuleHandler modules = new ModularModuleHandler(
                () -> inventory.getItem(this.bagSlot),
                stack -> inventory.setItem(this.bagSlot, stack),
                ModuleType.MEMORY_CARD_ITEMS,
                MODULE_SLOTS
        );
        for (int i = 0; i < MODULE_SLOTS; i++) {
            addSlot(new SlotItemHandler(modules, i, 8 + xOff + 90 + i * 18, 69));
        }

        int playerX = 8 + xOff;
        addPlayerSlots(inventory, playerX, 174);
        for (int i = 0; i < 4; i++) {
            final EquipmentSlot type = ARMOR[i];
            addSlot(new Slot(inventory, 39 - i, playerX, 15 + i * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(type, HandyBagMenu.this.player);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            }).setBackground(InventoryMenu.BLOCK_ATLAS, emptyArmorIcon(type));
        }
        addSlot(new Slot(inventory, Inventory.SLOT_OFFHAND, playerX + 72, 51))
                .setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);

        this.craftSlots = new TransientCraftingContainer(this, 2, 2);
        this.craftResultIndex = slots.size();
        addSlot(new ResultSlot(inventory.player, craftSlots, resultSlots, 0, playerX + 144, 25));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new Slot(craftSlots, col + row * 2, playerX + 90 + col * 18, 15 + row * 18));
            }
        }
        slotsChanged(craftSlots);
    }

    public HandyBagMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, readBagSlot(inventory, buf));
    }

    private static int readBagSlot(Inventory inventory, FriendlyByteBuf buf) {
        int fallback = ItemHandyBag.findOpenableBagSlot(inventory.player);
        if (fallback < 0) {
            fallback = inventory.selected;
        }
        return EnderMenu.readMenuVarInt(buf, fallback);
    }

    public int getBagTier() {
        return bagTier;
    }

    public int getBagSlots() {
        return bagSlots;
    }

    public int getSelectedModule() {
        return ModularHelper.data(player.getInventory().getItem(bagSlot)).selectedLinkCrystal();
    }

    public ItemStack getBag() {
        return player.getInventory().getItem(bagSlot);
    }

    public boolean hasSelectedCard() {
        return !ItemHandyBag.selectedCard(getBag()).isEmpty();
    }

    private static ResourceLocation emptyArmorIcon(EquipmentSlot type) {
        return switch (type) {
            case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            default -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
        };
    }

    private void addPlayerSlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (player.level().isClientSide) {
            return;
        }
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .map(RecipeHolder::value)
                .map(recipe -> recipe.assemble(input, player.level().registryAccess()))
                .orElse(ItemStack.EMPTY);
        resultSlots.setItem(0, result);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, craftSlots);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == bagSlot) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void performItemGuiAction(Player player, int action, int element) {
        ItemStack bag = getBag();
        if (!(bag.getItem() instanceof ItemHandyBag)) {
            return;
        }
        if (action == ItemHandyBag.GUI_ACTION_SELECT_MODULE && element >= 0 && element < MODULE_SLOTS) {
            ModularHelper.setData(bag, ModularHelper.data(bag).withSelectedLinkCrystal(element));
        } else if (action == ItemHandyBag.GUI_ACTION_TOGGLE_MODES) {
            int mode = element & 0x3;
            boolean reverse = (element & 0x8000) != 0;
            if (mode == 0) {
                ModularHelper.setData(bag, ModularHelper.data(bag).withBindLocked(!ItemHandyBag.isLocked(bag)));
            } else if (mode == 1) {
                ItemHandyBag.cycleCardField(bag, 0, reverse);
            } else if (mode == 2) {
                ItemHandyBag.cycleCardField(bag, 2, reverse);
            }
        } else if (action == ItemHandyBag.GUI_ACTION_TOGGLE_SHIFTCLICK) {
            int raw = ItemHandyBag.packedMode(bag);
            ItemHandyBag.setPackedMode(bag, ItemHandyBag.cycleField(raw, 0, element == 1));
        } else if (action == ItemHandyBag.GUI_ACTION_TOGGLE_SHIFTCLICK_DOUBLETAP) {
            if (ItemHandyBag.getShiftMode(bag) == 2) {
                ItemHandyBag.setPackedMode(bag, ItemHandyBag.packedMode(bag) ^ 0x80);
            }
        } else if (action == ItemHandyBag.GUI_ACTION_TOGGLE_BLOCK && element >= 0 && element <= 2) {
            ItemHandyBag.toggleSectionMask(bag, element, 0);
        } else if (action == ItemHandyBag.GUI_ACTION_TOGGLE_UPDATE && element >= 0 && element <= 2) {
            ItemHandyBag.toggleSectionMask(bag, element, 1);
        } else if (action == ItemHandyBag.GUI_ACTION_MOVE_ITEMS) {
            boolean includeHotbar = (element & 0x8000) != 0;
            moveItems(player, element & 0x7, includeHotbar);
        } else if (action == ItemHandyBag.GUI_ACTION_SORT_ITEMS && element >= 0 && element <= 3) {
            sortSection(player, element);
        } else if (action == ItemHandyBag.GUI_ACTION_OPEN_CURIOS
                && dev.celli.enderutilities.compat.ModCompat.curiosLoaded()) {
            player.closeContainer();
        }
    }

    private void moveItems(Player player, int kind, boolean includeHotbar) {
        var playerInv = new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(player.getInventory());
        var bagInv = ItemHandyBag.unlockedCardInventory(getBag());
        int start = includeHotbar ? 0 : 9;
        switch (kind) {
            case 0 -> InventoryHelper.moveAll(playerInv, bagInv, start, 36);
            case 1 -> InventoryHelper.moveMatching(playerInv, bagInv, start, 36);
            case 2 -> InventoryHelper.leaveOneFullStack(playerInv, bagInv);
            case 3 -> InventoryHelper.fillMatching(bagInv, playerInv);
            case 4 -> InventoryHelper.moveMatching(bagInv, playerInv, 0, bagInv.getSlots());
            case 5 -> InventoryHelper.moveAll(bagInv, playerInv, 0, bagInv.getSlots());
            default -> {
            }
        }
    }

    private void sortSection(Player player, int section) {
        if (section == 3) {
            var playerInv = new net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper(player.getInventory());
            sortRange(playerInv, 9, 36);
            return;
        }
        ItemStack bag = getBag();
        if (!(bag.getItem() instanceof ItemHandyBag handy)) {
            return;
        }
        int[] range = handy.tier() == ItemHandyBag.TIER_2 ? ItemHandyBag.sectionRange(section) : new int[] {0, handy.inventorySize()};
        sortRange(ItemHandyBag.cardInventory(bag), range[0], range[1]);
    }

    private static void sortRange(net.neoforged.neoforge.items.IItemHandlerModifiable inv, int from, int to) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = from; i < to && i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        stacks.sort((a, b) -> {
            int name = a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
            return name != 0 ? name : Integer.compare(b.getCount(), a.getCount());
        });
        int slot = from;
        for (ItemStack stack : stacks) {
            if (slot >= to) {
                break;
            }
            inv.setStackInSlot(slot++, stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int customEnd = bagSlots + MODULE_SLOTS;
            // The player inventory (27 main + 9 hotbar) is added right after the module slots, but the
            // armor/offhand/result/crafting slots are added AFTER it, so slots.size() is not the end of the
            // player inventory. Since reverseDirection=true walks the range from the end, those trailing
            // slots are filled first - which is why items used to end up in the 2x2 crafting grid.
            int playerEnd = customEnd + 36;
            if (index == craftResultIndex) {
                if (!moveItemStackTo(stack, customEnd, playerEnd, true) && !moveItemStackTo(stack, 0, bagSlots, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (index < bagSlots) {
                // Moving out of the bag: at most one normal stack per click, like the original mod (which
                // capped the transfer at the destination slot's limit) and only ever into the player inventory.
                ItemStack before = stack.copy();
                int want = Math.min(before.getCount(), Math.max(1, before.getMaxStackSize()));
                ItemStack toMove = before.copyWithCount(want);
                if (!moveItemStackTo(toMove, customEnd, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
                int moved = want - toMove.getCount();
                if (moved > 0) {
                    int remaining = before.getCount() - moved;
                    slot.set(remaining <= 0 ? ItemStack.EMPTY : before.copyWithCount(remaining));
                }
                // AbstractContainerMenu#clicked keeps calling quickMoveStack as long as the return value is
                // a non-empty stack that still matches the slot, so returning "result" here would keep
                // moving until the slot is empty. Returning empty tells the caller to stop after one stack.
                return ItemStack.EMPTY;
            } else if (index < customEnd) {
                if (!moveItemStackTo(stack, customEnd, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ItemHandyBag.getEffectiveShiftMode(getBag()) == 1) {
                int playerStart = customEnd;
                boolean hotbar = index >= playerStart + 27 && index < playerStart + 36;
                if (hotbar) {
                    if (!moveItemStackTo(stack, playerStart, playerStart + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, playerStart + 27, playerStart + 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, bagSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getItem(bagSlot).getItem() instanceof ItemHandyBag;
    }
}
