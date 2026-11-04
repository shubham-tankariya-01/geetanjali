// calendar.js: Interactive controller for production delivery scheduler
document.addEventListener('DOMContentLoaded', function() {
    const dayBoxes = document.querySelectorAll('.calendar-day-box');
    const selectedDayTitle = document.getElementById('selectedDayTitle');
    const selectedDayBadge = document.getElementById('selectedDayBadge');
    const selectedDayOrdersList = document.getElementById('selectedDayOrdersList');
    const topDayItems = document.querySelectorAll('.top-day-item');

    let currentSelectedDate = null;

    // Helper to format ISO date "YYYY-MM-DD" into human-friendly string
    function formatHumanDate(dateStr) {
        if (!dateStr) return '';
        const parts = dateStr.split('-');
        if (parts.length !== 3) return dateStr;
        const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        return d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
    }

    // Helper to extract initials from customer name (e.g. "Ketan Tankariya" -> "KT")
    function getInitials(name) {
        if (!name) return 'C';
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    // Selects a day and updates the sidebar details panel
    function selectDay(dayBox) {
        if (!dayBox) return;

        // Deselect previous
        dayBoxes.forEach(box => box.classList.remove('is-selected'));
        dayBox.classList.add('is-selected');

        const dateStr = dayBox.getAttribute('data-date');
        currentSelectedDate = dateStr;

        const orderCount = parseInt(dayBox.getAttribute('data-order-count') || '0', 10);
        const color = dayBox.getAttribute('data-color') || 'NONE';

        // Update title
        if (selectedDayTitle) {
            selectedDayTitle.textContent = formatHumanDate(dateStr);
        }

        // Update badge (clean numbers, NO wordy explanations)
        if (selectedDayBadge) {
            selectedDayBadge.className = 'top-day-tag';
            if (color === 'RED') {
                selectedDayBadge.style.background = '#FEF2F2';
                selectedDayBadge.style.color = '#991B1B';
                selectedDayBadge.style.border = '1px solid #FECACA';
                selectedDayBadge.textContent = orderCount + ' orders';
            } else if (color === 'YELLOW') {
                selectedDayBadge.style.background = '#FFFBEB';
                selectedDayBadge.style.color = '#92400E';
                selectedDayBadge.style.border = '1px solid #FDE68A';
                selectedDayBadge.textContent = orderCount + ' orders';
            } else if (color === 'GREEN') {
                selectedDayBadge.style.background = '#ECFDF5';
                selectedDayBadge.style.color = '#065F46';
                selectedDayBadge.style.border = '1px solid #A7F3D0';
                selectedDayBadge.textContent = orderCount + (orderCount === 1 ? ' order' : ' orders');
            } else {
                selectedDayBadge.style.background = '#F1F5F9';
                selectedDayBadge.style.color = '#64748B';
                selectedDayBadge.style.border = '1px solid #E2E8F0';
                selectedDayBadge.textContent = '0 orders';
            }
        }

        // Parse items JSON embedded in data attribute
        const itemsJson = dayBox.getAttribute('data-items-json');
        let items = [];
        try {
            if (itemsJson) {
                items = JSON.parse(itemsJson);
            }
        } catch (e) {
            console.warn('Failed to parse day items JSON:', e);
        }

        renderDayOrders(items, dateStr);
    }

    function renderDayOrders(items, dateStr) {
        if (!selectedDayOrdersList) return;

        if (!items || items.length === 0) {
            selectedDayOrdersList.innerHTML = `
                <div class="day-empty-state">
                    <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="1.6" style="margin-bottom: 8px;">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    <p style="margin: 0 0 8px 0; color: #475569; font-weight: 600;">No deliveries scheduled</p>
                    <span style="font-size: 12px; color: #94A3B8;">Workshop capacity is fully available for this date.</span>
                </div>
            `;
            return;
        }

        let html = '';
        items.forEach(item => {
            const statusClass = (item.status === 'DELIVERED') ? 'badge-delivered' :
                                (item.status === 'DONE') ? 'badge-ready' : 'badge-in-progress';

            const balance = (item.orderBalance !== null && item.orderBalance !== undefined) ? Number(item.orderBalance).toFixed(2) : '0.00';
            const balanceText = (Number(balance) > 0) ? `<span style="color: #DC2626; font-weight: 600;">₹${balance} due</span>` : `<span style="color: #047857; font-weight: 600;">Paid</span>`;
            const initials = getInitials(item.customerName);

            html += `
                <div class="day-order-item">
                    <div class="day-order-row-top">
                        <a href="/orders/${item.orderId}" class="day-order-link" title="View order details">
                            Order #${item.orderNo}
                        </a>
                        <span class="status-badge ${statusClass}" style="font-size: 10.5px; padding: 2px 7px;">
                            ${item.status}
                        </span>
                    </div>
                    <div class="day-order-customer-row">
                        <div class="day-order-avatar">${initials}</div>
                        <div>
                            <span class="day-order-customer-name">${escapeHtml(item.customerName || 'Customer')}</span>
                            ${item.customerMobile ? `<span class="day-order-customer-phone">(${escapeHtml(item.customerMobile)})</span>` : ''}
                        </div>
                    </div>
                    <div class="day-order-row-bottom">
                        <span>${escapeHtml(item.garmentName || 'Garment')} &bull; ₹${Number(item.price || 0).toFixed(2)}</span>
                        <span>${balanceText}</span>
                    </div>
                </div>
            `;
        });

        selectedDayOrdersList.innerHTML = html;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // Bind click events on day boxes
    dayBoxes.forEach(box => {
        box.addEventListener('click', () => {
            selectDay(box);
        });
    });

    // Bind click on top delivery days sidebar items
    topDayItems.forEach(item => {
        item.addEventListener('click', () => {
            const dateStr = item.getAttribute('data-date');
            const targetBox = document.querySelector(`.calendar-day-box[data-date="${dateStr}"]`);
            if (targetBox) {
                selectDay(targetBox);
                targetBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        });
    });

    // Auto-select "Today" or the first day with orders
    const todayBox = document.querySelector('.calendar-day-box.is-today');
    if (todayBox) {
        selectDay(todayBox);
    } else {
        const firstOrderBox = document.querySelector('.calendar-day-box:not(.is-other-month)[data-order-count]:not([data-order-count="0"])');
        if (firstOrderBox) {
            selectDay(firstOrderBox);
        } else if (dayBoxes.length > 0) {
            selectDay(dayBoxes[0]);
        }
    }
});
