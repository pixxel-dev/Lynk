package com.example.lynk.core.domain.audit.rules;

public class ArchitecturalRule {
    private final String pattern;
    private final String description;

    public ArchitecturalRule(String pattern, String description) {
        this.pattern = pattern;
        this.description = description;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDescription() {
        return description;
    }
}
