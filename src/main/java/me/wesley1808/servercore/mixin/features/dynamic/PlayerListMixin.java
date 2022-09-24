package me.wesley1808.servercore.mixin.features.dynamic;

import me.wesley1808.servercore.common.dynamic.DynamicSetting;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "setViewDistance", at = @At("HEAD"))
    private void servercore$updateViewDistance(int i, CallbackInfo ci) {
        DynamicSetting.VIEW_DISTANCE.set(i, false);
    }

    @Inject(method = "setSimulationDistance", at = @At("HEAD"))
    private void servercore$updateSimulationDistance(int i, CallbackInfo ci) {
        DynamicSetting.SIMULATION_DISTANCE.set(i, false);
    }
}