package org.provim.servercore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import org.provim.servercore.config.tables.CommandConfig;
import org.provim.servercore.utils.TickManager;

import static net.minecraft.commands.Commands.literal;

public final class InfoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (CommandConfig.COMMAND_MOBCAPS.get() && !FabricLoader.getInstance().isModLoaded("vmp")) {
            dispatcher.register(literal("mobcaps").executes(InfoCommand::mobcaps));
        }

        if (CommandConfig.COMMAND_STATUS.get()) {
            dispatcher.register(literal("servercore").then(literal("status").executes(InfoCommand::status)));
        }
    }

    private static int mobcaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        TextComponent text = new TextComponent(CommandConfig.MOBCAP_TITLE.get().replace("%MODIFIER%", TickManager.getModifierAsString()));
        NaturalSpawner.SpawnState state = player.getLevel().getChunkSource().getLastSpawnState();

        if (state != null) {
            LocalMobCapCalculator.MobCounts mobCounts = state.localMobCapCalculator.playerMobCounts.computeIfAbsent(player, p -> new LocalMobCapCalculator.MobCounts());
            for (MobCategory category : MobCategory.values()) {
                text.append("\n").append(new TextComponent(CommandConfig.MOBCAP_CONTENT.get()
                        .replace("%NAME%", category.getName())
                        .replace("%CURRENT%", String.valueOf(mobCounts.counts.getOrDefault(category, 0)))
                        .replace("%CAPACITY%", String.valueOf(category.getMaxInstancesPerChunk()))
                ));
            }
        }

        player.displayClientMessage(text, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(TickManager.createStatusReport(), false);
        return Command.SINGLE_SUCCESS;
    }
}
