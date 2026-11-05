package com.zoo.care.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void validate_missingFileReturnsMessage() {
        assertThat(service.validate(null)).isEqualTo("Please choose an image");
        assertThat(service.validate(new MockMultipartFile("photo", new byte[0])))
                .isEqualTo("Please choose an image");
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
}