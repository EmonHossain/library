package com.example.library.storage;

import com.example.library.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(StorageProperties properties) throws IOException {
        this.root = Paths.get(properties.getLocalRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String storageType() {
        return "local";
    }

    @Override
    public void store(String key, InputStream inputStream, long sizeBytes, String contentType) throws IOException {
        Path destination = resolve(key);
        Files.createDirectories(destination.getParent());
        Files.copy(inputStream, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public StoredObject load(String key, String contentType, String filename) throws IOException {
        return new StoredObject(Files.readAllBytes(resolve(key)), contentType, filename);
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return resolved;
    }
}
