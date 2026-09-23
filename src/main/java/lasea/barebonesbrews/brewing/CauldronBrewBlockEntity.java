package lasea.barebonesbrews.brewing;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lasea.barebonesbrews.recipe.CauldronBrewingRecipe;
import lasea.barebonesbrews.Config;
import lasea.barebonesbrews.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** The single authoritative state for one brewing cauldron. */
public final class CauldronBrewBlockEntity extends BlockEntity {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_RECIPE = "Recipe";
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_DURATION = "Duration";
    private static final String TAG_COLOR = "Color";
    private static final String TAG_READY = "Ready";
    private static final String TAG_COMPLETED_AT = "CompletedAt";
    private static final String TAG_RESULT = "Result";
    private static final String TAG_SERVINGS = "Servings";
    private static final String TAG_PREVIOUS_RESULT = "PreviousResult";
    private static final String TAG_TANK = "Tank";
    private static final ResourceLocation STORED_RECIPE = ResourceLocation.fromNamespaceAndPath(
            lasea.barebonesbrews.BareBonesBrews.MODID, "stored_fluid");
    private static final ResourceLocation POST_PROCESS_RECIPE = ResourceLocation.fromNamespaceAndPath(
            lasea.barebonesbrews.BareBonesBrews.MODID, "post_process");
    public static final int COMPLETION_SINK_TICKS = 36;

    private final List<ItemStack> ingredients = new ArrayList<>();
    @Nullable private ResourceLocation recipeId;
    private int progress;
    private int duration;
    private int brewColor;
    private boolean ready;
    private long completedAtGameTime = -1L;
    private ItemStack result = ItemStack.EMPTY;
    private ItemStack previousResult = ItemStack.EMPTY;
    private final CauldronTank fluidTank = new CauldronTank();
    private boolean updatingFluidState;

    public CauldronBrewBlockEntity(BlockPos pos, BlockState state) {
        super(ModBrewing.CAULDRON_BLOCK_ENTITY.get(), pos, state);
        fluidTank.setFluid(waterForState(state));
    }

    public boolean addIngredient(ServerLevel level, ItemStack held) {
        if (!Config.roughPotionsEnabled() || !Config.cauldronRecipesEnabled()) {
            return false;
        }
        if (ready) {
            return startPostProcessing(level, held);
        }
        if (POST_PROCESS_RECIPE.equals(recipeId) || ingredients.size() >= CauldronBrewing.MAX_INGREDIENTS
                || !fluidTank.getFluid().is(Fluids.WATER)
                || fluidTank.getFluidAmount() != FluidType.BUCKET_VOLUME
                || !CauldronBrewing.canStoreIngredient(held)) {
            return false;
        }

        ingredients.add(held.copyWithCount(1));
        refreshRecipe(level);
        if (recipeId == null) {
            CauldronBrewSoundscape.ingredientAdded(level, worldPosition);
        }
        sync();
        return true;
    }

    private void refreshRecipe(ServerLevel level) {
        // Ingredient changes invalidate both the old result and its elapsed cooking time.
        resetActiveRecipe();
        RecipeHolder<CauldronBrewingRecipe> match = CauldronBrewing.findExactRecipe(level, ingredients);
        if (match != null) {
            lockRecipe(level, match);
            CauldronBrewSoundscape.brewingStarted(level, worldPosition);
        }
    }

    private void lockRecipe(ServerLevel level, RecipeHolder<CauldronBrewingRecipe> match) {
        CauldronBrewingRecipe recipe = match.value();
        recipeId = match.id();
        progress = 0;
        duration = recipe.getDuration();
        PotionContents contents = recipe.getResultItem(level.registryAccess()).get(DataComponents.POTION_CONTENTS);
        result = recipe.getResultItem(level.registryAccess()).copy();
        brewColor = contents == null ? 0 : contents.getColor();
        ready = false;
        completedAtGameTime = -1L;
    }

    private boolean startPostProcessing(ServerLevel level, ItemStack reagent) {
        ItemStack current = ModFluids.toBottle(fluidTank.getFluid());
        ItemStack transformed = RoughPotionBrewing.transform(current, reagent);
        if (transformed.isEmpty()) {
            return false;
        }
        ingredients.clear();
        ingredients.add(reagent.copyWithCount(1));
        previousResult = current;
        result = transformed;
        recipeId = POST_PROCESS_RECIPE;
        progress = 0;
        duration = RoughPotionBrewing.POST_PROCESS_DURATION;
        ready = false;
        completedAtGameTime = -1L;
        CauldronBrewSoundscape.brewingStarted(level, worldPosition);
        sync();
        return true;
    }

    @Nullable
    public ItemStack takeResult(ServerLevel level) {
        if (!ready || !ModFluids.isValidPotion(fluidTank.getFluid())
                || fluidTank.getFluidAmount() < ModFluids.BOTTLE_VOLUME) {
            return null;
        }
        ItemStack bottled = ModFluids.toBottle(fluidTank.drain(ModFluids.BOTTLE_VOLUME, IFluidHandler.FluidAction.EXECUTE));
        if (bottled.isEmpty()) {
            return null;
        }
        CauldronBrewSoundscape.bottled(level, worldPosition);
        return bottled;
    }

    public boolean canTakeIngredient() {
        return !ready && !ingredients.isEmpty()
                && (!POST_PROCESS_RECIPE.equals(recipeId) || !previousResult.isEmpty());
    }

    /** Returns the oldest visible ingredient, then re-evaluates the remaining contents. */
    public ItemStack takeOldestIngredient(ServerLevel level) {
        if (!canTakeIngredient()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ingredients.remove(0);
        if (POST_PROCESS_RECIPE.equals(recipeId) && !previousResult.isEmpty()) {
            result = previousResult;
            previousResult = ItemStack.EMPTY;
            ready = true;
            progress = duration;
            completedAtGameTime = level.getGameTime();
            PotionContents contents = result.get(DataComponents.POTION_CONTENTS);
            brewColor = contents == null ? 0 : contents.getColor();
        } else {
            refreshRecipe(level);
        }
        sync();
        return removed;
    }

    private void resetActiveRecipe() {
        recipeId = null;
        progress = 0;
        duration = 0;
        brewColor = 0;
        completedAtGameTime = -1L;
        result = ItemStack.EMPTY;
        previousResult = ItemStack.EMPTY;
    }

    public void serverTick(ServerLevel level) {
        if (recipeId == null) {
            return;
        }
        if (ready) {
            if (completedAtGameTime < 0L) {
                completedAtGameTime = level.getGameTime();
                sync();
            } else if (!ingredients.isEmpty()
                    && level.getGameTime() - completedAtGameTime >= COMPLETION_SINK_TICKS) {
                ingredients.clear();
                sync();
            }
            return;
        }
        if (!getBlockState().is(Blocks.WATER_CAULDRON)) {
            clear();
            return;
        }
        if (!CauldronBrewing.isHeated(level, worldPosition)) {
            return;
        }
        progress++;
        CauldronBrewSoundscape.brewingTick(level, worldPosition, progress);
        setChanged();
        if (progress >= Math.max(1, duration)) {
            progress = Math.max(1, duration);
            PotionContents contents = result.get(DataComponents.POTION_CONTENTS);
            brewColor = contents == null ? 0 : contents.getColor();
            ready = true;
            int amount = POST_PROCESS_RECIPE.equals(recipeId) ? fluidTank.getFluidAmount() : ModFluids.BREW_VOLUME;
            fluidTank.setFluid(ModFluids.fromBottle(result, amount));
            previousResult = ItemStack.EMPTY;
            completedAtGameTime = level.getGameTime();
            sync();
        }
    }

    private void clear() {
        ingredients.clear();
        recipeId = null;
        progress = 0;
        duration = 0;
        brewColor = 0;
        ready = false;
        completedAtGameTime = -1L;
        result = ItemStack.EMPTY;
        previousResult = ItemStack.EMPTY;
        sync();
    }

    @Override
    public void setBlockState(BlockState state) {
        BlockState previous = getBlockState();
        super.setBlockState(state);
        // Buckets, washing, rain and burning entities must not leave stale servings behind.
        if (!updatingFluidState && !previous.equals(state) && level != null && !level.isClientSide) {
            clear();
            fluidTank.setFluid(waterForState(state));
            sync();
        }
    }

    /** This is the same storage used by bottling, saving, Jade and external fluid pipes. */
    @Nullable
    public IFluidHandler fluidHandler() {
        return supportsFluidStorage() ? fluidTank : null;
    }

    public boolean containsPotion() {
        return ModFluids.isPotion(fluidTank.getFluid());
    }

    public int fluidColor() {
        return fluidTank.getFluid().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
    }

    public boolean canBottlePotion() {
        return ready && containsPotion() && fluidTank.getFluidAmount() >= ModFluids.BOTTLE_VOLUME;
    }

    public boolean hasExactVanillaWaterLevel() {
        return FluidStack.matches(fluidTank.getFluid(), waterForState(getBlockState()));
    }

    private boolean supportsFluidStorage() {
        return getBlockState().is(Blocks.CAULDRON) || getBlockState().is(Blocks.WATER_CAULDRON);
    }

    private static FluidStack waterForState(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON)
                ? new FluidStack(Fluids.WATER, state.getValue(LayeredCauldronBlock.LEVEL) * FluidType.BUCKET_VOLUME / 3)
                : FluidStack.EMPTY;
    }

    private void fluidContentsChanged() {
        if (fluidTank.isEmpty()) {
            clear();
        } else if (ModFluids.isPotion(fluidTank.getFluid())) {
            ready = true;
            result = ModFluids.toBottle(fluidTank.getFluid());
            if (recipeId == null) {
                recipeId = STORED_RECIPE;
            }
            brewColor = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
        }
        updateFluidBlockState();
        sync();
    }

    private void updateFluidBlockState() {
        if (level == null || level.isClientSide) {
            return;
        }
        int full = ModFluids.isPotion(fluidTank.getFluid()) ? ModFluids.BREW_VOLUME : FluidType.BUCKET_VOLUME;
        int layers = Math.min(3, (fluidTank.getFluidAmount() * 3 + full - 1) / full);
        BlockState target = layers == 0 ? Blocks.CAULDRON.defaultBlockState()
                : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, layers);
        if (target.equals(getBlockState())) {
            return;
        }
        // Changing the vanilla block (empty <-> water) replaces its block entity.
        // Transfer the exact tank contents to the replacement instead of rounding to its water level.
        CompoundTag snapshot = saveWithoutMetadata(level.registryAccess());
        updatingFluidState = true;
        try {
            level.setBlockAndUpdate(worldPosition, target);
            if (level.getBlockEntity(worldPosition) instanceof CauldronBrewBlockEntity replacement && replacement != this) {
                replacement.loadWithComponents(snapshot, level.registryAccess());
                replacement.sync();
            }
        } finally {
            updatingFluidState = false;
        }
    }

    private final class CauldronTank extends FluidTank {
        private CauldronTank() {
            super(FluidType.BUCKET_VOLUME, stack -> (stack.is(Fluids.WATER) && stack.isComponentsPatchEmpty())
                    || ModFluids.isValidPotion(stack));
        }

        @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? getFluid().copy() : FluidStack.EMPTY;
        }

        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? (ModFluids.isPotion(getFluid()) ? ModFluids.BREW_VOLUME : FluidType.BUCKET_VOLUME) : 0;
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isFluidValid(stack);
        }

        private boolean canTransfer() {
            return !isRemoved() && supportsFluidStorage() && level != null && !level.isClientSide
                    && !isBrewing() && (ready || ingredients.isEmpty());
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!canTransfer() || resource.isEmpty()) {
                return 0;
            }
            int limit = ModFluids.isPotion(resource) ? ModFluids.BREW_VOLUME : FluidType.BUCKET_VOLUME;
            int space = Math.max(0, limit - getFluidAmount());
            return space == 0 ? 0 : super.fill(resource.copyWithAmount(Math.min(space, resource.getAmount())), action);
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return canTransfer() && maxDrain > 0 ? super.drain(maxDrain, action) : FluidStack.EMPTY;
        }

        @Override protected void onContentsChanged() {
            fluidContentsChanged();
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public int brewColor() {
        return brewColor;
    }

    public boolean isBrewing() {
        return recipeId != null && !ready;
    }

    public boolean isReady() {
        return ready;
    }

    public float completionProgress(float partialTick) {
        if (!ready || completedAtGameTime < 0L || level == null) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F,
                (level.getGameTime() + partialTick - completedAtGameTime) / COMPLETION_SINK_TICKS));
    }

    public boolean hasBrew() {
        return recipeId != null || !ingredients.isEmpty();
    }

    public List<ItemStack> floatingItems() {
        return List.copyOf(ingredients);
    }

    public int fillLevel() {
        return getBlockState().is(Blocks.WATER_CAULDRON)
                ? getBlockState().getValue(LayeredCauldronBlock.LEVEL) : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        ingredients.forEach(stack -> list.add(stack.save(registries)));
        tag.put(TAG_ITEMS, list);
        if (recipeId != null) {
            tag.putString(TAG_RECIPE, recipeId.toString());
        }
        tag.putInt(TAG_PROGRESS, progress);
        tag.putInt(TAG_DURATION, duration);
        tag.putInt(TAG_COLOR, brewColor);
        tag.putBoolean(TAG_READY, ready);
        if (completedAtGameTime >= 0L) {
            tag.putLong(TAG_COMPLETED_AT, completedAtGameTime);
        }
        if (!result.isEmpty()) {
            tag.put(TAG_RESULT, result.save(registries));
        }
        tag.put(TAG_TANK, fluidTank.writeToNBT(registries, new CompoundTag()));
        if (!previousResult.isEmpty()) {
            tag.put(TAG_PREVIOUS_RESULT, previousResult.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingredients.clear();
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                ingredients.add(stack);
            }
        }
        recipeId = tag.contains(TAG_RECIPE, Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(TAG_RECIPE)) : null;
        progress = Math.max(0, tag.getInt(TAG_PROGRESS));
        duration = Math.max(0, tag.getInt(TAG_DURATION));
        brewColor = tag.getInt(TAG_COLOR);
        ready = tag.getBoolean(TAG_READY);
        completedAtGameTime = tag.contains(TAG_COMPLETED_AT, Tag.TAG_LONG)
                ? tag.getLong(TAG_COMPLETED_AT) : -1L;
        result = tag.contains(TAG_RESULT, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(TAG_RESULT)) : ItemStack.EMPTY;
        previousResult = tag.contains(TAG_PREVIOUS_RESULT, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(TAG_PREVIOUS_RESULT)) : ItemStack.EMPTY;
        if (tag.contains(TAG_TANK, Tag.TAG_COMPOUND)) {
            fluidTank.readFromNBT(registries, tag.getCompound(TAG_TANK));
        } else if (ready || POST_PROCESS_RECIPE.equals(recipeId)) {
            // Migrate worlds from the original item-and-serving storage without losing effects.
            int servings = Math.clamp(tag.getInt(TAG_SERVINGS), 1, 3);
            fluidTank.setFluid(ModFluids.fromBottle(previousResult.isEmpty() ? result : previousResult,
                    servings * ModFluids.BOTTLE_VOLUME));
        } else {
            fluidTank.setFluid(waterForState(getBlockState()));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
