package com.geetanjali.backend.service.storage;

import com.geetanjali.backend.dto.response.PhotoRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

// LocalDiskPhotoStorageService: Implementation of PhotoStorageService that stores files on local server disk.
// Files are saved inside the folder specified by app.upload-dir (default: "uploads").
// In Express, this corresponds to using `multer.diskStorage()` to write uploaded photos to `./uploads/`.
//
// WHY random UUID filenames:
// 1. Prevents Path Traversal attacks (e.g. someone uploading "../../etc/passwd" or malicious script names).
// 2. Prevents collisions when multiple customers upload files with the same camera name (e.g. "IMG_0001.jpg").
// 3. Obfuscates filenames so unauthorized visitors cannot guess or enumerate other customer photos.
//
// Honest limitations (Hackathon scope):
// 1. Abandoned order drafts leave orphan files on disk.
// 2. Anyone with the exact unguessable URL can view the photo.
// 3. Local disk storage is ephemeral on containerized/serverless platforms, making this ideal for dev/demo.
//
// @Service = Tells Spring to instantiate and manage this class as a Spring Bean.
@Service
public class LocalDiskPhotoStorageService implements PhotoStorageService {

    // Maximum permitted file upload size: 5 MB (5 * 1024 * 1024 bytes)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Set of permitted image MIME types. Everything else (PDF, EXE, HTML, SVG) is strictly rejected!
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    // Configured upload folder path from application.properties ("uploads").
    private final Path rootLocation;

    // Constructor that initializes the upload folder on the server filesystem.
    // Inputs: uploadDir (String) injected from app.upload-dir property.
    public LocalDiskPhotoStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            // Create uploads directory if it does not already exist
            if (!Files.exists(this.rootLocation)) {
                Files.createDirectories(this.rootLocation);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not create directory for uploads: " + this.rootLocation, e);
        }
    }

    // Saves an uploaded file to local disk under a random UUID name after validating size, type, and path.
    // Inputs: file (MultipartFile) - uploaded file data.
    // Output: PhotoRef - record containing web URL (e.g. "/uploads/uuid.jpg") and public ID ("uuid.jpg").
    @Override
    public PhotoRef save(MultipartFile file) {
        // Step 1: Reject empty files
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file.");
        }

        // Step 2: Enforce 5 MB maximum file size limit
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Photo file size exceeds the 5 MB limit");
        }

        // Step 3: Validate content type (strictly jpeg, png, webp)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and WebP images are allowed");
        }

        // Step 4: Determine safe file extension derived from the validated MIME type
        String extension = determineExtension(contentType.toLowerCase());

        // Step 5: Generate a random UUID filename (never trust or use the user's original filename!)
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize();

        // Step 6: Security check - ensure file path does not escape the upload directory (path traversal attack)
        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside upload directory.");
        }

        // Step 7: Stream file bytes onto local disk
        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on disk", e);
        }

        // Return relative URL path served by WebMvcConfig and publicId
        String webUrl = "/uploads/" + uniqueFilename;
        return new PhotoRef(webUrl, uniqueFilename);
    }

    // Deletes a photo file from local disk.
    // Inputs: identifier (String) - the URL or filename (e.g. "uuid.jpg" or "/uploads/uuid.jpg").
    // Output: boolean - true if file was found and deleted, false otherwise.
    @Override
    public boolean delete(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }

        // Strip leading "/uploads/" or path separators if present
        String cleanedName = identifier.trim();
        if (cleanedName.contains("/")) {
            cleanedName = cleanedName.substring(cleanedName.lastIndexOf('/') + 1);
        }

        Path file = this.rootLocation.resolve(cleanedName).normalize();
        // Security check: ensure path does not escape rootLocation
        if (!file.startsWith(this.rootLocation)) {
            return false;
        }

        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            return false;
        }
    }

    // Helper: Maps validated MIME type to safe file extension.
    // Inputs: contentType (String).
    // Output: String (extension including dot, e.g. ".jpg").
    private String determineExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    // Getter for rootLocation Path (helpful for unit testing).
    // Output: Path - root directory on disk.
    public Path getRootLocation() {
        return this.rootLocation;
    }
}
