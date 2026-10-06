package com.example.lynk.core.domain.agent;

/**
 * Represents the status of an {@link AgentTask}.
 */
public enum TaskStatus {
    /**
     * Task is waiting to be processed.
     */
    PENDING,

    /**
     * Task is currently being processed by an agent.
     */
    IN_PROGRESS,

    /**
     * Task has been successfully completed.
     */
    COMPLETED,

    /**
     * Task execution failed.
     */
    FAILED,

    /**
     * Task has been cancelled.
     */
    CANCELLED
}
