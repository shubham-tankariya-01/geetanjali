package com.geetanjali.backend.service.storage;

import com.geetanjali.backend.dto.response.PhotoRef;
import org.springframework.web.multipart.MultipartFile;

// PhotoStorageService: Interface abstracting photo storage operations.
// WHY: In the hackathon, we save photos to local disk via LocalDiskPhotoStorageService.
// Later in production, this interface allows us to swap in CloudinaryPhotoStorageService or AWS S3
// without modifying any controller or business logic.
public interface PhotoStorageService {

    // Saves an uploaded photo file and returns the PhotoRef (URL and public ID).
    // Validates file content type (JPEG, PNG, WebP) and size (<= 5 MB).
    // Inputs: file (MultipartFile) - the uploaded image from the camera or file picker.
    // Output: PhotoRef - record containing web URL (e.g. "/uploads/abc.jpg") and public ID ("abc.jpg").
    PhotoRef save(MultipartFile file);

    // Legacy/convenience store method returning just the web URL string.
    default String store(MultipartFile file) {
        return save(file).url();
    }

    // Deletes an image from storage using its unique public identifier or URL.
    // Input: identifier (String) - file name, public ID, or URL path.
    // Output: boolean - true if file existed and was deleted, false otherwise.
    boolean delete(String identifier);
}
