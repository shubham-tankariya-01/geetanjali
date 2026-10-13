package com.geetanjali.backend.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhoneNumbersTest {

    @Test
    void testNormalize() {
        assertEquals("9876543210", PhoneNumbers.normalize("9876543210"));
        assertEquals("9876543210", PhoneNumbers.normalize("+91-98765-43210"));
        assertEquals("9876543210", PhoneNumbers.normalize("+91 98765 43210"));
        assertEquals("9876543210", PhoneNumbers.normalize("09876543210"));
        assertEquals("9876543210", PhoneNumbers.normalize("98765-43210"));
        assertEquals("9876543210", PhoneNumbers.normalize("+9109876543210"));
        assertEquals("9876543210", PhoneNumbers.normalize(" 9876543210 "));
        assertEquals("", PhoneNumbers.normalize(null));
        assertEquals("", PhoneNumbers.normalize("   "));
        // When more than 10 digits provided, keeps last 10
        assertEquals("9876543210", PhoneNumbers.normalize("129876543210"));
    }

    @Test
    void testIsValid() {
        assertTrue(PhoneNumbers.isValid("9876543210"));
        assertTrue(PhoneNumbers.isValid("8123456789"));
        assertTrue(PhoneNumbers.isValid("7123456789"));
        assertTrue(PhoneNumbers.isValid("6123456789"));

        // Invalid: starts with 5 or below
        assertFalse(PhoneNumbers.isValid("5123456789"));
        assertFalse(PhoneNumbers.isValid("1123456789"));
        // Invalid: not 10 digits
        assertFalse(PhoneNumbers.isValid("98765"));
        assertFalse(PhoneNumbers.isValid("98765432101"));
        // Invalid: non-numeric
        assertFalse(PhoneNumbers.isValid("987654321a"));
        assertFalse(PhoneNumbers.isValid(null));
        assertFalse(PhoneNumbers.isValid(""));
    }
}
