package org.provim.servercore.mixin.optimizations.ticking;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.provim.servercore.interfaces.ILevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * From: Airplane (Optimize-random-calls-in-chunk-ticking.patch)
 * License: GPL-3.0 (licenses/GPL.md)
 */

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin implements ILevelChunk {
    // Airplane - instead of using a random every time the chunk is ticked, define when lightning strikes preemptively.
    @Unique
    private int lightningTick;

    // shouldDoLightning compiles down to 29 bytes,
    // which with the default of 35 byte inlining should guarantee an inline.
    @Override
    public final int shouldDoLightning(Random random) {
        if (this.lightningTick-- <= 0) {
            this.lightningTick = random.nextInt(100000) << 1;
            return 0;
        }
        return -1;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/ticks/LevelChunkTicks;Lnet/minecraft/world/ticks/LevelChunkTicks;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;Lnet/minecraft/world/level/levelgen/blending/BlendingData;)V", at = @At("RETURN"))
    private void initLightingTick(Level level, ChunkPos chunkPos, UpgradeData upgradeData, LevelChunkTicks levelChunkTicks, LevelChunkTicks levelChunkTicks2, long l, LevelChunkSection[] levelChunkSections, LevelChunk.PostLoadProcessor postLoadProcessor, BlendingData blendingData, CallbackInfo ci) {
        this.lightningTick = level.random.nextInt(100000) << 1;
    }
}