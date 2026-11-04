// app.js - Client-side script for Geetanjali Tailor Shop

/**
 * Helper to get the CSRF token stored in the page meta tags.
 * In Spring Security with CSRF enabled, AJAX / fetch requests (POST/PUT/DELETE)
 * must send this token in the HTTP header specified by getCsrfHeader().
 *
 * Express / csurf comparison:
 * Similar to reading <meta name="csrf-token"> or the XSRF-TOKEN cookie in Express.
 *
 * @returns {string} The CSRF token string, or empty string if not found.
 */
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : '';
}

/**
 * Helper to get the CSRF header name (typically 'X-CSRF-TOKEN').
 *
 * @returns {string} The header name for CSRF authentication.
 */
function getCsrfHeader() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
}

// Attach helper functions to window for global access in page-specific scripts
window.getCsrfToken = getCsrfToken;
window.getCsrfHeader = getCsrfHeader;

document.addEventListener('DOMContentLoaded', () => {
    // Auto-dismiss alert banners after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });
});
