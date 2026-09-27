package lasea.barebonesbrews.compat.client;

import java.util.List;

import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipe;
import lasea.barebonesbrews.compat.RoughBrewingDisplayRecipes;
import net.minecraft.client.Minecraft;

public final class ClientBrewingRecipes {
    private ClientBrewingRecipes() {}

    public static List<RoughBrewingDisplayRecipe> create() {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? List.of()
                : RoughBrewingDisplayRecipes.create(connection.getRecipeManager(), connection.registryAccess());
    }
}
