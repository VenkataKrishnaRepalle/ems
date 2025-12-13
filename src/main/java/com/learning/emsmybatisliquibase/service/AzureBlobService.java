package com.learning.emsmybatisliquibase.service;

import java.util.Map;

public interface AzureBlobService {

    byte[] downloadFile(String containerName, String directoryName);

    void uploadWithMetadata(String containerName, String blobName, byte[] data, Map<String, String> metadata);

    void updateMetadata(String containerName, String blobName, Map<String, String> metadata);

    String findBlobName(String containerName, String directoryName);

    Map<String, String> getMetadata(String containerName, String blobName);

    void uploadFile(String container, String blobName, String path);

    void downloadFile(String container, String blobName, String localPath);
}
