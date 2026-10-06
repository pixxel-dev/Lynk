package com.example.lynk.core.domain.audit;

/**
 * Represents the outcome of evaluating an {@link AuditRule}.
 */
public class AuditResult {
    private final boolean passed;
    private final String message;
    private final String ruleName;

    /**
     * Constructs a new AuditResult.
     *
     * @param passed true if the audit rule passed, false if it failed.
     * @param message A message detailing why the rule failed, or a success confirmation.
     * @param ruleName The name of the rule that was evaluated.
     */
    public AuditResult(boolean passed, String message, String ruleName) {
        this.passed = passed;
        this.message = message;
        this.ruleName = ruleName;
    }

    /**
     * Checks if the audit passed.
     *
     * @return true if passed, false otherwise.
     */
    public boolean isPassed() {
        return passed;
    }

    /**
     * Retrieves the audit message.
     *
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Retrieves the name of the rule associated with this result.
     *
     * @return The rule name.
     */
    public String getRuleName() {
        return ruleName;
    }
}
