package com.geetanjali.backend.dto.form;

import com.geetanjali.backend.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// SignupForm: Form backing object (DTO) for capturing user signup and staff registration input.
// In Express, this is the typed request body `req.body` payload validated with Joi / Zod.
public class SignupForm {

    // Tailor shop name (required for first-run setup)
    private String shopName;

    // Full name of the user / staff member
    // @NotBlank = Rejects null, empty strings, and whitespace-only strings.
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    // Mobile number used as unique login identifier
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    // Plain text password to be hashed with BCrypt
    @NotBlank(message = "Password is required")
    @Size(min = 4, max = 50, message = "Password must be at least 4 characters")
    private String password;

    // Selected role (OWNER or TAILOR). Defaults to OWNER during first-time shop setup.
    private Role role = Role.OWNER;

    // Default zero-argument constructor: Spring MVC needs this for form data binding.
    public SignupForm() {
    }

    // Convenience constructor.
    public SignupForm(String shopName, String fullName, String mobile, String password, Role role) {
        this.shopName = shopName;
        this.fullName = fullName;
        this.mobile = mobile;
        this.password = password;
        this.role = role;
    }

    // Getter for shopName.
    // Input: None.
    // Output: String.
    public String getShopName() {
        return shopName;
    }

    // Setter for shopName.
    // Input: String shopName.
    // Output: None (void).
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    // Getter for fullName.
    // Input: None.
    // Output: String.
    public String getFullName() {
        return fullName;
    }

    // Setter for fullName.
    // Input: String fullName.
    // Output: None (void).
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // Getter for mobile.
    // Input: None.
    // Output: String.
    public String getMobile() {
        return mobile;
    }

    // Setter for mobile.
    // Input: String mobile.
    // Output: None (void).
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    // Getter for password.
    // Input: None.
    // Output: String.
    public String getPassword() {
        return password;
    }

    // Setter for password.
    // Input: String password.
    // Output: None (void).
    public void setPassword(String password) {
        this.password = password;
    }

    // Getter for role.
    // Input: None.
    // Output: Role.
    public Role getRole() {
        return role;
    }

    // Setter for role.
    // Input: Role role.
    // Output: None (void).
    public void setRole(Role role) {
        this.role = role;
    }
}
