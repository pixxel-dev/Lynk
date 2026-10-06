package com.example.lynk.core.domain.system;

import java.util.Map;

public class DeviceInfo {
    private final String manufacturer;
    private final String brand;
    private final String model;
    private final String device;
    private final int androidSdk;
    private final String androidRelease;
    private final Map<String, String> carSystemProperties;

    public DeviceInfo(String manufacturer, String brand, String model, String device, int androidSdk, String androidRelease, Map<String, String> carSystemProperties) {
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.model = model;
        this.device = device;
        this.androidSdk = androidSdk;
        this.androidRelease = androidRelease;
        this.carSystemProperties = carSystemProperties;
    }

    public String getManufacturer() { return manufacturer; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getDevice() { return device; }
    public int getAndroidSdk() { return androidSdk; }
    public String getAndroidRelease() { return androidRelease; }
    public Map<String, String> getCarSystemProperties() { return carSystemProperties; }
}
