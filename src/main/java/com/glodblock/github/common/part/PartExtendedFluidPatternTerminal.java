package com.glodblock.github.common.part;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartModel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.sync.GuiBridge;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.reporting.PartExpandedProcessingPatternTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidEncodedPattern;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.inventory.ExAppEngInternalInventory;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.util.Util;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class PartExtendedFluidPatternTerminal extends PartExpandedProcessingPatternTerminal {

    private boolean combine = false;
    private boolean fluidFirst = false;

    @PartModels
    public static ResourceLocation[] MODELS = new ResourceLocation[] {
            new ResourceLocation(FluidCraft.MODID, "part/f_pattern_ex_term_on"), // 0
            new ResourceLocation(FluidCraft.MODID, "part/f_pattern_ex_term_off"), // 1
    };

    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODELS[0], MODEL_STATUS_ON);
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODELS[1], MODEL_STATUS_OFF);
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODELS[0], MODEL_STATUS_HAS_CHANNEL);

    public PartExtendedFluidPatternTerminal(ItemStack is) {
        super(is);
        ExAppEngInternalInventory exCraft = new ExAppEngInternalInventory((AppEngInternalInventory) getInventoryByName("crafting"));
        ExAppEngInternalInventory exOutput = new ExAppEngInternalInventory((AppEngInternalInventory) getInventoryByName("output"));
        this.crafting = exCraft;
        this.output = exOutput;
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d pos) {
        TileEntity te = this.getTile();
        BlockPos tePos = te.getPos();
        if (Platform.isWrench(player, player.inventory.getCurrentItem(), tePos)) {
            return super.onPartActivate(player, hand, pos);
        }
        if (Platform.isServer()) {
            if (GuiBridge.GUI_EXPANDED_PROCESSING_PATTERN_TERMINAL.hasPermissions(te, tePos.getX(), tePos.getY(), tePos.getZ(), getSide(), player)) {
                InventoryHandler.openGui(player, te.getWorld(), tePos, getSide().getFacing(), GuiType.FLUID_EXTENDED_PATTERN_TERMINAL);
            } else {
                Platform.openGUI(player, this.getHost().getTile(), this.getSide(), GuiBridge.GUI_ME);
            }
        }
        return true;
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc, ItemStack removedStack,
                                  ItemStack newStack) {
        if (slot == 1) {
            final ItemStack is = inv.getStackInSlot(1);
            if (!is.isEmpty() && is.getItem() instanceof ItemFluidEncodedPattern) {
                final ItemFluidEncodedPattern pattern = (ItemFluidEncodedPattern) is.getItem();
                final ICraftingPatternDetails details = pattern.getPatternForItem( is, this.getHost().getTile().getWorld() );
                if( details != null )
                {

                    for( int x = 0; x < this.getInventoryByName("crafting").getSlots(); x ++ ) {
                        ((AppEngInternalInventory) this.getInventoryByName("crafting")).setStackInSlot(x, ItemStack.EMPTY);
                    }

                    for( int x = 0; x < this.getInventoryByName("output").getSlots(); x ++ ) {
                        ((AppEngInternalInventory) this.getInventoryByName("output")).setStackInSlot(x, ItemStack.EMPTY);
                    }

                    for( int x = 0; x < this.getInventoryByName("crafting").getSlots() && x < details.getInputs().length; x++ )
                    {
                        final IAEItemStack item = details.getInputs()[x];
                        if (item != null && item.getItem() instanceof ItemFluidDrop) {
                            ItemStack packet = ItemFluidPacket.newStack(ItemFluidDrop.getFluidStack(item.createItemStack()));
                            ((AppEngInternalInventory) this.getInventoryByName("crafting")).setStackInSlot(x, packet);
                        }
                        else ((AppEngInternalInventory) this.getInventoryByName("crafting")).setStackInSlot( x, item == null ? ItemStack.EMPTY : item.createItemStack() );
                    }

                    for( int x = 0; x < this.getInventoryByName("output").getSlots() && x < details.getOutputs().length; x++ )
                    {
                        final IAEItemStack item = details.getOutputs()[x];
                        if (item != null && item.getItem() instanceof ItemFluidDrop) {
                            ItemStack packet = ItemFluidPacket.newStack(ItemFluidDrop.getFluidStack(item.createItemStack()));
                            ((AppEngInternalInventory) this.getInventoryByName("output")).setStackInSlot(x, packet);
                        }
                        else ((AppEngInternalInventory) this.getInventoryByName("output")).setStackInSlot( x, item == null ? ItemStack.EMPTY : item.createItemStack() );
                    }
                }
                this.getHost().markForSave();
                return;
            }
        }
        super.onChangeInventory(inv, slot, mc, removedStack, newStack);
    }

    public void onChangeCrafting(HashMap<Integer, ItemStack[]> inputs, ItemStack[] outputs, boolean combine) {
        IItemHandler crafting = this.getInventoryByName("crafting");
        IItemHandler output = this.getInventoryByName("output");
        IItemList<IAEItemStack> storageList = this.getInventory(Util.ITEM) == null ?
                null : this.getInventory(Util.ITEM).getStorageList();
        if (crafting instanceof AppEngInternalInventory && output instanceof AppEngInternalInventory) {
            Util.clearItemInventory((IItemHandlerModifiable) crafting);
            Util.clearItemInventory((IItemHandlerModifiable) output);
            ItemStack[] fuzzyFind = new ItemStack[Util.findMax(inputs.keySet()) + 1];
            for (int index : inputs.keySet()) {
                Util.fuzzyTransferItems(index, inputs.get(index), fuzzyFind, storageList);
            }
            if (combine) {
                fuzzyFind = Util.compress(fuzzyFind);
            }
            int bound = Math.min(crafting.getSlots(), fuzzyFind.length);
            for (int x = 0; x < bound; x++) {
                final ItemStack item = fuzzyFind[x];
                ((AppEngInternalInventory) crafting).setStackInSlot(x, item == null ? ItemStack.EMPTY : item);
            }
            bound = Math.min(output.getSlots(), outputs.length);
            for (int x = 0; x < bound; x++) {
                final ItemStack item = outputs[x];
                ((AppEngInternalInventory) output).setStackInSlot(x, item == null ? ItemStack.EMPTY : item);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        combine = data.getBoolean("combineMode");
        fluidFirst = data.getBoolean("fluidFirst");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("combineMode", combine);
        data.setBoolean("fluidFirst", fluidFirst);
    }

    public void setCombineMode(boolean value) {
        this.combine = value;
    }

    public boolean getCombineMode() {
        return this.combine;
    }

    public void setFluidPlaceMode(boolean value) {
        this.fluidFirst = value;
    }

    public boolean getFluidPlaceMode() {
        return this.fluidFirst;
    }

}
