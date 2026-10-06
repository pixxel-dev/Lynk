package com.example.lynk.core.domain.cloud;

import com.example.lynk.core.domain.file.FileItem;
import java.util.List;

public interface CloudStorageClient {
    List<FileItem> listFiles(String path) throws Exception;
}