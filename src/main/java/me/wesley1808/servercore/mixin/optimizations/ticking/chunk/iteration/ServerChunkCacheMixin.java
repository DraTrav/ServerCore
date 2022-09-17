package me.wesley1808.servercore.mixin.optimizations.ticking.chunk.iteration;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.wesley1808.servercore.common.collections.CachedChunkList;
import me.wesley1808.servercore.common.interfaces.chunk.IServerChunkCache;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = ServerChunkCache.class, priority = 900)
public class ServerChunkCacheMixin implements IServerChunkCache {
    @Unique
    private final ReferenceOpenHashSet<ChunkHolder> requiresBroadcast = new ReferenceOpenHashSet<>();
    @Unique
    private CachedChunkList cachedChunks;
    @Unique
    private boolean isChunkLoaded;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void servercore$onInit(ServerLevel level, LevelStorageSource.LevelStorageAccess storageAccess, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor executor, ChunkGenerator chunkGenerator, int i, int j, boolean bl, ChunkProgressListener chunkProgressListener, ChunkStatusUpdateListener chunkStatusUpdateListener, Supplier supplier, CallbackInfo ci) {
        this.cachedChunks = new CachedChunkList(level);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void servercore$onSave(boolean bl, CallbackInfo ci) {
        this.cachedChunks.shouldTrim();
    }

    // Avoids unnecessary array allocations.
    @Redirect(
            method = "tickChunks",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Lists;newArrayListWithCapacity(I)Ljava/util/ArrayList;",
                    remap = false
            )
    )
    private ArrayList<?> servercore$noList(int initialArraySize) {
        return null;
    }

    // Replaces the list variable with our own.
    @ModifyVariable(
            method = "tickChunks",
            index = 12,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    private List<?> servercore$replaceList(List<?> list) {
        return this.cachedChunks;
    }

    // Updates our own list and prevents vanilla from adding chunks to it.
    @Redirect(
            method = "tickChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;",
                    ordinal = 0
            )
    )
    private Iterator<ChunkHolder> servercore$updateCachedChunks(Iterable<ChunkHolder> holders) {
        this.cachedChunks.update(holders);
        return Collections.emptyIterator();
    }

    // Don't shuffle the chunk list.
    @Redirect(
            method = "tickChunks",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V"
            )
    )
    private void servercore$cancelShuffle(List<?> list) {
        // NO-OP
    }

    @Inject(
            method = "tickChunks",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;isNaturalSpawningAllowed(Lnet/minecraft/world/level/ChunkPos;)Z",
                    shift = At.Shift.BEFORE
            )
    )
    private void servercore$setLoaded(CallbackInfo ci, long l, long m, boolean bl, LevelData levelData, ProfilerFiller profilerFiller, int i, boolean bl2, int j, NaturalSpawner.SpawnState spawnState, List list, boolean bl3, Iterator var14, ServerChunkCache.ChunkAndHolder chunkAndHolder, LevelChunk chunk, ChunkPos pos) {
        this.isChunkLoaded = chunk.loaded;
    }

    @Redirect(
            method = "tickChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;isNaturalSpawningAllowed(Lnet/minecraft/world/level/ChunkPos;)Z"
            )
    )
    private boolean servercore$skipUnloadedChunks(ServerLevel level, ChunkPos pos) {
        return this.isChunkLoaded;
    }

    @Redirect(
            method = "tickChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;anyPlayerCloseEnoughForSpawning(Lnet/minecraft/world/level/ChunkPos;)Z"
            )
    )
    private boolean servercore$skipCheck(ChunkMap chunkMap, ChunkPos pos) {
        return true;
    }

    @Redirect(
            method = "tickChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                    ordinal = 0
            )
    )
    private void servercore$broadcastChanges(List<ServerChunkCache.ChunkAndHolder> list, Consumer<ServerChunkCache.ChunkAndHolder> consumer) {
        for (ChunkHolder holder : this.requiresBroadcast) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null) {
                holder.broadcastChanges(chunk);
            }
        }
        this.requiresBroadcast.clear();
    }

    @Override
    public void requiresBroadcast(ChunkHolder holder) {
        this.requiresBroadcast.add(holder);
    }
}