package com.example.lynk.core.domain.update;

public class UpdateInfo {

    public enum UpdateState {
        CHECKING,
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        DOWNLOADING,
        DOWNLOADED,
        ERROR
    }

    private final String latestVersion;
    private final String changelog;
    private final String downloadUrl;
    private final UpdateState state;

    public UpdateInfo(String latestVersion, String changelog, String downloadUrl, UpdateState state) {
        this.latestVersion = latestVersion;
        this.changelog = changelog;
        this.downloadUrl = downloadUrl;
        this.state = state;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public UpdateState getState() {
        return state;
    }
}
