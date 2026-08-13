package com.sen2x.nemesisai.api;

import dev.nemesis.entity.Tactic;

/**
 * One learning event: the tactic Nemesis picked, why, and its overall adaptation level.
 *
 * @param tactic          the tactic Nemesis switched to
 * @param reason          short human-readable trigger, e.g. "Player blocked 5 hits with shield"
 * @param adaptationLevel overall learning progress in the 0.0-1.0 range, shown on the HUD bar
 */
public record LearningResult(Tactic tactic, String reason, float adaptationLevel) {
	public LearningResult {
		adaptationLevel = Math.clamp(adaptationLevel, 0.0f, 1.0f);
	}
}
