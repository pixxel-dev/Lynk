package com.example.lynk.core.domain.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for executing a set of {@link AuditRule}s against a target.
 *
 * @param <T> The type of object to be audited.
 */
public class RuleAuditor<T> {
    private final List<AuditRule<T>> rules;

    /**
     * Constructs a new RuleAuditor.
     */
    public RuleAuditor() {
        this.rules = new ArrayList<>();
    }

    /**
     * Adds a rule to this auditor.
     *
     * @param rule The {@link AuditRule} to add.
     */
    public void addRule(AuditRule<T> rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    /**
     * Executes all registered rules against the given target.
     *
     * @param target The target object to audit.
     * @return A list of {@link AuditResult}s containing the outcomes of each rule.
     */
    public List<AuditResult> audit(T target) {
        List<AuditResult> results = new ArrayList<>();
        for (AuditRule<T> rule : rules) {
            results.add(rule.evaluate(target));
        }
        return results;
    }
}
