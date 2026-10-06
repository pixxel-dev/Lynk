package com.example.lynk.core.domain.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an agent in the system capable of executing tasks.
 */
public class Agent {
    private final String id;
    private final String name;
    private final List<AgentTask> assignedTasks;

    /**
     * Constructs a new Agent.
     *
     * @param id The unique identifier for the agent.
     * @param name The name of the agent.
     */
    public Agent(String id, String name) {
        this.id = id;
        this.name = name;
        this.assignedTasks = new ArrayList<>();
    }

    /**
     * Retrieves the agent's unique identifier.
     *
     * @return The agent ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the agent's name.
     *
     * @return The agent name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the list of tasks assigned to this agent.
     *
     * @return A list of {@link AgentTask} objects.
     */
    public List<AgentTask> getAssignedTasks() {
        return new ArrayList<>(assignedTasks);
    }

    /**
     * Assigns a new task to the agent.
     *
     * @param task The {@link AgentTask} to assign.
     */
    public void assignTask(AgentTask task) {
        if (task != null) {
            task.setAssignedAgentId(this.id);
            this.assignedTasks.add(task);
        }
    }

    /**
     * Completes a specific task assigned to this agent.
     *
     * @param taskId The ID of the task to complete.
     */
    public void completeTask(String taskId) {
        for (AgentTask task : assignedTasks) {
            if (task.getId().equals(taskId)) {
                task.setStatus(TaskStatus.COMPLETED);
                break;
            }
        }
    }
}
