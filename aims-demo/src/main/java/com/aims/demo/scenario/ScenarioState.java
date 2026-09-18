package com.aims.demo.scenario;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class ScenarioState {

    private final AtomicReference<Scenario> current = new AtomicReference<>(Scenario.NORMAL);

    public Scenario get() {
        return current.get();
    }

    public void set(Scenario scenario) {
        current.set(scenario);
    }
}
