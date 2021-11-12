package org.provim.servercore.mixin.performance.maps;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MapItemSavedData.class, priority = 999)
public abstract class MapItemSavedDataMixin {

    @Shadow
    protected abstract void removeDecoration(String string);

    /**
     * Cancels inventory iteration from Maps in Item Frames to save performance.
     *
     * @return Boolean: whether moving player icons should be tracked by item frames.
     */

    @Redirect(method = "tickCarriedBy", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;contains(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean stopInvIteration(Inventory inventory, ItemStack stack) {
        return !stack.isFramed() && inventory.contains(stack);
    }

    // Fixes potential bugs with blinking (moving) player icons on player held maps.
    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;removeDecoration(Ljava/lang/String;)V", ordinal = 0))
    private void removeDecoration(MapItemSavedData data, String id, Player player, ItemStack stack) {
        if (!stack.isFramed()) {
            this.removeDecoration(id);
        }
    }

    /**
     * Replaces getString() in map updates with a faster alternative.
     *
     * @param component: The player name as text component
     * @return String: The ID string of the icon.
     */

    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String getString(net.minecraft.network.chat.Component component) {
        return ((TextComponent) component).getText();
    }
}