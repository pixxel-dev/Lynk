package com.example.lynk.core.domain.backlog;

/**
 * Represents an item in the system backlog.
 */
public class BacklogItem {
    private final String id;
    private final String title;
    private final String description;
    private int priority;
    private boolean completed;

    /**
     * Constructs a new BacklogItem.
     *
     * @param id The unique identifier for the backlog item.
     * @param title The title or short name of the item.
     * @param description Detailed description of the item.
     * @param priority The priority of the item (higher number = higher priority).
     */
    public BacklogItem(String id, String title, String description, int priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.completed = false;
    }

    /**
     * Retrieves the item's unique identifier.
     *
     * @return The item ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the title of the item.
     *
     * @return The item title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Retrieves the description of the item.
     *
     * @return The item description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retrieves the priority of the item.
     *
     * @return The item priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Updates the priority of the item.
     *
     * @param priority The new priority to set.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Checks if the item is completed.
     *
     * @return true if completed, false otherwise.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Marks the item as completed.
     */
    public void markCompleted() {
        this.completed = true;
    }
}
