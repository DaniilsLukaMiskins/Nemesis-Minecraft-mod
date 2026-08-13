package com.sen2x.nemesisai.network;

import com.sen2x.nemesisai.NemesisAiMod;
import com.sen2x.nemesisai.api.LearningResult;
import dev.nemesis.entity.Tactic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from server to client whenever Nemesis learns something, so the HUD module can
 * render an adaptation update without depending on the AI module directly.
 */
public record LearningResultPayload(Tactic tactic, String reason, float adaptationLevel) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LearningResultPayload> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NemesisAiMod.MOD_ID, "learning_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LearningResultPayload> STREAM_CODEC = StreamCodec.composite(
			StreamCodec.of(
					(buf, tactic) -> buf.writeEnum(tactic),
					buf -> buf.readEnum(Tactic.class)
			),
			LearningResultPayload::tactic,
			StreamCodec.of(
					(buf, value) -> buf.writeUtf(value, 256),
					buf -> buf.readUtf(256)
			),
			LearningResultPayload::reason,
			StreamCodec.of(
					(buf, value) -> buf.writeFloat(value),
					RegistryFriendlyByteBuf::readFloat
			),
			LearningResultPayload::adaptationLevel,
			LearningResultPayload::new
	);

	public LearningResultPayload(LearningResult result) {
		this(result.tactic(), result.reason(), result.adaptationLevel());
	}

	public LearningResult toLearningResult() {
		return new LearningResult(tactic, reason, adaptationLevel);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
