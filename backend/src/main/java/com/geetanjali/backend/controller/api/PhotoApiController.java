package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.dto.response.PhotoRef;
import com.geetanjali.backend.service.storage.PhotoStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

// PhotoApiController: REST API controller handling AJAX file uploads and deletions for tailor photos.
// In Express, this corresponds to:
//   app.post('/api/photos', upload.single('file'), (req, res) => { ... });
//   app.delete('/api/photos', (req, res) => { ... });
//
// @RestController = Combines @Controller and @ResponseBody, serializing returned Java objects directly to JSON.
@RestController
// @RequestMapping = Base URL prefix for photo API endpoints ("/api/photos").
@RequestMapping("/api/photos")
public class PhotoApiController {

    private final PhotoStorageService photoStorageService;

    // Constructor injection: Spring injects the PhotoStorageService bean (LocalDiskPhotoStorageService).
    // Input: photoStorageService (PhotoStorageService).
    public PhotoApiController(PhotoStorageService photoStorageService) {
        this.photoStorageService = photoStorageService;
    }

    // Handles POST /api/photos: Receives multipart photo file, validates, saves to storage, and returns JSON { url, publicId }.
    //
    // Express comparison: `app.post('/api/photos', upload.single('file'), (req, res) => res.json(photoRef));`.
    //
    // Inputs:
    //   file (@RequestParam("file") MultipartFile) - uploaded image payload.
    // Output: ResponseEntity<?> - 200 OK with PhotoRef on success, or 400 Bad Request with error description.
    //
    // @PostMapping = Maps HTTP POST requests to "/api/photos".
    @PostMapping
    public ResponseEntity<?> uploadPhoto(
            // @RequestParam = Extracts multipart file from the HTTP request body named "file".
            @RequestParam("file") MultipartFile file) {
        try {
            PhotoRef photoRef = photoStorageService.save(file);
            return ResponseEntity.ok(photoRef);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload photo: " + e.getMessage()));
        }
    }

    // Handles DELETE /api/photos: Deletes an uploaded photo from disk/storage before the order is submitted.
    // Accepts either ?publicId=... or ?id=... query parameters.
    //
    // Express comparison: `app.delete('/api/photos', (req, res) => res.json({ success: true, deleted }));`.
    //
    // Inputs:
    //   publicId (@RequestParam(value = "publicId", required = false) String) - public storage key or filename.
    //   id (@RequestParam(value = "id", required = false) String) - fallback parameter name.
    // Output: ResponseEntity<?> - 200 OK with { "success": true, "deleted": boolean }.
    //
    // @DeleteMapping = Maps HTTP DELETE requests to "/api/photos".
    @DeleteMapping
    public ResponseEntity<?> deletePhoto(
            @RequestParam(value = "publicId", required = false) String publicId,
            @RequestParam(value = "id", required = false) String id) {

        String targetId = (publicId != null && !publicId.isBlank()) ? publicId : id;
        if (targetId == null || targetId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Photo identifier (publicId or id) is required"));
        }

        boolean deleted = photoStorageService.delete(targetId);
        return ResponseEntity.ok(Map.of("success", true, "deleted", deleted));
    }
}
