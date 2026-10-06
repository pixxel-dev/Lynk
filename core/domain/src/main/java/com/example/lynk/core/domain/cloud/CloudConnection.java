package com.example.lynk.core.domain.cloud;

public class CloudConnection {
    private final String id;
    private final String name;
    private final String webDavUrl;
    private final String username;
    private final String passwordToken;

    public CloudConnection(String id, String name, String webDavUrl, String username, String passwordToken) {
        this.id = id;
        this.name = name;
        this.webDavUrl = webDavUrl;
        this.username = username;
        this.passwordToken = passwordToken;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWebDavUrl() {
        return webDavUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordToken() {
        return passwordToken;
    }
}
