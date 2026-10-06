package com.example.lynk.core.domain.audit.rules;

import com.example.lynk.core.domain.audit.AuditRule;
import com.example.lynk.core.domain.audit.AuditResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuplicatePatternRule implements AuditRule<List<ArchitecturalRule>> {

    @Override
    public String getName() {
        return "Duplicate Pattern Rule";
    }

    @Override
    public AuditResult evaluate(List<ArchitecturalRule> target) {
        Set<String> seenPatterns = new HashSet<>();
        for (ArchitecturalRule rule : target) {
            if (!seenPatterns.add(rule.getPattern())) {
                return new AuditResult(false, "Duplicate pattern found: " + rule.getPattern(), getName());
            }
        }
        return new AuditResult(true, "No duplicate patterns found.", getName());
    }
}
