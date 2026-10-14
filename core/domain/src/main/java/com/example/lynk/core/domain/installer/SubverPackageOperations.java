package com.example.lynk.core.domain.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Бизнес-логика управления пакетами по аналогии с ru.subver55.hudsettings:
 * - actionInstallPackage (потоковая установка APK в профиль User 10)
 * - actionGiveMyPermissions (выдача всех разрешений для авто + защита от сна)
 * - actionForceStop (принудительная остановка приложения)
 * - actionRemovePackage (удаление приложения из профиля)
 */
public class SubverPackageOperations {

    /**
     * Интерфейс запуска shell-процессов (через Shizuku или локальный ADB/Shell).
     */
    public interface ShellExecutor {
        Process startProcess(String command) throws Exception;
    }

    private final ShellExecutor shellExecutor;

    public SubverPackageOperations(ShellExecutor shellExecutor) {
        this.shellExecutor = shellExecutor;
    }

    /**
     * Тихая потоковая установка APK (pm install -r -g --user <userId> -S <size>)
     * с автоматической выдачей всех разрешений после успешной установки.
     */
    public boolean installApkAndGrantPermissions(
            File apkFile,
            String packageName,
            int userId,
            String expectedSha256
    ) {
        if (apkFile == null || !apkFile.exists()) {
            return false;
        }

        if (expectedSha256 != null && !expectedSha256.trim().isEmpty()) {
            if (!verifySha256(apkFile, expectedSha256)) {
                return false;
            }
        }

        boolean installed = streamInstallApk(apkFile, userId);
        if (installed && packageName != null && !packageName.trim().isEmpty()) {
            giveMyPermissions(packageName, userId);
        }
        return installed;
    }

    /**
     * Аналог actionGiveMyPermissions из ru.subver55.hudsettings:
     * выдаёт пакету все права для профиля User 10 и отключает оптимизацию батареи.
     */
    public List<String> giveMyPermissions(String packageName, int userId) {
        String pkg = packageName.trim();
        String[] commands = new String[]{
                "pm grant --user " + userId + " " + pkg + " android.permission.READ_EXTERNAL_STORAGE",
                "pm grant --user " + userId + " " + pkg + " android.permission.WRITE_EXTERNAL_STORAGE",
                "pm grant --user " + userId + " " + pkg + " android.permission.POST_NOTIFICATIONS",
                "pm grant --user " + userId + " " + pkg + " android.permission.ACCESS_FINE_LOCATION",
                "pm grant --user " + userId + " " + pkg + " android.permission.ACCESS_COARSE_LOCATION",
                "appops set --user " + userId + " " + pkg + " SYSTEM_ALERT_WINDOW allow",
                "appops set --user " + userId + " " + pkg + " MANAGE_EXTERNAL_STORAGE allow",
                "appops set --user " + userId + " " + pkg + " REQUEST_INSTALL_PACKAGES allow",
                "appops set --user " + userId + " " + pkg + " GET_USAGE_STATS allow",
                "dumpsys deviceidle whitelist +" + pkg
        };

        List<String> logs = new ArrayList<>();
        for (String cmd : commands) {
            String out = executeCommand(cmd).trim();
            logs.add(cmd + (out.isEmpty() ? " -> OK" : " -> " + out));
        }
        return logs;
    }

    /**
     * Аналог actionForceStop (forceStopBtn) из ru.subver55.hudsettings.
     */
    public boolean forceStopPackage(String packageName, int userId) {
        if (packageName == null || packageName.trim().isEmpty()) return false;
        executeCommand("am force-stop --user " + userId + " " + packageName.trim());
        return true;
    }

    /**
     * Аналог actionRemovePackage (deleteAppButton) из ru.subver55.hudsettings.
     */
    public boolean removePackage(String packageName, int userId) {
        if (packageName == null || packageName.trim().isEmpty()) return false;
        String out = executeCommand("pm uninstall --user " + userId + " " + packageName.trim());
        return out.contains("Success");
    }

    private boolean streamInstallApk(File apkFile, int userId) {
        Process process = null;
        try {
            long sizeBytes = apkFile.length();
            String cmd = "pm install -r -g --user " + userId + " -S " + sizeBytes;
            process = shellExecutor.startProcess(cmd);

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

    private boolean verifySha256(File file, String expectedHex) {
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equalsIgnoreCase(expectedHex.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private String executeCommand(String command) {
        Process process = null;
        StringBuilder out = new StringBuilder();
        try {
            process = shellExecutor.startProcess(command);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return out.toString();
    }
}