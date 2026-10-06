package com.example.lynk.core.domain.installer;

public class InstallResult {
    private final boolean success;
    private final String message;
    private final InstallStep step;

    public InstallResult(boolean success, String message, InstallStep step) {
        this.success = success;
        this.message = message;
        this.step = step;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public InstallStep getStep() {
        return step;
    }
}
