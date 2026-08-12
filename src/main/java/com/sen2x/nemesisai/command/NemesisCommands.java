package com.sen2x.nemesisai.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sen2x.nemesisai.api.LearningResult;
import com.sen2x.nemesisai.api.NemesisFeedback;
import com.sen2x.nemesisai.api.NemesisMemoryStore;
import com.sen2x.nemesisai.api.Tactic;
import com.sen2x.nemesisai.ModEntities;
import com.sen2x.nemesisai.entity.NemesisEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import java.util.Comparator;

/**
 * Integrated test/debug controls for the real animated Nemesis, its persistent memory,
 * selectable combat tactics, and HUD learning notifications.
 */
public final class NemesisCommands {
	private NemesisCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> learn = Commands.literal("learn");
			LiteralArgumentBuilder<CommandSourceStack> tactics = Commands.literal("tactic");
			for (Tactic tactic : Tactic.values()) {
				learn.then(Commands.literal(tactic.name().toLowerCase())
						.executes(context -> learn(context.getSource(), tactic)));
				tactics.then(Commands.literal(tactic.name().toLowerCase())
						.executes(context -> setTactic(context.getSource(), tactic)));
			}

			dispatcher.register(Commands.literal("nemesis")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("summon").executes(context -> summon(context.getSource())))
					.then(Commands.literal("memory").executes(context -> memory(context.getSource())))
					.then(Commands.literal("resetmemory").executes(context -> resetMemory(context.getSource())))
					.then(tactics)
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
		source.sendSuccess(() -> Component.literal("Spawned the animated Nemesis."), true);
		return 1;
	}

	private static int memory(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		NemesisEntity nemesis = nearest(player);
		if (nemesis == null) {
			source.sendFailure(Component.literal("No Nemesis found within 32 blocks."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("NEMESIS MEMORY | Melee: " + nemesis.getMeleeHits()
				+ " | Ranged: " + nemesis.getRangedHits() + " | Melee resistance: "
				+ nemesis.hasLearnedMelee() + " | Ranged resistance: " + nemesis.hasLearnedRanged()), false);
		return 1;
	}

	private static int setTactic(CommandSourceStack source, Tactic tactic) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		NemesisEntity nemesis = nearest(player);
		if (nemesis == null) {
			source.sendFailure(Component.literal("No Nemesis found within 32 blocks."));
			return 0;
		}
		nemesis.setTactic(tactic);
		source.sendSuccess(() -> Component.literal("Nemesis tactic: " + tactic.displayName()), false);
		return 1;
	}

	private static NemesisEntity nearest(ServerPlayer player) {
		return player.level().getEntitiesOfClass(NemesisEntity.class,
				player.getBoundingBox().inflate(32.0)).stream()
				.min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
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
