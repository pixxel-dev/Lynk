package com.example.lynk.core.domain.update;

import java.util.Collections;
import java.util.List;

public class UpdateInfo {
    private final String currentVersion;
    private final String latestVersion;
    private final int versionCode;
    private final String updateUrl;
    private final List<String> changelog;
    private final boolean hasUpdate;
    private final String releaseDate;
    private final long fileSize;

    public UpdateInfo(String currentVersion, String latestVersion, int versionCode,
                      String updateUrl, List<String> changelog, boolean hasUpdate,
                      String releaseDate, long fileSize) {
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.versionCode = versionCode;
        this.updateUrl = updateUrl;
        this.changelog = changelog != null ? changelog : Collections.emptyList();
        this.hasUpdate = hasUpdate;
        this.releaseDate = releaseDate;
        this.fileSize = fileSize;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public List<String> getChangelog() {
        return changelog;
    }

    public boolean isHasUpdate() {
        return hasUpdate;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public long getFileSize() {
        return fileSize;
    }
}
