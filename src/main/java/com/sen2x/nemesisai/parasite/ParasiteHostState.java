package com.sen2x.nemesisai.parasite;

public interface ParasiteHostState {
    boolean nemesisAi$isInfected();
    int nemesisAi$getIncubationTicks();
    void nemesisAi$infect(int ticks);
    void nemesisAi$setIncubationTicks(int ticks);
}
