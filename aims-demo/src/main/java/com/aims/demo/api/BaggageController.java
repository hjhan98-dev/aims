package com.aims.demo.api;

import com.aims.demo.api.dto.BaggageRequest;
import com.aims.demo.api.dto.BaggageResponse;
import com.aims.demo.scenario.ScenarioSimulator;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaggageController {

    private final ScenarioSimulator scenarioSimulator;

    public BaggageController(ScenarioSimulator scenarioSimulator) {
        this.scenarioSimulator = scenarioSimulator;
    }

    @PostMapping("/api/baggage")
    public BaggageResponse checkBaggage(@RequestBody BaggageRequest request) {
        scenarioSimulator.apply();
        return new BaggageResponse(UUID.randomUUID().toString(), request.flightNumber(), "TAGGED");
    }
}
