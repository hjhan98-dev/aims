package com.aims.demo.scenario;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminScenarioController {

    private final ScenarioState scenarioState;

    public AdminScenarioController(ScenarioState scenarioState) {
        this.scenarioState = scenarioState;
    }

    @PostMapping("/admin/scenario/normal")
    public Map<String, String> normal() {
        scenarioState.set(Scenario.NORMAL);
        return Map.of("scenario", Scenario.NORMAL.name());
    }

    @PostMapping("/admin/scenario/slow-db")
    public Map<String, String> slowDb() {
        scenarioState.set(Scenario.SLOW_DB);
        return Map.of("scenario", Scenario.SLOW_DB.name());
    }

    @PostMapping("/admin/scenario/exception")
    public Map<String, String> exception() {
        scenarioState.set(Scenario.EXCEPTION);
        return Map.of("scenario", Scenario.EXCEPTION.name());
    }
}
