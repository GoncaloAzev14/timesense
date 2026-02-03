package com.datacentric.timesense.utils.storage;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;

public class AzureBlobStorageProvider implements IStorageProvider {

    private final BlobContainerClient containerClient;
    private static final String OBJECT_NOT_FOUND = "Object not found: ";
    private static final String UPLOAD_FAILED = "Failed to upload blob: ";

    public AzureBlobStorageProvider(
            @StorageProperty("azure.storage.connection-string") String connectionString,
            @StorageProperty(value = "azure.storage.container-name",
                defaultValue = "timesense-attachments") String containerName) {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public String put(
        String objectId, InputStream inputStream, long contentLength) throws Exception {
        try {
            BlobClient blobClient = containerClient.getBlobClient(objectId);

            if (contentLength >= 0) {
                blobClient.upload(inputStream, contentLength, true);
            } else {
                blobClient.upload(inputStream, true);
            }

            return objectId;
        } catch (BlobStorageException e) {
            throw new Exception(UPLOAD_FAILED + objectId, e);
        } catch (Exception e) {
            throw new Exception(UPLOAD_FAILED + objectId, e);
        }
    }

    @Override
    public void get(String objectId, OutputStream outputStream) throws Exception {
        try {
            BlobClient blobClient = containerClient.getBlobClient(objectId);

            if (!blobClient.exists()) {
                throw new FileNotFoundException(OBJECT_NOT_FOUND + objectId);
            }

            blobClient.downloadStream(outputStream);

        } catch (BlobStorageException e) {
            if (e.getErrorCode() == BlobErrorCode.BLOB_NOT_FOUND) {
                throw new FileNotFoundException(OBJECT_NOT_FOUND + objectId);
            }
            throw new Exception("Failed to download blob: " + objectId, e);
        }
    }

    @Override
    public void delete(String objectId) throws Exception {
        try {
            BlobClient blobClient = containerClient.getBlobClient(objectId);

            blobClient.deleteIfExists();
        } catch (BlobStorageException e) {
            throw new Exception("Failed to delete blob: " + objectId, e);
        }
    }
}
