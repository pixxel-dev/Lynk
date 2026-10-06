package com.example.lynk.core.domain.floating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FloatingButtonConfig {

    private boolean quickLaunchEnabled;
    private boolean fullscreenOverlayEnabled;
    private boolean homeNavigatorEnabled;
    private boolean backNavigatorEnabled;
    private boolean refreshNavigatorEnabled;
    private boolean separateButtonsEnabled;

    private List<String> quickLaunchApps;
    private List<String> fullscreenApps;

    private int quickLaunchButtonSize;
    private int fullscreenButtonSize;
    private int homeButtonSize;
    private int backButtonSize;
    private int refreshButtonSize;
    private int combinedButtonSize;

    private int quickLaunchX;
    private int quickLaunchY;
    private int fullscreenX;
    private int fullscreenY;
    private int homeX;
    private int homeY;
    private int backX;
    private int backY;
    private int refreshX;
    private int refreshY;
    private int combinedX;
    private int combinedY;

    private int opacityPercent;
    private int quickLaunchOpacityPercent;
    private int fullscreenOpacityPercent;
    private int homeOpacityPercent;
    private int backOpacityPercent;
    private int refreshOpacityPercent;

    private boolean secondaryDisplayMirroring;

    private List<FloatingButtonAction> actions;

    public FloatingButtonConfig() {
        this.quickLaunchEnabled = false;
        this.fullscreenOverlayEnabled = false;
        this.homeNavigatorEnabled = false;
        this.backNavigatorEnabled = false;
        this.refreshNavigatorEnabled = false;
        this.separateButtonsEnabled = false;

        this.quickLaunchApps = new ArrayList<>();
        this.fullscreenApps = new ArrayList<>();

        this.quickLaunchButtonSize = 48;
        this.fullscreenButtonSize = 48;
        this.homeButtonSize = 48;
        this.backButtonSize = 48;
        this.refreshButtonSize = 48;
        this.combinedButtonSize = 48;

        this.quickLaunchX = 0;
        this.quickLaunchY = 200;
        this.fullscreenX = 100;
        this.fullscreenY = 100;
        this.homeX = 0;
        this.homeY = 300;
        this.backX = 0;
        this.backY = 400;
        this.refreshX = 0;
        this.refreshY = 500;
        this.combinedX = 0;
        this.combinedY = 200;

        this.opacityPercent = 85;
        this.quickLaunchOpacityPercent = 85;
        this.fullscreenOpacityPercent = 85;
        this.homeOpacityPercent = 85;
        this.backOpacityPercent = 85;
        this.refreshOpacityPercent = 85;

        this.secondaryDisplayMirroring = true;

        this.actions = new ArrayList<>();
        initDefaultActions();
    }

    public FloatingButtonConfig(FloatingButtonConfig other) {
        if (other != null) {
            this.quickLaunchEnabled = other.quickLaunchEnabled;
            this.fullscreenOverlayEnabled = other.fullscreenOverlayEnabled;
            this.homeNavigatorEnabled = other.homeNavigatorEnabled;
            this.backNavigatorEnabled = other.backNavigatorEnabled;
            this.refreshNavigatorEnabled = other.refreshNavigatorEnabled;
            this.separateButtonsEnabled = other.separateButtonsEnabled;
            this.quickLaunchApps = other.quickLaunchApps != null ? new ArrayList<>(other.quickLaunchApps) : new ArrayList<>();
            this.fullscreenApps = other.fullscreenApps != null ? new ArrayList<>(other.fullscreenApps) : new ArrayList<>();
            this.quickLaunchButtonSize = other.quickLaunchButtonSize;
            this.fullscreenButtonSize = other.fullscreenButtonSize;
            this.homeButtonSize = other.homeButtonSize;
            this.backButtonSize = other.backButtonSize;
            this.refreshButtonSize = other.refreshButtonSize;
            this.combinedButtonSize = other.combinedButtonSize;

            this.quickLaunchX = other.quickLaunchX;
            this.quickLaunchY = other.quickLaunchY;
            this.fullscreenX = other.fullscreenX;
            this.fullscreenY = other.fullscreenY;
            this.homeX = other.homeX;
            this.homeY = other.homeY;
            this.backX = other.backX;
            this.backY = other.backY;
            this.refreshX = other.refreshX;
            this.refreshY = other.refreshY;
            this.combinedX = other.combinedX;
            this.combinedY = other.combinedY;

            this.opacityPercent = other.opacityPercent;
            this.quickLaunchOpacityPercent = other.quickLaunchOpacityPercent;
            this.fullscreenOpacityPercent = other.fullscreenOpacityPercent;
            this.homeOpacityPercent = other.homeOpacityPercent;
            this.backOpacityPercent = other.backOpacityPercent;
            this.refreshOpacityPercent = other.refreshOpacityPercent;

            this.secondaryDisplayMirroring = other.secondaryDisplayMirroring;
            this.actions = new ArrayList<>();
            if (other.actions != null) {
                for (FloatingButtonAction action : other.actions) {
                    this.actions.add(new FloatingButtonAction(
                        action.getId(),
                        action.getLabel(),
                        action.getActionType(),
                        action.getTargetPackages(),
                        action.isEnabled()
                    ));
                }
            }
        } else {
            this.quickLaunchApps = new ArrayList<>();
            this.fullscreenApps = new ArrayList<>();
            this.actions = new ArrayList<>();
            initDefaultActions();
        }
    }

    public FloatingButtonConfig copy() {
        return new FloatingButtonConfig(this);
    }

    private void initDefaultActions() {
        this.actions.add(new FloatingButtonAction("ql_menu", "Quick Launch Menu", FloatingButtonAction.ActionType.QUICK_LAUNCH, quickLaunchApps, quickLaunchEnabled));
        this.actions.add(new FloatingButtonAction("fs_toggle", "Fullscreen Toggle", FloatingButtonAction.ActionType.FULLSCREEN_TOGGLE, fullscreenApps, fullscreenOverlayEnabled));
        this.actions.add(new FloatingButtonAction("nav_home", "Навигатор \"Домой\"", FloatingButtonAction.ActionType.HOME, new ArrayList<>(), homeNavigatorEnabled));
        this.actions.add(new FloatingButtonAction("nav_back", "Навигатор \"Назад\"", FloatingButtonAction.ActionType.BACK, new ArrayList<>(), backNavigatorEnabled));
        this.actions.add(new FloatingButtonAction("nav_refresh", "Обновить / Refresh", FloatingButtonAction.ActionType.REFRESH, new ArrayList<>(), refreshNavigatorEnabled));
    }

    // Business logic methods
    public boolean isAnyOverlayActive() {
        return quickLaunchEnabled || fullscreenOverlayEnabled || homeNavigatorEnabled || backNavigatorEnabled || refreshNavigatorEnabled;
    }

    public boolean shouldShowFullscreenForApp(String packageName) {
        if (!fullscreenOverlayEnabled || packageName == null) {
            return false;
        }
        return fullscreenApps.contains(packageName);
    }

    public int calculateSecondaryX(int primaryX, int elementWidth, int screenWidth) {
        return Math.max(0, screenWidth - primaryX - elementWidth);
    }

    public int calculateSecondaryY(int primaryY, int elementHeight, int screenHeight) {
        return Math.max(0, screenHeight - primaryY - elementHeight);
    }

    public void addQuickLaunchApp(String packageName) {
        if (packageName != null && !packageName.trim().isEmpty() && !quickLaunchApps.contains(packageName)) {
            quickLaunchApps.add(packageName);
            syncActionsWithApps();
        }
    }

    public void removeQuickLaunchApp(String packageName) {
        quickLaunchApps.remove(packageName);
        syncActionsWithApps();
    }

    public void addFullscreenApp(String packageName) {
        if (packageName != null && !packageName.trim().isEmpty() && !fullscreenApps.contains(packageName)) {
            fullscreenApps.add(packageName);
            syncActionsWithApps();
        }
    }

    public void removeFullscreenApp(String packageName) {
        fullscreenApps.remove(packageName);
        syncActionsWithApps();
    }

    public void syncActionsWithApps() {
        for (FloatingButtonAction action : actions) {
            if (action.getActionType() == FloatingButtonAction.ActionType.QUICK_LAUNCH) {
                action.setTargetPackages(quickLaunchApps);
                action.setEnabled(quickLaunchEnabled);
            } else if (action.getActionType() == FloatingButtonAction.ActionType.FULLSCREEN_TOGGLE) {
                action.setTargetPackages(fullscreenApps);
                action.setEnabled(fullscreenOverlayEnabled);
            } else if (action.getActionType() == FloatingButtonAction.ActionType.HOME) {
                action.setEnabled(homeNavigatorEnabled);
            } else if (action.getActionType() == FloatingButtonAction.ActionType.BACK) {
                action.setEnabled(backNavigatorEnabled);
            } else if (action.getActionType() == FloatingButtonAction.ActionType.REFRESH) {
                action.setEnabled(refreshNavigatorEnabled);
            }
        }
    }

    // Getters and Setters

    public boolean isQuickLaunchEnabled() {
        return quickLaunchEnabled;
    }

    public void setQuickLaunchEnabled(boolean quickLaunchEnabled) {
        this.quickLaunchEnabled = quickLaunchEnabled;
        syncActionsWithApps();
    }

    public boolean isFullscreenOverlayEnabled() {
        return fullscreenOverlayEnabled;
    }

    public void setFullscreenOverlayEnabled(boolean fullscreenOverlayEnabled) {
        this.fullscreenOverlayEnabled = fullscreenOverlayEnabled;
        syncActionsWithApps();
    }

    public boolean isHomeNavigatorEnabled() {
        return homeNavigatorEnabled;
    }

    public void setHomeNavigatorEnabled(boolean homeNavigatorEnabled) {
        this.homeNavigatorEnabled = homeNavigatorEnabled;
        syncActionsWithApps();
    }

    public boolean isBackNavigatorEnabled() {
        return backNavigatorEnabled;
    }

    public void setBackNavigatorEnabled(boolean backNavigatorEnabled) {
        this.backNavigatorEnabled = backNavigatorEnabled;
        syncActionsWithApps();
    }

    public boolean isRefreshNavigatorEnabled() {
        return refreshNavigatorEnabled;
    }

    public void setRefreshNavigatorEnabled(boolean refreshNavigatorEnabled) {
        this.refreshNavigatorEnabled = refreshNavigatorEnabled;
        syncActionsWithApps();
    }

    public boolean isSeparateButtonsEnabled() {
        return separateButtonsEnabled;
    }

    public void setSeparateButtonsEnabled(boolean separateButtonsEnabled) {
        this.separateButtonsEnabled = separateButtonsEnabled;
    }

    public List<String> getQuickLaunchApps() {
        return Collections.unmodifiableList(quickLaunchApps);
    }

    public void setQuickLaunchApps(List<String> quickLaunchApps) {
        this.quickLaunchApps = quickLaunchApps != null ? new ArrayList<>(quickLaunchApps) : new ArrayList<>();
        syncActionsWithApps();
    }

    public List<String> getFullscreenApps() {
        return Collections.unmodifiableList(fullscreenApps);
    }

    public void setFullscreenApps(List<String> fullscreenApps) {
        this.fullscreenApps = fullscreenApps != null ? new ArrayList<>(fullscreenApps) : new ArrayList<>();
        syncActionsWithApps();
    }

    public int getQuickLaunchButtonSize() {
        return quickLaunchButtonSize;
    }

    public void setQuickLaunchButtonSize(int quickLaunchButtonSize) {
        this.quickLaunchButtonSize = Math.max(30, Math.min(150, quickLaunchButtonSize));
    }

    public int getFullscreenButtonSize() {
        return fullscreenButtonSize;
    }

    public void setFullscreenButtonSize(int fullscreenButtonSize) {
        this.fullscreenButtonSize = Math.max(30, Math.min(150, fullscreenButtonSize));
    }

    public int getHomeButtonSize() {
        return homeButtonSize;
    }

    public void setHomeButtonSize(int homeButtonSize) {
        this.homeButtonSize = Math.max(30, Math.min(150, homeButtonSize));
    }

    public int getBackButtonSize() {
        return backButtonSize;
    }

    public void setBackButtonSize(int backButtonSize) {
        this.backButtonSize = Math.max(30, Math.min(150, backButtonSize));
    }

    public int getRefreshButtonSize() {
        return refreshButtonSize;
    }

    public void setRefreshButtonSize(int refreshButtonSize) {
        this.refreshButtonSize = Math.max(30, Math.min(150, refreshButtonSize));
    }

    public int getCombinedButtonSize() {
        return combinedButtonSize;
    }

    public void setCombinedButtonSize(int combinedButtonSize) {
        this.combinedButtonSize = Math.max(30, Math.min(150, combinedButtonSize));
    }

    public int getQuickLaunchX() {
        return quickLaunchX;
    }

    public void setQuickLaunchX(int quickLaunchX) {
        this.quickLaunchX = quickLaunchX;
    }

    public int getQuickLaunchY() {
        return quickLaunchY;
    }

    public void setQuickLaunchY(int quickLaunchY) {
        this.quickLaunchY = quickLaunchY;
    }

    public int getFullscreenX() {
        return fullscreenX;
    }

    public void setFullscreenX(int fullscreenX) {
        this.fullscreenX = fullscreenX;
    }

    public int getFullscreenY() {
        return fullscreenY;
    }

    public void setFullscreenY(int fullscreenY) {
        this.fullscreenY = fullscreenY;
    }

    public int getHomeX() {
        return homeX;
    }

    public void setHomeX(int homeX) {
        this.homeX = homeX;
    }

    public int getHomeY() {
        return homeY;
    }

    public void setHomeY(int homeY) {
        this.homeY = homeY;
    }

    public int getBackX() {
        return backX;
    }

    public void setBackX(int backX) {
        this.backX = backX;
    }

    public int getBackY() {
        return backY;
    }

    public void setBackY(int backY) {
        this.backY = backY;
    }

    public int getRefreshX() {
        return refreshX;
    }

    public void setRefreshX(int refreshX) {
        this.refreshX = refreshX;
    }

    public int getRefreshY() {
        return refreshY;
    }

    public void setRefreshY(int refreshY) {
        this.refreshY = refreshY;
    }

    public int getCombinedX() {
        return combinedX;
    }

    public void setCombinedX(int combinedX) {
        this.combinedX = combinedX;
    }

    public int getCombinedY() {
        return combinedY;
    }

    public void setCombinedY(int combinedY) {
        this.combinedY = combinedY;
    }

    public int getOpacityPercent() {
        return opacityPercent;
    }

    public void setOpacityPercent(int opacityPercent) {
        this.opacityPercent = Math.max(10, Math.min(100, opacityPercent));
    }

    public int getQuickLaunchOpacityPercent() {
        return quickLaunchOpacityPercent;
    }

    public void setQuickLaunchOpacityPercent(int quickLaunchOpacityPercent) {
        this.quickLaunchOpacityPercent = Math.max(10, Math.min(100, quickLaunchOpacityPercent));
    }

    public int getFullscreenOpacityPercent() {
        return fullscreenOpacityPercent;
    }

    public void setFullscreenOpacityPercent(int fullscreenOpacityPercent) {
        this.fullscreenOpacityPercent = Math.max(10, Math.min(100, fullscreenOpacityPercent));
    }

    public int getHomeOpacityPercent() {
        return homeOpacityPercent;
    }

    public void setHomeOpacityPercent(int homeOpacityPercent) {
        this.homeOpacityPercent = Math.max(10, Math.min(100, homeOpacityPercent));
    }

    public int getBackOpacityPercent() {
        return backOpacityPercent;
    }

    public void setBackOpacityPercent(int backOpacityPercent) {
        this.backOpacityPercent = Math.max(10, Math.min(100, backOpacityPercent));
    }

    public int getRefreshOpacityPercent() {
        return refreshOpacityPercent;
    }

    public void setRefreshOpacityPercent(int refreshOpacityPercent) {
        this.refreshOpacityPercent = Math.max(10, Math.min(100, refreshOpacityPercent));
    }

    public boolean isSecondaryDisplayMirroring() {
        return secondaryDisplayMirroring;
    }

    public void setSecondaryDisplayMirroring(boolean secondaryDisplayMirroring) {
        this.secondaryDisplayMirroring = secondaryDisplayMirroring;
    }

    public List<FloatingButtonAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public void setActions(List<FloatingButtonAction> actions) {
        this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
    }

    public void addAction(FloatingButtonAction action) {
        if (action != null && !actions.contains(action)) {
            actions.add(action);
        }
    }
}
