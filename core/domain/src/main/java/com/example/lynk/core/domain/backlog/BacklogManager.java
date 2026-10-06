package com.example.lynk.core.domain.backlog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages a collection of {@link BacklogItem}s.
 */
public class BacklogManager {
    private final List<BacklogItem> items;

    /**
     * Constructs a new BacklogManager.
     */
    public BacklogManager() {
        this.items = new ArrayList<>();
    }

    /**
     * Adds a new item to the backlog.
     *
     * @param item The {@link BacklogItem} to add.
     */
    public void addItem(BacklogItem item) {
        if (item != null) {
            items.add(item);
        }
    }

    /**
     * Removes an item from the backlog by its ID.
     *
     * @param itemId The ID of the item to remove.
     * @return true if the item was removed, false otherwise.
     */
    public boolean removeItem(String itemId) {
        return items.removeIf(item -> item.getId().equals(itemId));
    }

    /**
     * Retrieves all items in the backlog, sorted by priority in descending order.
     *
     * @return A sorted list of {@link BacklogItem}s.
     */
    public List<BacklogItem> getItemsSortedByPriority() {
        return items.stream()
                .sorted(Comparator.comparingInt(BacklogItem::getPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all uncompleted items from the backlog.
     *
     * @return A list of pending {@link BacklogItem}s.
     */
    public List<BacklogItem> getPendingItems() {
        return items.stream()
                .filter(item -> !item.isCompleted())
                .collect(Collectors.toList());
    }

    /**
     * Marks a specific item as completed.
     *
     * @param itemId The ID of the item to complete.
     */
    public void completeItem(String itemId) {
        items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(BacklogItem::markCompleted);
    }
}
