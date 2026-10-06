package com.example.lynk.core.domain.agent;

/**
 * Represents a task assigned to an {@link Agent}.
 */
public class AgentTask {
    private final String id;
    private final String description;
    private TaskStatus status;
    private String assignedAgentId;

    /**
     * Constructs a new AgentTask.
     *
     * @param id The unique identifier for the task.
     * @param description The description of the task.
     */
    public AgentTask(String id, String description) {
        this.id = id;
        this.description = description;
        this.status = TaskStatus.PENDING;
    }

    /**
     * Retrieves the task's unique identifier.
     *
     * @return The task ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the task's description.
     *
     * @return The description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retrieves the current status of the task.
     *
     * @return The task status.
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Updates the status of the task.
     *
     * @param status The new status to set.
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Retrieves the ID of the agent assigned to this task.
     *
     * @return The assigned agent's ID, or null if unassigned.
     */
    public String getAssignedAgentId() {
        return assignedAgentId;
    }

    /**
     * Assigns this task to an agent.
     *
     * @param assignedAgentId The ID of the agent to assign.
     */
    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }
}
