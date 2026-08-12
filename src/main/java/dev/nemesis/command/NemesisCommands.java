package dev.nemesis.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nemesis.entity.NemesisEntity;
import dev.nemesis.entity.Tactic;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Locale;

public final class NemesisCommands {
    private static final double SEARCH_RADIUS = 64.0;

    private NemesisCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nemesis")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("tactic")
                        .then(tactic("normal", Tactic.NORMAL))
                        .then(tactic("fast_chase", Tactic.FAST_CHASE))
                        .then(tactic("delayed_attack", Tactic.DELAYED_ATTACK))
                        .then(tactic("zigzag_approach", Tactic.ZIGZAG_APPROACH))
                        .then(tactic("ranged_attack", Tactic.RANGED_ATTACK))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tactic(String name, Tactic tactic) {
        return Commands.literal(name).executes(context -> setNearest(context.getSource(), tactic));
    }

    private static int setNearest(CommandSourceStack source, Tactic tactic) {
        Vec3 origin = source.getPosition();
        NemesisEntity nearest = source.getLevel()
                .getEntitiesOfClass(NemesisEntity.class,
                        AABB.ofSize(origin, SEARCH_RADIUS * 2.0, SEARCH_RADIUS * 2.0,
                                SEARCH_RADIUS * 2.0),
                        entity -> entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)))
                .orElse(null);

        if (nearest == null) {
            source.sendFailure(Component.literal("No Nemesis found within 64 blocks."));
            return 0;
        }

        nearest.setTactic(tactic);
        source.sendSuccess(() -> Component.literal("Nearest Nemesis tactic set to "
                + tactic.name().toLowerCase(Locale.ROOT) + "."), false);
        return 1;
    }
}
