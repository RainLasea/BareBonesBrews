package lasea.barebonesbrews.compat.jade;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import lasea.barebonesbrews.BareBonesBrews;
import lasea.barebonesbrews.brewing.CauldronBrewBlockEntity;
import lasea.barebonesbrews.compat.HexaliaCauldronView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

/** Server-authoritative Jade view for both native and Hexalia cauldrons. */
public enum CauldronJadeProvider implements IBlockComponentProvider,
        IServerDataProvider<BlockAccessor> {

    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            BareBonesBrews.MODID, "cauldron_brewing");
    private static final String ROOT = "BareBonesBrews";
    private static final String ITEMS = "Items";
    private static final String HEXALIA_BLOCK_ENTITY =
            "net.astralya.hexalia.block.entity.custom.SmallCauldronBlockEntity";

    private static volatile Field hexaliaContentsField;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        List<ItemStack> ingredients;
        boolean active;

        if (blockEntity instanceof CauldronBrewBlockEntity brew) {
            ingredients = brew.floatingItems();
            active = brew.hasBrew();
        } else if (blockEntity != null
                && blockEntity.getClass().getName().equals(HEXALIA_BLOCK_ENTITY)) {
            HexaliaCauldronView view = hexaliaView(blockEntity);
            if (view == null) {
                return;
            }
            ingredients = view.barebonesbrews$displayIngredients();
            active = view.barebonesbrews$hasActiveContents();
        } else {
            return;
        }

        if (!active) {
            return;
        }
        CompoundTag payload = new CompoundTag();
        ListTag itemTags = new ListTag();
        ingredients.stream().filter(stack -> !stack.isEmpty())
                .forEach(stack -> itemTags.add(stack.save(accessor.getLevel().registryAccess())));
        payload.put(ITEMS, itemTags);
        data.put(ROOT, payload);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(ROOT, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag payload = accessor.getServerData().getCompound(ROOT);
        IElementHelper helper = IElementHelper.get();
        List<ItemStack> ingredients = readItems(payload, accessor);
        if (!ingredients.isEmpty()) {
            tooltip.add(Component.translatable("jade.barebonesbrews.ingredients"));
            tooltip.append(helper.spacer(4, 0));
            ingredients.forEach(stack -> tooltip.append(helper.item(stack)));
        }
    }

    static boolean hasBrewingData(BlockAccessor accessor) {
        return accessor.getServerData().contains(ROOT, Tag.TAG_COMPOUND);
    }

    private static List<ItemStack> readItems(CompoundTag payload, BlockAccessor accessor) {
        List<ItemStack> stacks = new ArrayList<>();
        ListTag list = payload.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(accessor.getLevel().registryAccess(),
                    list.getCompound(index));
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static HexaliaCauldronView hexaliaView(BlockEntity blockEntity) {
        try {
            Field field = hexaliaContentsField;
            if (field == null) {
                field = blockEntity.getClass().getDeclaredField("contents");
                field.setAccessible(true);
                hexaliaContentsField = field;
            }
            Object contents = field.get(blockEntity);
            return contents instanceof HexaliaCauldronView view ? view : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
