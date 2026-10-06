package com.example.lynk.core.domain.update;

import java.util.Arrays;
import java.util.List;

public class AppUpdateManager {

    public enum UpdateStatus {
        IDLE,
        CHECKING,
        UPDATE_AVAILABLE,
        NO_UPDATE,
        DOWNLOADING,
        READY_TO_INSTALL,
        ERROR
    }

    private String currentVersion = "1.0.0";

    public AppUpdateManager() {
    }

    public AppUpdateManager(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public UpdateInfo checkForUpdates() {
        String latestVersion = "1.1.0";
        int versionCode = 101;
        String updateUrl = "https://github.com/lynk/releases/download/v1.1.0/lynk-v1.1.0.apk";
        String releaseDate = "2025-02-28";
        long fileSize = 15_400_000L;

        List<String> changelog = Arrays.asList(
            "• Переключение на темную и светлую тему (ThemeMode: SYSTEM, LIGHT, DARK)",
            "• Новый экран состояния прав доступа (Permissions Info) с быстрым переходом в настройки",
            "• Раздел обновления приложения (App Update) с чекером версий и журналом изменений",
            "• Адаптация интерфейса под альбомный режим (Landscape) и устранение обрезки текста"
        );

        boolean hasUpdate = isVersionNewer(latestVersion, currentVersion);

        return new UpdateInfo(
            currentVersion,
            latestVersion,
            versionCode,
            updateUrl,
            changelog,
            hasUpdate,
            releaseDate,
            fileSize
        );
    }

    public boolean isVersionNewer(String latest, String current) {
        if (latest == null || current == null) return false;
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int v2 = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            if (v1 > v2) return true;
            if (v1 < v2) return false;
        }
        return false;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
