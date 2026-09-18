package com.aims.demo.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScenarioSimulatorTest {

    @Test
    void normalScenarioDoesNothing() {
        ScenarioState state = new ScenarioState();
        ScenarioSimulator simulator = new ScenarioSimulator(state);

        long start = System.currentTimeMillis();
        simulator.apply();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(500);
    }

    @Test
    void slowDbScenarioBlocksForFixedDelay() {
        ScenarioState state = new ScenarioState();
        state.set(Scenario.SLOW_DB);
        ScenarioSimulator simulator = new ScenarioSimulator(state);

        long start = System.currentTimeMillis();
        simulator.apply();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(2900);
    }

    @Test
    void exceptionScenarioThrowsSimulatedFailure() {
        ScenarioState state = new ScenarioState();
        state.set(Scenario.EXCEPTION);
        ScenarioSimulator simulator = new ScenarioSimulator(state);

        assertThatThrownBy(simulator::apply).isInstanceOf(SimulatedFailureException.class);
    }
}
