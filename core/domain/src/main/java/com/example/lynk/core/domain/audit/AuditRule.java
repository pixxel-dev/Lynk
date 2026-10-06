package com.example.lynk.core.domain.audit;

/**
 * Represents a rule that can be evaluated during an audit.
 * 
 * @param <T> The type of object this rule evaluates.
 */
public interface AuditRule<T> {
    
    /**
     * Retrieves the name of the rule.
     *
     * @return The rule name.
     */
    String getName();

    /**
     * Evaluates the rule against the given target.
     *
     * @param target The object to evaluate.
     * @return An {@link AuditResult} detailing the outcome of the evaluation.
     */
    AuditResult evaluate(T target);
}
