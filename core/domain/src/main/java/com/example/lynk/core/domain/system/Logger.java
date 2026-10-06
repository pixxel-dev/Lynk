package com.example.lynk.core.domain.system;

public interface Logger {
    void startRecording();
    String stopRecording();
    boolean isRecording();
}
