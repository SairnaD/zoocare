package com.zoo.care.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:src/main/resources/photos}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
        log.info("Photo upload directory: {}", root);
    }

    public Path getRoot() {
        return root;
    }

    /** Returns an error message, or null if the file is acceptable. */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Please choose an image";
        }
        if (!EXTENSIONS.containsKey(file.getContentType())) {
            return "Only JPG, PNG, WebP or GIF images are allowed";
        }
        return null;
    }

    /** Saves the file under a generated name and returns that file name. */
    public String store(MultipartFile file) {
        String error = validate(file);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        // The name is generated, so the client's file name is never used (no path traversal).
        String filename = UUID.randomUUID() + EXTENSIONS.get(file.getContentType());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store photo", e);
        }
        return filename;
    }
}