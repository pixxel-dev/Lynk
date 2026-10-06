package com.example.lynk.core.domain.audit.rules;

import com.example.lynk.core.domain.audit.AuditResult;
import com.example.lynk.core.domain.audit.RuleAuditor;

import java.util.ArrayList;
import java.util.List;

public class AuditorRunner {

    public static void main(String[] args) {
        System.out.println("Starting Architectural Rules Audit...");

        List<ArchitecturalRule> rules = new ArrayList<>();
        rules.add(new ArchitecturalRule("**/*.java", "All Java files"));
        rules.add(new ArchitecturalRule("**/*.kt", "All Kotlin files"));
        rules.add(new ArchitecturalRule("**/*.xml", "All XML files"));
        // Simulating a duplicate
        rules.add(new ArchitecturalRule("**/*.kt", "All Kotlin files (duplicate)"));

        RuleAuditor<List<ArchitecturalRule>> auditor = new RuleAuditor<>();
        auditor.addRule(new DuplicatePatternRule());

        List<AuditResult> results = auditor.audit(rules);

        boolean allPassed = true;
        for (AuditResult result : results) {
            System.out.println("Rule: " + result.getRuleName());
            System.out.println("Passed: " + result.isPassed());
            System.out.println("Message: " + result.getMessage());
            System.out.println("--------------------------------------------------");
            if (!result.isPassed()) {
                allPassed = false;
            }
        }

        if (allPassed) {
            System.out.println("Audit finished successfully.");
            System.exit(0);
        } else {
            System.err.println("Audit failed.");
            System.exit(1);
        }
    }
}
