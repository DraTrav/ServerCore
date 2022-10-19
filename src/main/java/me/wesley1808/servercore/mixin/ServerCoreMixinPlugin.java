package me.wesley1808.servercore.mixin;

import me.wesley1808.servercore.common.ServerCore;
import me.wesley1808.servercore.common.config.tables.OptimizationConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ServerCoreMixinPlugin implements IMixinConfigPlugin {
    private String mixinPackage;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage + ".";
        ServerCore.onLoadMixins();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override // Disables specific mixins for mod compatibility.
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Cyclonite - Disabled activation range as it attempts to multithread entities.
        if (mixinClassName.startsWith(this.mixinPackage + "features.activation_range")) {
            return !this.isModLoaded("c3h6n6o6");
        }

        if (mixinClassName.startsWith(this.mixinPackage + "optimizations.sync_loads")) {
            return OptimizationConfig.REDUCE_SYNC_LOADS.get();
        }

        if (mixinClassName.startsWith(this.mixinPackage + "optimizations.biome_lookups")) {
            return OptimizationConfig.FAST_BIOME_LOOKUPS.get();
        }

        if (mixinClassName.startsWith(this.mixinPackage + "optimizations.ticking.chunk.cache")) {
            return OptimizationConfig.CACHE_TICKING_CHUNKS.get();
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private boolean isModLoaded(String... ids) {
        for (String id : ids) {
            if (this.isModLoaded(id)) {
                return true;
            }
        }
        return false;
    }
}