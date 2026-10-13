package com.geetanjali.backend.service;

import com.geetanjali.backend.dto.response.PhotoRef;
import com.geetanjali.backend.service.storage.LocalDiskPhotoStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// LocalDiskPhotoStorageServiceTest: Verifies local file storage, security checks, and deletion.
class LocalDiskPhotoStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private LocalDiskPhotoStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalDiskPhotoStorageService(tempUploadDir.toString());
    }

    @Test
    void testSave_validJpegFile_success() throws IOException {
        byte[] content = "fake-jpeg-image-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "fabric.jpg", "image/jpeg", content
        );

        PhotoRef photoRef = storageService.save(file);

        assertNotNull(photoRef);
        assertNotNull(photoRef.url());
        assertNotNull(photoRef.publicId());
        assertTrue(photoRef.url().startsWith("/uploads/"));
        assertTrue(photoRef.url().endsWith(".jpg"));
        assertTrue(photoRef.publicId().endsWith(".jpg"));

        // Verify the file was written to disk
        Path savedFile = tempUploadDir.resolve(photoRef.publicId());
        assertTrue(Files.exists(savedFile));
        assertEquals(content.length, Files.size(savedFile));
    }

    @Test
    void testSave_validPngFile_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pattern.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        PhotoRef photoRef = storageService.save(file);

        assertTrue(photoRef.url().endsWith(".png"));
        assertTrue(photoRef.publicId().endsWith(".png"));
        assertTrue(Files.exists(tempUploadDir.resolve(photoRef.publicId())));
    }

    @Test
    void testSave_validWebpFile_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.webp", "image/webp", new byte[]{1, 2, 3}
        );

        PhotoRef photoRef = storageService.save(file);

        assertTrue(photoRef.url().endsWith(".webp"));
        assertTrue(photoRef.publicId().endsWith(".webp"));
        assertTrue(Files.exists(tempUploadDir.resolve(photoRef.publicId())));
    }

    @Test
    void testSave_emptyFile_throwsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageService.save(emptyFile)
        );
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void testSave_unsupportedContentType_throwsException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageService.save(pdfFile)
        );
        assertTrue(ex.getMessage().contains("Invalid file type"));
    }

    @Test
    void testSave_fileExceeding5MB_throwsException() {
        // 5 MB + 1 byte
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "giant.jpg", "image/jpeg", largeBytes
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageService.save(largeFile)
        );
        assertTrue(ex.getMessage().contains("5 MB limit"));
    }

    @Test
    void testDelete_existingFile_deletesAndReturnsTrue() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        PhotoRef photoRef = storageService.save(file);
        Path savedFile = tempUploadDir.resolve(photoRef.publicId());
        assertTrue(Files.exists(savedFile));

        boolean deleted = storageService.delete(photoRef.publicId());
        assertTrue(deleted);
        assertFalse(Files.exists(savedFile));
    }

    @Test
    void testDelete_withFullUrl_deletesAndReturnsTrue() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        PhotoRef photoRef = storageService.save(file);

        // Deleting using the URL "/uploads/uuid.jpg"
        boolean deleted = storageService.delete(photoRef.url());
        assertTrue(deleted);
        assertFalse(Files.exists(tempUploadDir.resolve(photoRef.publicId())));
    }

    @Test
    void testDelete_nonExistentFile_returnsFalse() {
        boolean deleted = storageService.delete("non-existent-uuid.jpg");
        assertFalse(deleted);
    }

    @Test
    void testDelete_directoryTraversalAttempt_returnsFalse() {
        boolean deleted = storageService.delete("../../etc/passwd");
        assertFalse(deleted);
    }
}
