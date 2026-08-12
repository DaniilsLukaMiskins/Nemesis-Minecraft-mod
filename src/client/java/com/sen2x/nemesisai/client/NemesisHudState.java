package com.sen2x.nemesisai.client;

import com.sen2x.nemesisai.api.LearningResult;

/**
 * Client-side holder for the latest learning event, updated by the network receiver and
 * read by the HUD renderer. Kept separate from rendering code so it can also be driven
 * directly by client-side test tooling without a server round trip.
 */
public final class NemesisHudState {
	private static final int MESSAGE_DURATION_TICKS = 100;

	private static volatile LearningResult latestResult;
	private static volatile String latestMessage;
	private static volatile int messageTicksRemaining;

	private NemesisHudState() {
	}

	/**
	 * @return the banner text to display for this update, e.g. {@code "LEARNED: SHIELD DEFENSE"}
	 * on the first tactic seen this session or {@code "TACTIC CHANGED: DELAYED ATTACK"} when
	 * Nemesis switches away from its previous tactic.
	 */
	public static String update(LearningResult result) {
		String tacticName = result.tactic().displayName().toUpperCase();
		String message = latestResult == null
				? "LEARNED: " + tacticName
				: (latestResult.tactic() != result.tactic() ? "TACTIC CHANGED: " + tacticName : null);

		latestResult = result;
		if (message != null) {
			latestMessage = message;
			messageTicksRemaining = MESSAGE_DURATION_TICKS;
		}
		return message;
	}

	public static LearningResult latestResult() {
		return latestResult;
	}

	public static String latestMessage() {
		return latestMessage;
	}

	public static boolean hasFreshMessage() {
		return messageTicksRemaining > 0;
	}

	public static void tick() {
		if (messageTicksRemaining > 0) {
			messageTicksRemaining--;
		}
	}
}
