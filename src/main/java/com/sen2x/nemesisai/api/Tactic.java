package com.sen2x.nemesisai.api;

/**
 * Shared contract between the AI module (Arseniy) and the mob module (teammate 2).
 * The HUD/messages module (teammate 3) only ever renders these values.
 */
public enum Tactic {
	NORMAL("Normal"),
	FAST_CHASE("Fast Chase"),
	DELAYED_ATTACK("Delayed Attack"),
	ZIGZAG_APPROACH("Zigzag Approach"),
	RANGED_ATTACK("Ranged Attack");

	private final String displayName;

	Tactic(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
