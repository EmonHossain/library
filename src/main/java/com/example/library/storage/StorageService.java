package com.example.library.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    String storageType();

    void store(String key, InputStream inputStream, long sizeBytes, String contentType) throws IOException;

    StoredObject load(String key, String contentType, String filename) throws IOException;
}
