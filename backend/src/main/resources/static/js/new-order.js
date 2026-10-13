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
            statusBadge.style.display = 'inline-block';
            statusBadge.className = 'badge badge-searching';
            statusBadge.textContent = '🔍 Checking...';
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
                    statusBadge.className = 'badge badge-success';
                    statusBadge.textContent = `✓ Existing Customer (${data.orderCount} past orders)`;
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
                    statusBadge.className = 'badge badge-info';
                    statusBadge.textContent = '➕ New Customer';
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
