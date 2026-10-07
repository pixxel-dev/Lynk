package com.example.lynk.core.domain.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Manages Wireless ADB connections over Wi-Fi network.
 */
public class AdbWirelessManager {

    private static final int DEFAULT_PORT = 5555;

    /**
     * Automatically retrieves the local Wi-Fi / Ethernet IPv4 address of the device.
     *
     * @return IPv4 address string, or "127.0.0.1" if auto-detection fails.
     */
    public String getWifiIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            // First pass: look specifically for Wi-Fi or Ethernet interfaces
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                String name = intf.getName().toLowerCase();
                if (name.contains("wlan") || name.contains("eth") || name.contains("ap")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String host = addr.getHostAddress();
                            if (host != null && !host.isEmpty() && !host.equals("127.0.0.1")) {
                                return host;
                            }
                        }
                    }
                }
            }
            // Second pass: fallback to any active non-loopback IPv4 address
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String host = addr.getHostAddress();
                        if (host != null && !host.isEmpty() && !host.equals("127.0.0.1")) {
                            return host;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    /**
     * Executes 'adb connect <ip>:<port>'.
     *
     * @param ip Target IP address.
     * @param port Target ADB port (defaults to 5555 if invalid or <= 0).
     * @return Execution output message.
     */
    public String connect(String ip, int port) {
        if (ip == null || ip.trim().isEmpty()) {
            return "Invalid IP address";
        }
        int targetPort = (port <= 0) ? DEFAULT_PORT : port;
        String target = ip.trim() + ":" + targetPort;
        return executeAdbCommand("adb connect " + target);
    }

    /**
     * Executes 'adb disconnect <ip>:<port>' or 'adb disconnect'.
     *
     * @param ip Target IP address.
     * @param port Target ADB port.
     * @return Execution output message.
     */
    public String disconnect(String ip, int port) {
        if (ip == null || ip.trim().isEmpty()) {
            return executeAdbCommand("adb disconnect");
        }
        int targetPort = (port <= 0) ? DEFAULT_PORT : port;
        String target = ip.trim() + ":" + targetPort;
        return executeAdbCommand("adb disconnect " + target);
    }

    /**
     * Checks if the specified IP and port (or any device) is currently connected via 'adb devices'.
     *
     * @param ip Target IP address.
     * @param port Target ADB port.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected(String ip, int port) {
        String devicesOutput = executeAdbCommand("adb devices");
        if (devicesOutput == null || devicesOutput.isEmpty()) {
            return false;
        }
        int targetPort = (port <= 0) ? DEFAULT_PORT : port;
        String target = (ip != null && !ip.trim().isEmpty()) ? ip.trim() + ":" + targetPort : "";

        String[] lines = devicesOutput.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("List of devices")) continue;
            if (!target.isEmpty() && trimmed.contains(target) && trimmed.contains("device")) {
                return true;
            } else if (target.isEmpty() && trimmed.endsWith("\tdevice")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes 'adb devices' and returns raw output.
     *
     * @return ADB devices list output string.
     */
    public String getConnectedDevices() {
        return executeAdbCommand("adb devices");
    }

    /**
     * Helper method to execute system command via Runtime.getRuntime().exec().
     */
    public String executeAdbCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append("\n");
                    }
                    output.append(line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        return output.toString().trim();
    }
}
