package com.aims.demo.scenario;

import org.springframework.stereotype.Component;

@Component
public class ScenarioSimulator {

    // No real database is involved; SLOW_DB only simulates a slow downstream
    // dependency by blocking the request thread for a fixed duration.
    private static final long SLOW_DB_SIMULATED_DELAY_MS = 3000L;

    private final ScenarioState scenarioState;

    public ScenarioSimulator(ScenarioState scenarioState) {
        this.scenarioState = scenarioState;
    }

    public void apply() {
        Scenario scenario = scenarioState.get();
        switch (scenario) {
            case SLOW_DB -> simulateDelay();
            case EXCEPTION -> throwSimulatedFailure();
            case NORMAL -> {
            }
        }
    }

    private void simulateDelay() {
        try {
            Thread.sleep(SLOW_DB_SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SimulatedFailureException("Simulated delay interrupted");
        }
    }

    private void throwSimulatedFailure() {
        throw new SimulatedFailureException("Simulated downstream exception");
    }
}
