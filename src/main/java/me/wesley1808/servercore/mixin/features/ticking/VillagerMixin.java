package me.wesley1808.servercore.mixin.features.ticking;

import me.wesley1808.servercore.config.tables.FeatureConfig;
import me.wesley1808.servercore.utils.ChunkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * From: Purpur (Lobotomize-stuck-villagers.patch)
 */

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
    @Unique
    private boolean lobotomized = false;

    private VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"
            )
    )
    private void shouldTickBrain(Brain<Villager> brain, ServerLevel level, LivingEntity livingEntity) {
        if (!FeatureConfig.LOBOTOMIZE_VILLAGERS.get() || !isLobotomized() || this.tickCount % FeatureConfig.LOBOTOMIZED_TICK_INTERVAL.get() == 0) {
            brain.tick(level, (Villager) (Object) this);
        }
    }

    private boolean isLobotomized() {
        if (this.tickCount % 300 == 0) {
            this.lobotomized = this.getVehicle() != null || !canTravel(this.blockPosition());
        }

        return this.lobotomized;
    }

    private boolean canTravel(BlockPos pos) {
        return canTravelTo(pos.east()) || canTravelTo(pos.west()) || canTravelTo(pos.north()) || canTravelTo(pos.south());
    }

    private boolean canTravelTo(BlockPos pos) {
        // Returns true in case it's surrounded by any bed. This way we don't break iron farms.
        final BlockState state = ChunkManager.getStateIfLoaded(this.level, pos);
        if (state.getBlock() instanceof BedBlock) {
            return true;
        }

        Path path = this.getNavigation().createPath(pos, 0);
        return path != null && path.canReach();
    }
}