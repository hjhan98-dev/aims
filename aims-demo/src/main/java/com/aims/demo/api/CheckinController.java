package com.aims.demo.api;

import com.aims.demo.api.dto.CheckinRequest;
import com.aims.demo.api.dto.CheckinResponse;
import com.aims.demo.scenario.ScenarioSimulator;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckinController {

    private final ScenarioSimulator scenarioSimulator;

    public CheckinController(ScenarioSimulator scenarioSimulator) {
        this.scenarioSimulator = scenarioSimulator;
    }

    @PostMapping("/api/passengers/checkin")
    public CheckinResponse checkin(@RequestBody CheckinRequest request) {
        scenarioSimulator.apply();
        return new CheckinResponse(UUID.randomUUID().toString(), request.flightNumber(), "CONFIRMED");
    }
}
