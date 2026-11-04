package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.dto.response.PhotoRef;
import com.geetanjali.backend.service.storage.PhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PhotoApiControllerTest: Unit tests for PhotoApiController endpoints.
@ExtendWith(MockitoExtension.class)
class PhotoApiControllerTest {

    @Mock
    private PhotoStorageService photoStorageService;

    private PhotoApiController controller;

    @BeforeEach
    void setUp() {
        controller = new PhotoApiController(photoStorageService);
    }

    @Test
    void testUploadPhoto_validFile_returnsOkWithPhotoRef() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fabric.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        PhotoRef expectedRef = new PhotoRef("/uploads/test-uuid.jpg", "test-uuid.jpg");
        when(photoStorageService.save(any())).thenReturn(expectedRef);

        ResponseEntity<?> response = controller.uploadPhoto(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedRef, response.getBody());
        verify(photoStorageService).save(file);
    }

    @Test
    void testUploadPhoto_invalidFile_returnsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.txt", "text/plain", new byte[]{1, 2, 3}
        );
        when(photoStorageService.save(any())).thenThrow(new IllegalArgumentException("Invalid file type"));

        ResponseEntity<?> response = controller.uploadPhoto(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Invalid file type", body.get("error"));
    }

    @Test
    void testDeletePhoto_withPublicId_returnsSuccess() {
        when(photoStorageService.delete("test-uuid.jpg")).thenReturn(true);

        ResponseEntity<?> response = controller.deletePhoto("test-uuid.jpg", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(true, body.get("deleted"));
        verify(photoStorageService).delete("test-uuid.jpg");
    }

    @Test
    void testDeletePhoto_withFallbackId_returnsSuccess() {
        when(photoStorageService.delete("fallback-uuid.jpg")).thenReturn(true);

        ResponseEntity<?> response = controller.deletePhoto(null, "fallback-uuid.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(true, body.get("deleted"));
        verify(photoStorageService).delete("fallback-uuid.jpg");
    }

    @Test
    void testDeletePhoto_missingIdentifier_returnsBadRequest() {
        ResponseEntity<?> response = controller.deletePhoto(null, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertTrue(((String) body.get("error")).contains("required"));
    }
}
