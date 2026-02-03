package com.datacentric.timesense.utils.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class LocalFileSystemStorageProvider implements IStorageProvider {

    private static final Path rootPath = Paths.get("data/storage");
    private static final String OBJECT_NOT_FOUND = "Object not found: ";
    private static final String UPLOAD_FAILED = "objectId cannot be null or empty";

    public LocalFileSystemStorageProvider() throws IOException {
        Files.createDirectories(rootPath);
    }

    @Override
    public String put(
        String objectId, InputStream inputStream, long contentLength) throws Exception {

        if (objectId == null || objectId.isEmpty()) {
            throw new IllegalArgumentException(UPLOAD_FAILED);
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null");
        }

        Path filePath = resolveSafePath(objectId);

        try (InputStream in = inputStream) {
            Files.createDirectories(filePath.getParent());
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            return objectId;
        } catch (IOException e) {
            throw new Exception("Failed to store file: " + objectId, e);
        }
    }

    @Override
    public void get(String objectId, OutputStream outputStream) throws Exception {

        if (objectId == null || objectId.isEmpty()) {
            throw new IllegalArgumentException(UPLOAD_FAILED);
        }

        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream cannot be null");
        }

        Path filePath = resolveSafePath(objectId);

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException(OBJECT_NOT_FOUND + objectId);
        }

        try {
            Files.copy(filePath, outputStream);
        } catch (IOException e) {
            throw new Exception("Failed to read file: " + objectId, e);
        }
    }

    @Override
    public void delete(String objectId) throws Exception {
        if (objectId == null || objectId.isEmpty()) {
            throw new IllegalArgumentException(UPLOAD_FAILED);
        }

        Path filePath = resolveSafePath(objectId);

        try {

            Files.deleteIfExists(filePath);

            // Recursively delete empty parent directories
            cleanupEmptyParentDirectories(filePath.getParent());

        } catch (IOException e) {
            throw new Exception("Failed to delete file", e);
        }
    }

    /**
     * Prevents path traversal attacks and ensures file stays inside ROOT_PATH.
     */
    private Path resolveSafePath(String objectId) throws IOException {
        Path resolved = rootPath.resolve(objectId).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new SecurityException("Invalid objectId path: " + objectId);
        }
        return resolved;
    }

    /**
     * Recursively deletes empty parent directories up to rootPath.
     */
    private void cleanupEmptyParentDirectories(Path directory) throws IOException {
        while (directory != null && !directory.equals(rootPath)) {

            try {
                // If the directory is not empty, we stop
                if (Files.list(directory).findAny().isPresent()) {
                    break;
                }

                Files.delete(directory);
                directory = directory.getParent();

            } catch (IOException e) {
                break;
            }
        }
    }
}