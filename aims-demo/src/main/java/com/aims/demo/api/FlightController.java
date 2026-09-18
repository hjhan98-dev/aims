package com.aims.demo.api;

import com.aims.demo.api.dto.FlightResponse;
import com.aims.demo.scenario.ScenarioSimulator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlightController {

    private final ScenarioSimulator scenarioSimulator;

    public FlightController(ScenarioSimulator scenarioSimulator) {
        this.scenarioSimulator = scenarioSimulator;
    }

    @GetMapping("/api/flights/{flightNumber}")
    public FlightResponse getFlight(@PathVariable String flightNumber) {
        scenarioSimulator.apply();
        return new FlightResponse(flightNumber, "ICN", "NRT", "SCHEDULED");
    }
}
