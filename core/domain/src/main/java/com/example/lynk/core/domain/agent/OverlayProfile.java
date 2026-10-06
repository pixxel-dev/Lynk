package com.example.lynk.core.domain.agent;

import com.example.lynk.core.domain.floating.FloatingButtonConfig;
import java.util.ArrayList;
import java.util.List;

public class OverlayProfile {
    private int id;
    private String name;
    private boolean isEnabled;
    private boolean isSeparateButtons;
    private List<FloatingButtonConfig> buttons;
    private FloatingButtonConfig config;

    public OverlayProfile() {
        this(1, "Оверлей 1", false, false, new FloatingButtonConfig());
    }

    public OverlayProfile(int id, String name, boolean isEnabled, boolean isSeparateButtons, FloatingButtonConfig config) {
        this.id = id;
        this.name = name != null ? name : "Оверлей " + id;
        this.isEnabled = isEnabled;
        this.isSeparateButtons = isSeparateButtons;
        this.config = config != null ? config : new FloatingButtonConfig();
        this.buttons = new ArrayList<>();
        this.buttons.add(this.config);
    }

    public OverlayProfile(int id, String name, boolean isEnabled, boolean isSeparateButtons, List<FloatingButtonConfig> buttons) {
        this.id = id;
        this.name = name != null ? name : "Оверлей " + id;
        this.isEnabled = isEnabled;
        this.isSeparateButtons = isSeparateButtons;
        this.buttons = buttons != null ? buttons : new ArrayList<>();
        if (!this.buttons.isEmpty()) {
            this.config = this.buttons.get(0);
        } else {
            this.config = new FloatingButtonConfig();
            this.buttons.add(this.config);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isSeparateButtons() {
        return isSeparateButtons;
    }

    public void setSeparateButtons(boolean separateButtons) {
        isSeparateButtons = separateButtons;
        if (this.config != null) {
            this.config.setSeparateButtonsEnabled(separateButtons);
        }
    }

    public List<FloatingButtonConfig> getButtons() {
        return buttons;
    }

    public void setButtons(List<FloatingButtonConfig> buttons) {
        this.buttons = buttons;
        if (buttons != null && !buttons.isEmpty()) {
            this.config = buttons.get(0);
        }
    }

    public FloatingButtonConfig getConfig() {
        return config;
    }

    public void setConfig(FloatingButtonConfig config) {
        this.config = config;
        this.buttons = new ArrayList<>();
        if (config != null) {
            this.buttons.add(config);
        }
    }
}
