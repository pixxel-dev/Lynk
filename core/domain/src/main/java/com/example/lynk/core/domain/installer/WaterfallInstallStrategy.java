package com.example.lynk.core.domain.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaterfallInstallStrategy {

    public static final int USER_ALL_SCREENS = -1;

    public enum InstallStage {
        PREPARING_PINE_AND_USER,
        TRYING_SHIZUKU_STREAM,
        TRYING_ADB_STREAM,
        FALLBACK_SYSTEM_INSTALLER_AS_USER,
        SUCCESS_SILENT,
        SUCCESS_SYSTEM_DIALOG,
        ERROR
    }

    public interface InstallProgressListener {
        void onStageChanged(InstallStage stage, String details);
    }

    public interface PrivilegedBridge {
        void ensurePineAndHiddenApiInitialized();
        boolean isShizukuReady();
        Process startShizukuProcess(String command) throws Exception;
        Process startLocalAdbOrShellProcess(String command) throws Exception;
        int resolveActiveDriverUserId();
        boolean launchSystemInstallerAsUser(File apkFile, int userId, int displayId);
    }

    private static final Pattern USER_PATTERN =
            Pattern.compile("UserInfo\\{(\\d+):([^:]+):.*\\}");

    private final PrivilegedBridge bridge;

    public WaterfallInstallStrategy(PrivilegedBridge bridge) {
        this.bridge = bridge;
    }

    public Map<Integer, String> getAvailableCarUsers() {
        Map<Integer, String> users = new LinkedHashMap<>();
        Process process = null;
        try {
            process = bridge.isShizukuReady()
                    ? bridge.startShizukuProcess("pm list users")
                    : bridge.startLocalAdbOrShellProcess("pm list users");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = USER_PATTERN.matcher(line);
                    if (matcher.find()) {
                        int id = Integer.parseInt(matcher.group(1));
                        String name = matcher.group(2);
                        users.put(id, name);
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        if (users.isEmpty()) {
            int driverId = bridge.resolveActiveDriverUserId();
            users.put(driverId, "Driver (User " + driverId + ")");
        }
        return users;
    }

    public boolean install(File apkFile, Integer explicitUserId, InstallProgressListener listener) {
        if (apkFile == null || !apkFile.exists()) {
            notify(listener, InstallStage.ERROR, "APK file not found");
            return false;
        }

        bridge.ensurePineAndHiddenApiInitialized();
        int targetUser = (explicitUserId != null) ? explicitUserId : bridge.resolveActiveDriverUserId();
        String userArg = (targetUser == USER_ALL_SCREENS) ? "all" : String.valueOf(targetUser);

        notify(listener, InstallStage.PREPARING_PINE_AND_USER, userArg);

        if (bridge.isShizukuReady()) {
            notify(listener, InstallStage.TRYING_SHIZUKU_STREAM, userArg);
            if (streamInstallInternal(apkFile, userArg, true)) {
                notify(listener, InstallStage.SUCCESS_SILENT, "Shizuku (User " + userArg + ")");
                return true;
            }
        }

        notify(listener, InstallStage.TRYING_ADB_STREAM, userArg);
        if (streamInstallInternal(apkFile, userArg, false)) {
            notify(listener, InstallStage.SUCCESS_SILENT, "ADB/Shell (User " + userArg + ")");
            return true;
        }

        int fallbackUser = (targetUser == USER_ALL_SCREENS) ? bridge.resolveActiveDriverUserId() : targetUser;
        notify(listener, InstallStage.FALLBACK_SYSTEM_INSTALLER_AS_USER, String.valueOf(fallbackUser));
        boolean launched = bridge.launchSystemInstallerAsUser(apkFile, fallbackUser, 0);
        if (launched) {
            notify(listener, InstallStage.SUCCESS_SYSTEM_DIALOG, "User " + fallbackUser);
        } else {
            notify(listener, InstallStage.ERROR, "All installation strategies failed");
        }
        return launched;
    }

    private boolean streamInstallInternal(File apkFile, String userArg, boolean useShizuku) {
        Process process = null;
        try {
            long sizeBytes = apkFile.length();
            String cmd = "pm install -r -g --user " + userArg + " -S " + sizeBytes;
            process = useShizuku
                    ? bridge.startShizukuProcess(cmd)
                    : bridge.startLocalAdbOrShellProcess(cmd);

            try (InputStream in = new FileInputStream(apkFile);
                 OutputStream out = process.getOutputStream()) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0 && output.toString().contains("Success");
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void notify(InstallProgressListener listener, InstallStage stage, String details) {
        if (listener != null) {
            listener.onStageChanged(stage, details);
        }
    }
}