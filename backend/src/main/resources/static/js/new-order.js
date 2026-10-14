/**
 * new-order.js: Client-side JavaScript for the Order Creation Wizard.
 *
 * Responsibilities:
 * 1. Live phone lookup: As the tailor types a 10-digit mobile number in Step 1,
 *    makes an asynchronous fetch request to /api/customers/lookup?mobile=...
 * 2. Auto-fills customer name and displays order history badges for existing clients.
 * 3. Highlights fields dynamically for new vs existing customers.
 */

document.addEventListener('DOMContentLoaded', () => {
    initPhoneLookup();
    initGarmentSelection();
    initNotesTapChips();
    initPhotoUpload();
});

/**
 * Initializes real-time customer mobile number lookup for Wizard Step 1.
 */
function initPhoneLookup() {
    const mobileInput = document.getElementById('customerMobile');
    const nameInput = document.getElementById('customerName');
    const customerIdInput = document.getElementById('customerId');
    const statusBadge = document.getElementById('lookupBadge');
    const historyInfo = document.getElementById('customerHistoryInfo');

    if (!mobileInput) {
        return; // Not on the customer step screen
    }

    let lookupDebounceTimer = null;
    let lastLookedUpMobile = '';

    const performLookup = async () => {
        const rawValue = mobileInput.value.trim();
        // Extract digits only
        const digits = rawValue.replace(/\D/g, '');

        // If fewer than 10 digits, reset status
        if (digits.length < 10) {
            if (statusBadge) {
                statusBadge.style.display = 'none';
                statusBadge.textContent = '';
            }
            if (historyInfo) {
                historyInfo.style.display = 'none';
            }
            return;
        }

        // Standardize to last 10 digits
        const normalized = digits.slice(-10);
        if (normalized === lastLookedUpMobile) {
            return; // Already checked this number
        }
        lastLookedUpMobile = normalized;

        if (statusBadge) {
            statusBadge.style.display = 'inline-flex';
            statusBadge.className = 'badge badge-searching';
            statusBadge.innerHTML = '<span class="material-symbols-rounded icon-sm">sync</span> Checking...';
        }

        try {
            const response = await fetch(`/api/customers/lookup?mobile=${encodeURIComponent(normalized)}`, {
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error ${response.status}`);
            }

            const data = await response.json();

            if (data.found) {
                // Existing customer found!
                if (statusBadge) {
                    statusBadge.style.display = 'inline-flex';
                    statusBadge.className = 'badge badge-success';
                    statusBadge.innerHTML = `<span class="material-symbols-rounded icon-sm">check_circle</span> Existing Customer (${data.orderCount} past orders)`;
                }

                // Auto-fill customer name if not already typed
                if (nameInput) {
                    nameInput.value = data.name;
                    nameInput.classList.add('input-autofilled');
                }

                // Show history snippet
                if (historyInfo) {
                    historyInfo.style.display = 'block';
                    const lastDateText = data.lastOrderDate ? data.lastOrderDate : 'None';
                    historyInfo.innerHTML = `<strong>${data.name}</strong> • Total Orders: <strong>${data.orderCount}</strong> • Last Order: <strong>${lastDateText}</strong>`;
                }

                // Provide visual confirmation
                mobileInput.classList.remove('input-warning');
                mobileInput.classList.add('input-success');
            } else {
                // New customer
                if (statusBadge) {
                    statusBadge.style.display = 'inline-flex';
                    statusBadge.className = 'badge badge-info';
                    statusBadge.innerHTML = '<span class="material-symbols-rounded icon-sm">person_add</span> New Customer';
                }

                if (historyInfo) {
                    historyInfo.style.display = 'none';
                }

                if (nameInput && nameInput.classList.contains('input-autofilled')) {
                    nameInput.value = '';
                    nameInput.classList.remove('input-autofilled');
                }

                if (customerIdInput) {
                    customerIdInput.value = '';
                }

                // Prompt user to enter customer name
                if (nameInput && !nameInput.value.trim()) {
                    nameInput.focus();
                }

                mobileInput.classList.remove('input-success');
            }
        } catch (error) {
            console.error('Customer lookup failed:', error);
            if (statusBadge) {
                statusBadge.style.display = 'none';
            }
        }
    };

    // Listen to typing events with 300ms debounce
    mobileInput.addEventListener('input', () => {
        clearTimeout(lookupDebounceTimer);
        lookupDebounceTimer = setTimeout(performLookup, 300);
    });

    mobileInput.addEventListener('blur', performLookup);

    // If page loaded with an existing value (e.g. returning to Step 1), trigger lookup
    if (mobileInput.value && mobileInput.value.trim().length >= 10) {
        performLookup();
    }
}

/**
 * Initializes interactive card selection in Wizard Step 2: Garment category selection.
 */
function initGarmentSelection() {
    const garmentCards = document.querySelectorAll('.garment-card');
    if (!garmentCards || garmentCards.length === 0) {
        return; // Not on garment selection screen
    }

    garmentCards.forEach(card => {
        card.addEventListener('click', () => {
            // Find radio input inside this card
            const radio = card.querySelector('input[type="radio"]');
            if (radio) {
                radio.checked = true;
            }

            // Remove selected class from all cards and add to clicked card
            garmentCards.forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
        });
    });
}

/**
 * Initializes quick-tap chips for custom styling notes in Wizard Step 3.
 */
function initNotesTapChips() {
    const chips = document.querySelectorAll('.tap-chip');
    const notesInput = document.getElementById('itemNotes');
    if (!chips || chips.length === 0 || !notesInput) {
        return;
    }

    chips.forEach(chip => {
        chip.addEventListener('click', (e) => {
            e.preventDefault();
            const textToAdd = chip.getAttribute('data-note') || chip.textContent.trim();
            const current = notesInput.value.trim();

            if (!current) {
                notesInput.value = textToAdd;
            } else {
                // If text already exists, append with comma
                notesInput.value = current + ', ' + textToAdd;
            }

            notesInput.focus();
        });
    });
}

/**
 * Initializes camera/file photo uploads for Wizard Step 3: Item details.
 *
 * Flow:
 * 1. "Add photo" button triggers hidden <input type="file"> with rear-camera preference (capture="environment").
 * 2. Canvas shrinks the picture to ~1280px wide JPEG to stay well under the 5 MB limit.
 * 3. Asynchronously POSTs the file with the Spring Security CSRF header to /api/photos.
 * 4. Appends a thumbnail with an X button and hidden inputs (photoUrls, photoPublicIds).
 * 5. Tapping X calls DELETE /api/photos?publicId=... to remove the file from storage and the DOM.
 */
function initPhotoUpload() {
    const addPhotoBtn = document.getElementById('addPhotoBtn');
    const photoFileInput = document.getElementById('photoFileInput');
    const thumbnailsContainer = document.getElementById('photoThumbnailsContainer');
    const uploadStatus = document.getElementById('photoUploadStatus');
    const uploadStatusText = document.getElementById('photoUploadStatusText');

    if (!addPhotoBtn || !photoFileInput || !thumbnailsContainer) {
        return; // Not on the item details screen
    }

    // Helper to get CSRF credentials from <head> meta tags
    const getCsrf = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
        return { token, header };
    };

    // Helper to show/hide status banner
    const showStatus = (text) => {
        if (uploadStatus && uploadStatusText) {
            uploadStatusText.textContent = text;
            uploadStatus.style.display = 'flex';
        }
    };
    const hideStatus = () => {
        if (uploadStatus) {
            uploadStatus.style.display = 'none';
        }
    };

    // Clicking the visible styled button triggers the hidden file picker/camera
    addPhotoBtn.addEventListener('click', () => {
        photoFileInput.click();
    });

    // Helper to shrink image to ~1280px wide JPEG using browser HTML5 canvas
    const shrinkImage = (file, maxWidth = 1280, quality = 0.85) => {
        return new Promise((resolve) => {
            if (!file.type || !file.type.startsWith('image/')) {
                // If not standard image or unable to read, resolve with original file
                resolve(file);
                return;
            }

            const reader = new FileReader();
            reader.onload = (e) => {
                const img = new Image();
                img.onload = () => {
                    let width = img.width;
                    let height = img.height;

                    // If larger than maxWidth, scale down proportionally
                    if (width > maxWidth) {
                        height = Math.round((height * maxWidth) / width);
                        width = maxWidth;
                    }

                    const canvas = document.createElement('canvas');
                    canvas.width = width;
                    canvas.height = height;

                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);

                    canvas.toBlob((blob) => {
                        if (blob) {
                            resolve(blob);
                        } else {
                            // Fallback to original file if blob creation fails
                            resolve(file);
                        }
                    }, 'image/jpeg', quality);
                };
                img.onerror = () => resolve(file); // Fallback on image parse error
                img.src = e.target.result;
            };
            reader.onerror = () => resolve(file);
            reader.readAsDataURL(file);
        });
    };

    // Helper to wire up deletion on any thumbnail X button
    const attachRemoveHandler = (button, cardElement) => {
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            const publicId = button.getAttribute('data-public-id') || button.getAttribute('data-url');
            if (!publicId) {
                cardElement.remove();
                return;
            }

            button.disabled = true;
            cardElement.style.opacity = '0.4';

            try {
                const { token, header } = getCsrf();
                const headers = {};
                if (token) {
                    headers[header] = token;
                }

                const response = await fetch(`/api/photos?publicId=${encodeURIComponent(publicId)}`, {
                    method: 'DELETE',
                    headers: headers
                });

                if (!response.ok) {
                    throw new Error(`Delete failed with HTTP status ${response.status}`);
                }

                // Smoothly remove card from page
                cardElement.remove();
            } catch (err) {
                console.error('Failed to delete photo:', err);
                alert('Could not remove photo. Please try again.');
                button.disabled = false;
                cardElement.style.opacity = '1';
            }
        });
    };

    // Wire up existing thumbnails (if re-rendered after validation failure)
    const existingButtons = thumbnailsContainer.querySelectorAll('.btn-remove-photo');
    existingButtons.forEach(btn => {
        const card = btn.closest('.photo-thumbnail-card');
        if (card) {
            attachRemoveHandler(btn, card);
        }
    });

    // Handle user camera capture or file selection
    photoFileInput.addEventListener('change', async () => {
        const file = photoFileInput.files && photoFileInput.files[0];
        if (!file) {
            return;
        }

        try {
            showStatus('Optimizing image size (scaling to ~1280px)...');
            const processedBlob = await shrinkImage(file);

            showStatus('Uploading photo to server...');
            const formData = new FormData();
            formData.append('file', processedBlob, file.name || 'photo.jpg');

            const { token, header } = getCsrf();
            const headers = {};
            if (token) {
                headers[header] = token;
            }

            const response = await fetch('/api/photos', {
                method: 'POST',
                headers: headers,
                body: formData
            });

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                throw new Error(errData.error || `Upload failed with HTTP ${response.status}`);
            }

            const result = await response.json(); // { url: "/uploads/....jpg", publicId: "....jpg" }

            // Create thumbnail card in the DOM
            const card = document.createElement('div');
            card.className = 'photo-thumbnail-card';
            card.setAttribute('data-url', result.url);
            card.innerHTML = `
                <img src="${result.url}" alt="Fabric Photo" class="photo-thumbnail-img">
                <button type="button"
                        class="btn-remove-photo"
                        data-url="${result.url}"
                        data-public-id="${result.publicId}"
                        title="Remove Photo">&times;</button>
                <input type="hidden" name="photoUrls" value="${result.url}">
                <input type="hidden" name="photoPublicIds" value="${result.publicId}">
            `;

            thumbnailsContainer.appendChild(card);
            attachRemoveHandler(card.querySelector('.btn-remove-photo'), card);

        } catch (err) {
            console.error('Photo upload error:', err);
            alert(`Photo upload failed: ${err.message}`);
        } finally {
            hideStatus();
            photoFileInput.value = ''; // Reset input to allow re-uploading the same file if needed
        }
    });
}
