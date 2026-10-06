package com.example.lynk.core.domain.file;

public class FileItem {
    private final String name;
    private final String path;
    private final long size;
    private final long lastModified;
    private final boolean isDirectory;
    private boolean isSelected;

    public FileItem(String name, String path, long size, long lastModified, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
        this.isSelected = false;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public boolean isUpNavigation() {
        return "..".equals(name);
    }
}
