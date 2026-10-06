package com.example.lynk.core.domain.installer;

import java.io.File;

/**
 * Domain logic representing the fallback strategy for installing APKs.
 * Tries Pine, then Shizuku, then Native ADB, then Local ADB, then File Provider / Package Installer.
 */
public class WaterfallInstallStrategy {

    private final ApkInstaller pineInstaller;
    private final ApkInstaller shizukuInstaller;
    private final ApkInstaller nativeAdbInstaller;
    private final ApkInstaller localAdbInstaller;
    private final ApkInstaller packageInstaller;

    public WaterfallInstallStrategy(ApkInstaller pineInstaller,
                                    ApkInstaller shizukuInstaller,
                                    ApkInstaller nativeAdbInstaller,
                                    ApkInstaller localAdbInstaller,
                                    ApkInstaller packageInstaller) {
        this.pineInstaller = pineInstaller;
        this.shizukuInstaller = shizukuInstaller;
        this.nativeAdbInstaller = nativeAdbInstaller;
        this.localAdbInstaller = localAdbInstaller;
        this.packageInstaller = packageInstaller;
    }

    public void executeInstall(File file, ApkInstaller.InstallCallback finalCallback) {
        pineInstaller.install(file, pineResult -> {
            if (pineResult.isSuccess()) {
                finalCallback.onResult(pineResult);
            } else {
                shizukuInstaller.install(file, shizukuResult -> {
                    if (shizukuResult.isSuccess()) {
                        finalCallback.onResult(shizukuResult);
                    } else {
                        nativeAdbInstaller.install(file, nativeResult -> {
                            if (nativeResult.isSuccess()) {
                                finalCallback.onResult(nativeResult);
                            } else {
                                localAdbInstaller.install(file, localResult -> {
                                    if (localResult.isSuccess()) {
                                        finalCallback.onResult(localResult);
                                    } else {
                                        packageInstaller.install(file, finalCallback);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }
}
