package lasea.barebonesbrews.brewing;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import lasea.barebonesbrews.recipe.CauldronBrewingRecipe;
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

/** The single authoritative state for one brewing cauldron. */
public final class CauldronBrewBlockEntity extends BlockEntity {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_RECIPE = "Recipe";
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_DURATION = "Duration";
    private static final String TAG_COLOR = "Color";
    private static final String TAG_READY = "Ready";
    private static final String TAG_COMPLETED_AT = "CompletedAt";
    public static final int COMPLETION_SINK_TICKS = 36;

    private final List<ItemStack> ingredients = new ArrayList<>();
    @Nullable private ResourceLocation recipeId;
    private int progress;
    private int duration;
    private int brewColor;
    private boolean ready;
    private long completedAtGameTime = -1L;

    public CauldronBrewBlockEntity(BlockPos pos, BlockState state) {
        super(ModBrewing.CAULDRON_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean addIngredient(ServerLevel level, ItemStack held) {
        if (recipeId != null || ingredients.size() >= CauldronBrewing.MAX_INGREDIENTS
                || !CauldronBrewing.isFullWaterCauldron(getBlockState())
                || !CauldronBrewing.isPlausibleIngredient(held)) {
            return false;
        }

        ingredients.add(held.copyWithCount(1));
        RecipeHolder<CauldronBrewingRecipe> match = CauldronBrewing.findExactRecipe(level, ingredients);
        if (match != null) {
            lockRecipe(level, match);
            CauldronBrewSoundscape.brewingStarted(level, worldPosition);
        } else if (!CauldronBrewing.couldStillMatch(level, ingredients)) {
            ingredients.remove(ingredients.size() - 1);
            return false;
        } else {
            CauldronBrewSoundscape.ingredientAdded(level, worldPosition);
        }
        sync();
        return true;
    }

    private void lockRecipe(ServerLevel level, RecipeHolder<CauldronBrewingRecipe> match) {
        CauldronBrewingRecipe recipe = match.value();
        recipeId = match.id();
        progress = 0;
        duration = recipe.getDuration();
        PotionContents contents = recipe.getResultItem(level.registryAccess()).get(DataComponents.POTION_CONTENTS);
        brewColor = contents == null ? 0 : contents.getColor();
        ready = false;
        completedAtGameTime = -1L;
    }

    @Nullable
    public ItemStack takeResult(ServerLevel level) {
        if (!ready || recipeId == null) {
            return null;
        }
        var holder = level.getRecipeManager().byKey(recipeId);
        if (holder.isEmpty() || !(holder.get().value() instanceof CauldronBrewingRecipe recipe)) {
            clear();
            return null;
        }
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        clear();
        LayeredCauldronBlock.lowerFillLevel(getBlockState(), level, worldPosition);
        CauldronBrewSoundscape.bottled(level, worldPosition);
        return result;
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
            ready = true;
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
        sync();
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
