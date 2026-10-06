package com.example.lynk.core.domain.installer;

import java.io.File;

public interface ApkInstaller {
    void install(File apkFile, InstallCallback callback);

    interface InstallCallback {
        void onResult(InstallResult result);
    }
}
