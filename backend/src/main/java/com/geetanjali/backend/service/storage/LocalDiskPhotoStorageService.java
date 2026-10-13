package com.geetanjali.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// LocalDiskPhotoStorageService: Implementation of PhotoStorageService that stores files on local disk.
// In Express, this is like using `multer.diskStorage()` to save uploaded images into a `./uploads/` folder.
// @Service = Tells Spring to instantiate and manage this class as a Spring Bean.
@Service
public class LocalDiskPhotoStorageService implements PhotoStorageService {

    // Configured upload folder path from application.properties ("uploads").
    private final Path rootLocation;

    // Constructor that initializes the upload folder on the server filesystem.
    // Input: uploadDir (String) injected from app.upload-dir property.
    public LocalDiskPhotoStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            // Create uploads directory if it does not already exist
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory for uploads: " + this.rootLocation, e);
        }
    }

    // Stores an uploaded file to local disk under a random UUID name to prevent file collisions.
    // Input: file (MultipartFile) - uploaded file data.
    // Output: String - public URL path (e.g. "/uploads/123e4567.jpg").
    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file.");
        }

        try {
            // Extract original file extension (e.g. ".jpg")
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate unique filename using UUID
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize();

            // Security check: ensure file path does not escape the upload directory (path traversal attack)
            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new SecurityException("Cannot store file outside upload directory.");
            }

            // Copy file stream to destination path
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return relative URL path served by WebMvcConfig
            return "/uploads/" + uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on disk", e);
        }
    }

    // Deletes a photo file from local disk.
    // Input: identifier (String) - the URL or filename.
    // Output: None (void).
    @Override
    public void delete(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }

        try {
            // Strip leading "/uploads/" if present
            String filename = identifier;
            if (filename.startsWith("/uploads/")) {
                filename = filename.substring("/uploads/".length());
            }

            Path file = this.rootLocation.resolve(filename).normalize();
            // Delete if file exists
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Non-critical logging in hackathon
            System.err.println("Failed to delete file: " + identifier + ". Reason: " + e.getMessage());
        }
    }
}
