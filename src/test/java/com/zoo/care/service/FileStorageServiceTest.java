package com.zoo.care.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new FileStorageService(tempDir.toString());
        service.init();
    }

    @Test
    void validate_missingFileIsAccepted() {
        assertThat(service.validate(null)).isNull();
        assertThat(service.validate(new MockMultipartFile("photo", new byte[0]))).isNull();
    }

    @Test
    void validate_nonImageReturnsMessage() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "a.pdf", "application/pdf", new byte[]{1});

        assertThat(service.validate(pdf)).isEqualTo("Only JPG, PNG, WebP or GIF images are allowed");
    }

    @Test
    void validate_imageIsAccepted() {
        MockMultipartFile png = new MockMultipartFile("photo", "leo.png", "image/png", new byte[]{1});

        assertThat(service.validate(png)).isNull();
    }

    @Test
    void store_usesGeneratedNameAndIgnoresClientFileName() throws IOException {
        MockMultipartFile file = new MockMultipartFile("photo", "../../evil.png", "image/png", new byte[]{1, 2});

        String stored = service.store(file);

        assertThat(stored).endsWith(".png").doesNotContain("evil").doesNotContain("/");
        assertThat(Files.exists(tempDir.resolve(stored))).isTrue();
    }

    @Test
    void store_rejectsNonImage() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "a.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.store(pdf)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void delete_removesStoredFile() {
        String stored = service.store(new MockMultipartFile("photo", "leo.png", "image/png", new byte[]{1}));

        service.delete(stored);

        assertThat(Files.exists(tempDir.resolve(stored))).isFalse();
    }

    @Test
    void delete_ignoresMissingAndBlankNames() {
        assertThatCode(() -> service.delete(null)).doesNotThrowAnyException();
        assertThatCode(() -> service.delete("")).doesNotThrowAnyException();
        assertThatCode(() -> service.delete("missing.png")).doesNotThrowAnyException();

        assertThat(Files.exists(tempDir)).isTrue(); // the photo folder itself is never deleted
    }

    @Test
    void store_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("photo", new byte[0]);

        assertThatThrownBy(() -> service.store(empty)).isInstanceOf(IllegalArgumentException.class);
    }
}