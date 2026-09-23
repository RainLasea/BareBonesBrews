package lasea.barebonesbrews.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve potion components in the recipe itself, including client recipe synchronization. */
@Pseudo
@Mixin(targets = "net.astralya.hexalia.recipe.SmallCauldronRecipe$Serializer", remap = false)
public abstract class HexaliaSmallCauldronSerializerMixin {
    @Shadow(remap = false) @Final @Mutable
    private static Codec<ItemStack> RESULT_CODEC;

    @Inject(method = "<clinit>", at = @At(value = "FIELD",
            target = "Lnet/astralya/hexalia/recipe/SmallCauldronRecipe$Serializer;RESULT_CODEC:Lcom/mojang/serialization/Codec;",
            opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER), remap = false)
    private static void barebonesbrews$preserveResultComponents(CallbackInfo callback) {
        // Keep Hexalia's existing {item, count} recipes readable alongside full {id, components} stacks.
        RESULT_CODEC = Codec.withAlternative(ItemStack.CODEC, RESULT_CODEC);
    }
}
