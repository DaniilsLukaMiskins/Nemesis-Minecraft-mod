package com.sen2x.nemesisai.api;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Placeholder in-memory store for per-player learning state, exposed so the
 * {@code /nemesis resetmemory} command has something real to clear during testing.
 * The AI module is expected to replace/extend this with persistent storage.
 */
public final class NemesisMemoryStore {
	private static final ConcurrentHashMap<UUID, AtomicReference<LearningResult>> LAST_RESULTS = new ConcurrentHashMap<>();

	private NemesisMemoryStore() {
	}

	public static void record(UUID playerId, LearningResult result) {
		LAST_RESULTS.computeIfAbsent(playerId, id -> new AtomicReference<>()).set(result);
	}

	public static LearningResult lastResult(UUID playerId) {
		AtomicReference<LearningResult> ref = LAST_RESULTS.get(playerId);
		return ref == null ? null : ref.get();
	}

	public static void reset(UUID playerId) {
		LAST_RESULTS.remove(playerId);
	}

	public static void resetAll() {
		LAST_RESULTS.clear();
	}
}
