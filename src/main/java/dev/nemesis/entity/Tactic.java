package dev.nemesis.entity;

/**
 * Single shared tactic enum: drives {@link NemesisEntity}'s combat goals AND is what the
 * HUD/learning pipeline (com.sen2x.nemesisai.api) reports to the player.
 */
public enum Tactic {
	NORMAL("Normal Assault"),
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
