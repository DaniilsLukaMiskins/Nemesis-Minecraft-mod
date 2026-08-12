package com.sen2x.nemesisai.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sen2x.nemesisai.api.LearningResult;
import com.sen2x.nemesisai.api.NemesisFeedback;
import com.sen2x.nemesisai.api.NemesisMemoryStore;
import com.sen2x.nemesisai.api.Tactic;
import dev.nemesis.entity.ModEntities;
import dev.nemesis.entity.NemesisEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Test/debug commands for teammate 3's HUD and integration work. {@code summon} places the
 * real Nemesis entity (teammate 2's mob module), {@code learn} simulates a learning event so
 * the HUD can be exercised without the real AI module (Arseniy) wired in, and
 * {@code resetmemory} clears the stub memory store.
 */
public final class NemesisCommands {
	private NemesisCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> learn = Commands.literal("learn");
			for (Tactic tactic : Tactic.values()) {
				learn.then(Commands.literal(tactic.name().toLowerCase())
						.executes(context -> learn(context.getSource(), tactic)));
			}

			dispatcher.register(Commands.literal("nemesis")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("summon").executes(context -> summon(context.getSource())))
					.then(Commands.literal("resetmemory").executes(context -> resetMemory(context.getSource())))
					.then(learn));
		});
	}

	private static int summon(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		BlockPos pos = player.blockPosition().offset(2, 0, 0);

		NemesisEntity nemesis = ModEntities.NEMESIS.spawn(level, pos, MobSpawnType.COMMAND);
		if (nemesis == null) {
			source.sendFailure(Component.literal("Could not spawn Nemesis."));
			return 0;
		}
		nemesis.setCustomName(Component.literal("Nemesis"));
		nemesis.setCustomNameVisible(true);

		source.sendSuccess(() -> Component.literal("Spawned Nemesis."), true);
		return 1;
	}

	private static int resetMemory(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		NemesisMemoryStore.reset(player.getUUID());
		source.sendSuccess(() -> Component.literal("Nemesis memory reset for " + player.getName().getString() + "."), true);
		return 1;
	}

	private static int learn(CommandSourceStack source, Tactic tactic) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		LearningResult result = new LearningResult(tactic, "Simulated via /nemesis learn", 0.5f);
		NemesisMemoryStore.record(player.getUUID(), result);
		NemesisFeedback.broadcastLearning(player, result);

		source.sendSuccess(() -> Component.literal("Simulated learning event: " + tactic.displayName()), true);
		return 1;
	}
}
