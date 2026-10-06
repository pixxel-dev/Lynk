package com.example.lynk.core.domain.floating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FloatingButtonAction {

    public enum ActionType {
        QUICK_LAUNCH("Quick Launch", "Quick app launcher menu"),
        FULLSCREEN_TOGGLE("Fullscreen Toggle", "Toggle secondary display / fullscreen mode"),
        HOME("Навигатор \"Домой\"", "Navigate to home screen"),
        BACK("Навигатор \"Назад\"", "Trigger back action"),
        REFRESH("Обновить / Refresh", "Refresh interface or trigger update"),
        CUSTOM("Custom Action", "Custom action button");

        private final String displayName;
        private final String description;

        ActionType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    private String id;
    private String label;
    private ActionType actionType;
    private List<String> targetPackages;
    private boolean enabled;

    public FloatingButtonAction(String id, String label, ActionType actionType, List<String> targetPackages, boolean enabled) {
        this.id = id;
        this.label = label;
        this.actionType = actionType;
        this.targetPackages = targetPackages != null ? new ArrayList<>(targetPackages) : new ArrayList<>();
        this.enabled = enabled;
    }

    public FloatingButtonAction(String id, String label, ActionType actionType) {
        this(id, label, actionType, new ArrayList<>(), true);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public List<String> getTargetPackages() {
        return Collections.unmodifiableList(targetPackages);
    }

    public void setTargetPackages(List<String> targetPackages) {
        this.targetPackages = targetPackages != null ? new ArrayList<>(targetPackages) : new ArrayList<>();
    }

    public void addTargetPackage(String packageName) {
        if (packageName != null && !packageName.trim().isEmpty() && !this.targetPackages.contains(packageName)) {
            this.targetPackages.add(packageName);
        }
    }

    public void removeTargetPackage(String packageName) {
        this.targetPackages.remove(packageName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloatingButtonAction that = (FloatingButtonAction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
