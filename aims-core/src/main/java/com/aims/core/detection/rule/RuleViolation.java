package com.aims.core.detection.rule;

public record RuleViolation(String signalType, double signalValue, double threshold, String description) {
}
