package com.example.lynk.core.domain.app;

public class AppItem {
    private final String label;
    private final String packageName;
    private boolean isChecked;

    public AppItem(String label, String packageName, boolean isChecked) {
        this.label = label;
        this.packageName = packageName;
        this.isChecked = isChecked;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }
}
