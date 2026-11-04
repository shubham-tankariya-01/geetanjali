package com.geetanjali.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

// WebMvcConfig: Customizes Spring MVC web behaviors, including serving static uploads from disk.
// In Express, this is like `app.use('/uploads', express.static(path.join(__dirname, 'uploads')))`.
// @Configuration = Tells Spring this is a configuration component.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Directory path on disk where uploaded photos are saved.
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // Configures Spring MVC to serve files stored in the local uploads directory when URLs starting with /uploads/ are requested.
    // Input: ResourceHandlerRegistry registry.
    // Output: None (void).
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}
