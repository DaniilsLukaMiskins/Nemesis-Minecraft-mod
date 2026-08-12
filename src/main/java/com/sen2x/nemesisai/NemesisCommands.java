package com.sen2x.nemesisai;

import com.sen2x.nemesisai.entity.NemesisEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Optional;

public final class NemesisCommands {
    private NemesisCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("nemesis")
                                        .then(
                                                Commands.literal("memory")
                                                        .executes(context ->
                                                                showMemory(
                                                                        context.getSource()
                                                                                .getPlayerOrException()
                                                                )
                                                        )
                                        )
                        )
        );
    }

    private static int showMemory(ServerPlayer player) {
        Optional<NemesisEntity> nearest = player.level()
                .getEntitiesOfClass(
                        NemesisEntity.class,
                        player.getBoundingBox().inflate(32.0)
                )
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr));

        if (nearest.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("No Nemesis found within 32 blocks.")
            );
            return 0;
        }

        NemesisEntity nemesis = nearest.get();

        String memory = "NEMESIS MEMORY | Melee: "
                + nemesis.getMeleeHits()
                + " | Ranged: "
                + nemesis.getRangedHits()
                + " | Melee resistance: "
                + nemesis.hasLearnedMelee()
                + " | Ranged resistance: "
                + nemesis.hasLearnedRanged();

        player.sendSystemMessage(Component.literal(memory));
        return 1;
    }
}