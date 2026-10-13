package com.geetanjali.backend.service.storage;

import org.springframework.web.multipart.MultipartFile;

// PhotoStorageService: Interface abstracting photo storage operations.
// WHY: In the hackathon, we save photos to local disk. Later in production,
// this interface allows us to swap in Cloudinary or AWS S3 without modifying any controller or business logic.
public interface PhotoStorageService {

    // Saves an uploaded photo file and returns the web-accessible URL.
    // Inputs: file (MultipartFile) - the uploaded image from the browser form.
    // Output: String - public URL path to display the image (e.g. "/uploads/abc.jpg").
    String store(MultipartFile file);

    // Deletes an image from storage using its identifier or URL.
    // Input: identifier (String) - file name, public ID, or URL path.
    // Output: None (void).
    void delete(String identifier);
}
