package com.datacentric.timesense.utils.storage;

import java.io.InputStream;
import java.io.OutputStream;

public interface IStorageProvider {
    /**
     * Uploads a file to storage using streaming.
     *
     * @param objectId unique identifier (UUID)
     * @param inputStream stream containing the file content
     * @param contentLength size of the file in bytes (use -1 if unknown)
     * @return the objectId that was stored
     * @throws Exception when upload fails
     */
    String put(String objectId, InputStream inputStream, long contentLength) throws Exception;

    /**
     * Retrieves a file from storage and writes it to an output stream.
     *
     * @param objectId the id of the stored object
     * @param outputStream stream where the file content will be written
     * @throws Exception when download fails or object not found
     */
    void get(String objectId, OutputStream outputStream) throws Exception;

    /**
     * Deletes a file from storage.
     *
     * @param objectId the id of the stored object
     * @throws Exception when file cannot be deleted
     */
    void delete(String objectId) throws Exception;

    /**
     * Checks if an object exists in storage.
     *
     * @param objectId the id of the stored object
     * @return true if the object exists, false otherwise
     * @throws Exception when check fails
     */
    //boolean exists(String objectId) throws Exception;
}