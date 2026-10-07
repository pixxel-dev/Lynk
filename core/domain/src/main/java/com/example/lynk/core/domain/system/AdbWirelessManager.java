package com.example.lynk.core.domain.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
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
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) {
                return "127.0.0.1";
            }
            List<NetworkInterface> interfaces = Collections.list(nis);
            // First pass: look specifically for Wi-Fi or Ethernet interfaces
            for (NetworkInterface intf : interfaces) {
                if (intf == null || intf.isLoopback() || !intf.isUp()) continue;
                String name = intf.getName();
                if (name == null) continue;
                name = name.toLowerCase();
                if (name.contains("wlan") || name.contains("eth") || name.contains("ap")) {
                    Enumeration<InetAddress> addrsEnum = intf.getInetAddresses();
                    if (addrsEnum != null) {
                        List<InetAddress> addrs = Collections.list(addrsEnum);
                        for (InetAddress addr : addrs) {
                            if (addr != null && !addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                                String host = addr.getHostAddress();
                                if (host != null && !host.isEmpty() && !host.equals("127.0.0.1")) {
                                    return host;
                                }
                            }
                        }
                    }
                }
            }
            // Second pass: fallback to any active non-loopback IPv4 address
            for (NetworkInterface intf : interfaces) {
                if (intf == null || intf.isLoopback() || !intf.isUp()) continue;
                Enumeration<InetAddress> addrsEnum = intf.getInetAddresses();
                if (addrsEnum != null) {
                    List<InetAddress> addrs = Collections.list(addrsEnum);
                    for (InetAddress addr : addrs) {
                        if (addr != null && !addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String host = addr.getHostAddress();
                            if (host != null && !host.isEmpty() && !host.equals("127.0.0.1")) {
                                return host;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "127.0.0.1";
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
        try {
            if (ip == null || ip.trim().isEmpty()) {
                return "Invalid IP address";
            }
            int targetPort = (port <= 0) ? DEFAULT_PORT : port;
            String target = ip.trim() + ":" + targetPort;
            return executeAdbCommand("adb connect " + target);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Executes 'adb disconnect <ip>:<port>' or 'adb disconnect'.
     *
     * @param ip Target IP address.
     * @param port Target ADB port.
     * @return Execution output message.
     */
    public String disconnect(String ip, int port) {
        try {
            if (ip == null || ip.trim().isEmpty()) {
                return executeAdbCommand("adb disconnect");
            }
            int targetPort = (port <= 0) ? DEFAULT_PORT : port;
            String target = ip.trim() + ":" + targetPort;
            return executeAdbCommand("adb disconnect " + target);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks if the specified IP and port (or any device) is currently connected via 'adb devices'.
     *
     * @param ip Target IP address.
     * @param port Target ADB port.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected(String ip, int port) {
        try {
            String devicesOutput = executeAdbCommand("adb devices");
            if (devicesOutput == null || devicesOutput.isEmpty()) {
                return false;
            }
            int targetPort = (port <= 0) ? DEFAULT_PORT : port;
            String target = (ip != null && !ip.trim().isEmpty()) ? ip.trim() + ":" + targetPort : "";

            String[] lines = devicesOutput.split("\n");
            for (String line : lines) {
                if (line == null) continue;
                String trimmed = line.trim();
                if (trimmed.startsWith("List of devices")) continue;
                if (!target.isEmpty() && trimmed.contains(target) && trimmed.contains("device")) {
                    return true;
                } else if (target.isEmpty() && trimmed.endsWith("\tdevice")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Executes 'adb devices' and returns raw output.
     *
     * @return ADB devices list output string.
     */
    public String getConnectedDevices() {
        try {
            String output = executeAdbCommand("adb devices");
            return output != null ? output : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Helper method to execute system command via Runtime.getRuntime().exec().
     */
    public String executeAdbCommand(String command) {
        try {
            if (command == null || command.trim().isEmpty()) {
                return "";
            }
            StringBuilder output = new StringBuilder();
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
            return output.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
