package com.learning.emsmybatisliquibase.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.learning.emsmybatisliquibase.service.AzureBlobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Service
public class AzureBlobServiceImpl implements AzureBlobService {

    private final BlobServiceClient blobServiceClient;
    public AzureBlobServiceImpl(@Value("${AZURE_STORAGE_CONNECTION_STRING}") String conn) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(conn)
                .buildClient();
    }

    public void uploadWithMetadata(String containerName, String blobName, byte[] data, Map<String, String> metadata) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        InputStream inputStream = new ByteArrayInputStream(data);
        blobClient.upload(inputStream, data.length, true);
        if (metadata != null && !metadata.isEmpty()) {
            blobClient.setMetadata(metadata);
        }
    }

    public void updateMetadata(String containerName, String blobName, Map<String, String> metadata) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        if (blobClient.exists()) {
            blobClient.setMetadata(metadata);
        } else {
            throw new IllegalStateException("Blob not found: " + blobName);
        }
    }

    public String findBlobName(String containerName, String directoryName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            return null;
        }
        // Find the first CSV file in the specified directory (folder)
        for (BlobItem blobItem : containerClient.listBlobsByHierarchy(directoryName + "/")) {
            if (!blobItem.isPrefix() && blobItem.getName().endsWith(".csv")) {
                return blobItem.getName();
            }
        }
        return null;
    }

    public Map<String, String> getMetadata(String containerName, String blobName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new IllegalStateException("Blob not found: " + blobName);
        }
        BlobProperties properties = blobClient.getProperties();
        return properties.getMetadata();
    }

    public byte[] downloadFile(String containerName, String directoryName) {
        String blobName = findBlobName(containerName, directoryName);
        if (blobName == null) {
            return null;
        }
        
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        return outputStream.toByteArray();
    }

    public void uploadFile(String container, String blobName, String path) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
        containerClient.createIfNotExists();

        BlobClient blob = containerClient.getBlobClient(blobName);
        blob.uploadFromFile(path, true);
    }

    public void downloadFile(String container, String blobName, String localPath) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
        BlobClient blob = containerClient.getBlobClient(blobName);
        blob.downloadToFile(localPath, true);
    }
}
