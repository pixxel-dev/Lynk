package com.example.lynk.core.domain.update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateManager {

    private String currentVersion = "1.0.0";
    private String repoPath = "pixxel-dev/Lynk";

    public AppUpdateManager() {
    }

    public AppUpdateManager(String currentVersion, String repoPath) {
        this.currentVersion = currentVersion;
        this.repoPath = repoPath;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public UpdateInfo checkForUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/" + repoPath + "/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "Lynk-Android-App");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            System.out.println("AppUpdateManager: Response code: " + responseCode + " for repo: " + repoPath);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();

                String json = content.toString();

                String tagName = extractJsonField(json, "tag_name");
                String body = extractJsonField(json, "body");
                if (body != null) {
                    body = body.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\\"", "\"");
                }
                
                String downloadUrl = extractBrowserDownloadUrl(json);

                if (tagName != null && isVersionNewer(tagName, currentVersion)) {
                    return new UpdateInfo(tagName, body, downloadUrl, UpdateInfo.UpdateState.UPDATE_AVAILABLE);
                } else {
                    return new UpdateInfo(currentVersion, "No changes", null, UpdateInfo.UpdateState.UP_TO_DATE);
                }
            } else {
                String errorMsg = "Error checking updates: HTTP " + responseCode;
                System.err.println("AppUpdateManager: " + errorMsg);
                return new UpdateInfo(currentVersion, errorMsg, null, UpdateInfo.UpdateState.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Exception checking updates: " + e.getMessage();
            System.err.println("AppUpdateManager: " + errorMsg);
            return new UpdateInfo(currentVersion, errorMsg, null, UpdateInfo.UpdateState.ERROR);
        }
    }

    private String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private String extractBrowserDownloadUrl(String json) {
        // 1. Search in assets array specifically
        int assetsIndex = json.indexOf("\"assets\"");
        if (assetsIndex != -1) {
            int arrayStart = json.indexOf("[", assetsIndex);
            int arrayEnd = json.indexOf("]", assetsIndex);
            if (arrayStart != -1 && arrayEnd != -1 && arrayEnd > arrayStart) {
                String assetsContent = json.substring(arrayStart, arrayEnd + 1);
                String[] objects = assetsContent.split("\\},\\s*\\{");
                for (String obj : objects) {
                    String name = extractJsonField(obj, "name");
                    String downloadUrl = extractJsonField(obj, "browser_download_url");
                    if (downloadUrl != null && (downloadUrl.toLowerCase().endsWith(".apk") || (name != null && name.toLowerCase().endsWith(".apk")))) {
                        System.out.println("AppUpdateManager: Found APK asset: name=" + name + ", url=" + downloadUrl);
                        return downloadUrl;
                    }
                }
            }
        }

        // 2. Fallback: Search all browser_download_url entries in JSON
        String pattern = "\"browser_download_url\":\"";
        int start = 0;
        while (true) {
            start = json.indexOf(pattern, start);
            if (start == -1) break;
            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) break;
            String url = json.substring(start, end);
            if (url.toLowerCase().endsWith(".apk")) {
                System.out.println("AppUpdateManager: Found APK URL via fallback: " + url);
                return url;
            }
            start = end;
        }

        System.err.println("AppUpdateManager: No valid .apk asset found in release JSON.");
        return null;
    }

    public boolean isVersionNewer(String latest, String current) {
        if (latest == null || current == null) return false;
        
        String cleanLatest = latest.toLowerCase().replace("v", "");
        String cleanCurrent = current.toLowerCase().replace("v", "");

        String[] latestParts = cleanLatest.split("\\.");
        String[] currentParts = cleanCurrent.split("\\.");

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
