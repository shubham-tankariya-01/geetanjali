package com.geetanjali.backend.util;

// PhoneNumbers: Utility class for normalizing and validating Indian mobile phone numbers.
// In tailor shops across India, customers and staff type phone numbers in various formats:
// "98765 43210", "+91-9876543210", "09876543210", "98765-43210", etc.
// This utility ensures all phone numbers are cleaned into standard 10-digit formats before saving to PostgreSQL.
public final class PhoneNumbers {

    // Private constructor prevents instantiation because this is a static utility class.
    private PhoneNumbers() {
    }

    // Normalizes raw phone number input into a clean 10-digit number.
    // 1. Removes all whitespace and dashes.
    // 2. Strips "+91" or leading "91" (country code).
    // 3. Strips a leading "0" (trunk prefix).
    // 4. Keeps only numeric digits and retains the last 10 digits if longer.
    // Input: raw (String) - raw input from a user or form.
    // Output: String - normalized 10-digit string, or empty string if input was null/blank.
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        // 1. Remove all spaces and dashes
        String cleaned = raw.replaceAll("[\\s\\-]", "");

        // 2. Remove leading "+91"
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        }

        // 3. Remove leading "0"
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }

        // 4. Keep only numeric digits
        cleaned = cleaned.replaceAll("[^0-9]", "");

        // 5. Keep the last 10 digits if more digits were provided
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(cleaned.length() - 10);
        }

        return cleaned;
    }

    // Checks whether the given phone number is a valid 10-digit Indian mobile number.
    // Rule: Exactly 10 digits, starting with 6, 7, 8, or 9 (Indian telecom numbering plan).
    // Input: phone (String) - the phone number to validate (preferably normalized).
    // Output: boolean - true if valid, false otherwise.
    public static boolean isValid(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^[6-9][0-9]{9}$");
    }
}
