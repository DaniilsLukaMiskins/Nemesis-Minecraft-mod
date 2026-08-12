package dev.nemesis.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.nemesis.entity.NemesisEntity;
import dev.nemesis.entity.Tactic;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

import static net.minecraft.server.command.CommandManager.literal;

/** Temporary manual controls for exercising tactics without a learning engine. */
public final class NemesisCommands {
    private static final double SEARCH_RADIUS = 64.0;

    private NemesisCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("nemesis")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("tactic")
                        .then(tactic("normal", Tactic.NORMAL))
                        .then(tactic("fast_chase", Tactic.FAST_CHASE))
                        .then(tactic("delayed_attack", Tactic.DELAYED_ATTACK))
                        .then(tactic("zigzag_approach", Tactic.ZIGZAG_APPROACH))
                        .then(tactic("ranged_attack", Tactic.RANGED_ATTACK))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> tactic(
            String name, Tactic tactic) {
        return literal(name).executes(context -> setNearest(context.getSource(), tactic));
    }

    private static int setNearest(ServerCommandSource source, Tactic tactic) {
        Vec3d origin = source.getPosition();
        NemesisEntity nearest = source.getWorld()
                .getEntitiesByClass(NemesisEntity.class, Box.of(origin, SEARCH_RADIUS * 2.0,
                        SEARCH_RADIUS * 2.0, SEARCH_RADIUS * 2.0), entity -> entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(origin)))
                .orElse(null);

        if (nearest == null) {
            source.sendError(Text.literal("No Nemesis found within 64 blocks."));
            return 0;
        }

        nearest.setTactic(tactic);
        source.sendFeedback(() -> Text.literal("Nearest Nemesis tactic set to "
                + tactic.name().toLowerCase(java.util.Locale.ROOT) + "."), false);
        return 1;
    }
}
