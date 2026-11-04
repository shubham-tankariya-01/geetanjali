/**
 * assistant.js - Client Controller for Floating AI Assistant
 * Geetanjali Tailoring Management System
 */
(function() {
    'use strict';

    // DOM Elements
    let launcherBtn = null;
    let drawer = null;
    let backdrop = null;
    let closeBtn = null;
    let clearBtn = null;
    let expandBtn = null;
    let form = null;
    let input = null;
    let messagesContainer = null;
    let typingIndicator = null;
    let contextRibbon = null;
    let chipsContainer = null;

    // State
    let isOpen = false;
    let isExpanded = false;
    let pageContext = null;

    function init() {
        launcherBtn = document.getElementById('assistantLauncherBtn');
        drawer = document.getElementById('assistantDrawer');
        backdrop = document.getElementById('assistantBackdrop');
        closeBtn = document.getElementById('assistantCloseBtn');
        clearBtn = document.getElementById('assistantClearBtn');
        expandBtn = document.getElementById('assistantExpandBtn');
        form = document.getElementById('assistantForm');
        input = document.getElementById('assistantInput');
        messagesContainer = document.getElementById('assistantMessages');
        typingIndicator = document.getElementById('assistantTypingIndicator');
        contextRibbon = document.getElementById('assistantContextRibbon');
        chipsContainer = document.getElementById('assistantChipsBar');

        if (!launcherBtn || !drawer) {
            return;
        }

        // Restore wide drawer preference if previously toggled
        try {
            if (localStorage.getItem('geetanjali_assistant_wide') === 'true') {
                setExpandedState(true);
            }
        } catch (e) {}

        pageContext = detectPageContext();
        updateContextRibbonUI();

        // Event Listeners
        launcherBtn.addEventListener('click', toggleDrawer);
        if (closeBtn) closeBtn.addEventListener('click', closeDrawer);
        if (backdrop) backdrop.addEventListener('click', closeDrawer);
        if (clearBtn) clearBtn.addEventListener('click', handleClearHistory);
        if (expandBtn) expandBtn.addEventListener('click', toggleExpand);
        if (form) form.addEventListener('submit', handleSubmit);

        // Chip button listeners
        if (chipsContainer) {
            chipsContainer.addEventListener('click', function(e) {
                const chip = e.target.closest('.assistant-chip-btn');
                if (chip && chip.dataset.prompt) {
                    sendQuery(chip.dataset.prompt);
                }
            });
        }

        // Global Keyboard Shortcut: Ctrl + Space or Cmd + Space
        document.addEventListener('keydown', function(e) {
            if ((e.ctrlKey || e.metaKey) && e.code === 'Space') {
                e.preventDefault();
                toggleDrawer();
            } else if (e.key === 'Escape' && isOpen) {
                closeDrawer();
            }
        });
    }

    function detectPageContext() {
        const path = window.location.pathname;
        const context = {
            currentPath: path,
            entityType: 'DASHBOARD',
            entityId: null,
            orderNo: null
        };

        const orderDetailMatch = path.match(/\/orders\/(\d+)/);
        if (orderDetailMatch) {
            context.entityType = 'ORDER';
            context.entityId = parseInt(orderDetailMatch[1], 10);
            const heading = document.querySelector('h1.page-title');
            if (heading) {
                const noMatch = heading.textContent.match(/(\d{4,5})/);
                if (noMatch) {
                    context.orderNo = parseInt(noMatch[1], 10);
                }
            }
        } else if (path.startsWith('/orders/new')) {
            context.entityType = 'ORDER_WIZARD';
        } else if (path.startsWith('/orders')) {
            context.entityType = 'ORDERS_LIST';
        } else if (path.startsWith('/customers/')) {
            const custMatch = path.match(/\/customers\/(\d+)/);
            if (custMatch) {
                context.entityType = 'CUSTOMER';
                context.entityId = parseInt(custMatch[1], 10);
            }
        } else if (path.startsWith('/customers')) {
            context.entityType = 'CUSTOMER_DIRECTORY';
        } else if (path.startsWith('/payments')) {
            context.entityType = 'PAYMENTS';
        } else if (path.startsWith('/settings') || path.startsWith('/more')) {
            context.entityType = 'SETTINGS';
        }

        return context;
    }

    function updateContextRibbonUI() {
        if (!contextRibbon) return;
        const tag = contextRibbon.querySelector('.context-tag');
        const desc = contextRibbon.querySelector('.context-desc');

        if (tag && desc && pageContext) {
            if (pageContext.entityType === 'ORDER') {
                tag.textContent = 'ORDER';
                desc.textContent = pageContext.orderNo ? 'Viewing Order #' + pageContext.orderNo : 'Viewing Order Details';
                contextRibbon.style.display = 'flex';
            } else if (pageContext.entityType === 'CUSTOMER') {
                tag.textContent = 'CUSTOMER';
                desc.textContent = 'Viewing Customer Profile';
                contextRibbon.style.display = 'flex';
            } else if (pageContext.entityType === 'PAYMENTS') {
                tag.textContent = 'FINANCE';
                desc.textContent = 'Viewing Payments Ledger';
                contextRibbon.style.display = 'flex';
            } else {
                tag.textContent = 'STUDIO';
                desc.textContent = 'Operational Context Active';
                contextRibbon.style.display = 'flex';
            }
        }
    }

    function toggleDrawer() {
        if (isOpen) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    function openDrawer() {
        isOpen = true;
        drawer.classList.add('is-open');
        if (backdrop) backdrop.classList.add('is-active');
        if (input) {
            setTimeout(() => input.focus(), 250);
        }
        scrollToBottom();
    }

    function closeDrawer() {
        isOpen = false;
        drawer.classList.remove('is-open');
        if (backdrop) backdrop.classList.remove('is-active');
    }

    function toggleExpand() {
        setExpandedState(!isExpanded);
        try {
            localStorage.setItem('geetanjali_assistant_wide', isExpanded ? 'true' : 'false');
        } catch (e) {}
    }

    function setExpandedState(expanded) {
        isExpanded = expanded;
        if (!drawer) return;
        if (isExpanded) {
            drawer.classList.add('is-expanded');
        } else {
            drawer.classList.remove('is-expanded');
        }
        if (expandBtn) {
            const iconExp = expandBtn.querySelector('.icon-expand');
            const iconComp = expandBtn.querySelector('.icon-compress');
            if (iconExp && iconComp) {
                iconExp.style.display = isExpanded ? 'none' : 'block';
                iconComp.style.display = isExpanded ? 'block' : 'none';
            }
            expandBtn.title = isExpanded ? 'Standard View' : 'Wide View';
        }
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (!input) return;
        const text = input.value.trim();
        if (!text) return;

        input.value = '';
        sendQuery(text);
    }

    function sendQuery(text) {
        if (!text) return;

        // Render user message bubble
        appendMessage('user', text, null, null);

        // Show typing indicator
        if (typingIndicator) {
            typingIndicator.classList.add('is-visible');
            scrollToBottom();
        }

        // Disable send button while request is processing
        const sendBtn = form ? form.querySelector('.assistant-send-btn') : null;
        if (sendBtn) {
            sendBtn.disabled = true;
            sendBtn.classList.add('is-loading');
        }

        const csrfToken = getCsrfToken();
        const csrfHeader = getCsrfHeader();

        const payload = {
            message: text,
            context: pageContext
        };

        const headers = {
            'Content-Type': 'application/json'
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch('/api/assistant/chat', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(payload)
        })
        .then(response => {
            if (!response.ok) {
                let friendlyMsg = 'Server returned HTTP ' + response.status;
                if (response.status === 401) {
                    friendlyMsg = 'Your session has expired. Please refresh the page to sign in again.';
                } else if (response.status === 429) {
                    friendlyMsg = 'The assistant is currently handling high request volume. Please wait a moment and retry.';
                } else if (response.status >= 500) {
                    friendlyMsg = 'Studio service encountered an error while retrieving data. Please retry.';
                }
                throw new Error(friendlyMsg);
            }
            return response.json();
        })
        .then(data => {
            if (typingIndicator) {
                typingIndicator.classList.remove('is-visible');
            }
            appendMessage('assistant', data.reply, null, data.actions);
        })
        .catch(err => {
            if (typingIndicator) {
                typingIndicator.classList.remove('is-visible');
            }
            appendError(err.message || 'Unable to connect to studio assistant service.', text);
        })
        .finally(() => {
            if (sendBtn) {
                sendBtn.disabled = false;
                sendBtn.classList.remove('is-loading');
            }
            if (input) {
                input.focus();
            }
        });
    }

    function appendError(errorMessage, retryQuery) {
        if (!messagesContainer) return;

        const row = document.createElement('div');
        row.className = 'message-row is-assistant is-error';

        const errorCard = document.createElement('div');
        errorCard.className = 'assistant-error-card';

        const header = document.createElement('div');
        header.className = 'error-card-header';
        header.innerHTML = `
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>Notice</span>
        `;

        const p = document.createElement('p');
        p.className = 'error-card-msg';
        p.textContent = errorMessage;

        errorCard.appendChild(header);
        errorCard.appendChild(p);

        if (retryQuery) {
            const retryBtn = document.createElement('button');
            retryBtn.type = 'button';
            retryBtn.className = 'assistant-retry-btn';
            retryBtn.innerHTML = `
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="23 4 23 10 17 10"></polyline>
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                </svg>
                <span>Retry</span>
            `;
            retryBtn.addEventListener('click', function() {
                row.remove();
                sendQuery(retryQuery);
            });
            errorCard.appendChild(retryBtn);
        }

        row.appendChild(errorCard);

        if (typingIndicator && typingIndicator.parentNode === messagesContainer) {
            messagesContainer.insertBefore(row, typingIndicator);
        } else {
            messagesContainer.appendChild(row);
        }

        scrollToBottom();
    }

    function handleClearHistory() {
        if (!confirm('Clear conversation history?')) return;

        const csrfToken = getCsrfToken();
        const csrfHeader = getCsrfHeader();
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        fetch('/api/assistant/clear', {
            method: 'POST',
            headers: headers
        })
        .then(() => {
            if (messagesContainer) {
                messagesContainer.innerHTML = '';
            }
            appendMessage('assistant', 'Conversation cleared. How can I assist you with your tailoring orders today?', 'system', null);
        });
    }

    function appendMessage(role, rawContent, provider, actions) {
        if (!messagesContainer) return;

        const row = document.createElement('div');
        row.className = 'message-row ' + (role === 'user' ? 'is-user' : 'is-assistant');

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        bubble.innerHTML = formatMarkdown(rawContent);

        // If assistant response contains a data table, allow row to expand across full drawer width
        if (role === 'assistant' && bubble.querySelector('.assistant-table-wrapper')) {
            row.classList.add('has-table');
        }

        // Render any quick action buttons
        if (actions && Array.isArray(actions) && actions.length > 0) {
            const actionsWrap = document.createElement('div');
            actionsWrap.className = 'assistant-actions-wrap';
            actions.forEach(act => {
                const link = document.createElement('a');
                link.className = 'assistant-action-link';
                link.href = act.url;
                link.textContent = act.label;
                actionsWrap.appendChild(link);
            });
            bubble.appendChild(actionsWrap);
        }

        row.appendChild(bubble);

        // Message Meta (clean timestamp only, no internal model/provider tags)
        const meta = document.createElement('div');
        meta.className = 'message-meta';
        const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const timeSpan = document.createElement('span');
        timeSpan.textContent = (role === 'user' ? 'You • ' : 'Studio Assistant • ') + timeStr;
        meta.appendChild(timeSpan);

        row.appendChild(meta);

        // Insert before typing indicator
        if (typingIndicator && typingIndicator.parentNode === messagesContainer) {
            messagesContainer.insertBefore(row, typingIndicator);
        } else {
            messagesContainer.appendChild(row);
        }

        scrollToBottom();
    }

    function formatMarkdown(text) {
        if (!text) return '';
        let s = text.replace(/\r\n/g, '\n');
        s = parseTables(s);

        const rawBlocks = s.split(/\n{2,}/);
        const out = [];

        rawBlocks.forEach(block => {
            const trimmed = block.trim();
            if (!trimmed) return;

            // If block is a parsed table, leave it untouched
            if (trimmed.startsWith('<div class="assistant-table-wrapper"')) {
                out.push(trimmed);
                return;
            }

            const lines = trimmed.split('\n');
            let blockHtml = '';
            let inList = false;
            let listType = 'ul';

            for (let i = 0; i < lines.length; i++) {
                const line = lines[i].trim();
                const isBullet = /^[•\-\*]\s+(.*)$/.test(line);
                const isNumbered = /^\d+\.\s+(.*)$/.test(line);

                if (isBullet || isNumbered) {
                    const item = line.replace(/^(?:[•\-\*]|\d+\.)\s+/, '');
                    const currentListType = isNumbered ? 'ol' : 'ul';

                    if (!inList) {
                        listType = currentListType;
                        blockHtml += '<' + listType + ' class="assistant-list">';
                        inList = true;
                    } else if (listType !== currentListType) {
                        blockHtml += '</' + listType + '><' + currentListType + ' class="assistant-list">';
                        listType = currentListType;
                    }

                    blockHtml += '<li>' + formatInline(item) + '</li>';
                } else {
                    if (inList) {
                        blockHtml += '</' + listType + '>';
                        inList = false;
                    }
                    if (line) {
                        blockHtml += '<p>' + formatInline(line) + '</p>';
                    }
                }
            }
            if (inList) {
                blockHtml += '</' + listType + '>';
            }

            out.push(blockHtml);
        });

        return out.join('');
    }

    function parseTables(text) {
        const lines = text.split('\n');
        const out = [];
        let i = 0;

        while (i < lines.length) {
            const line = lines[i].trim();
            if (line.startsWith('|') && line.endsWith('|') && i + 1 < lines.length) {
                const next = lines[i + 1].trim();
                if (/^\|(\s*:?-+:?\s*\|)+$/.test(next)) {
                    const rawHeaders = line.slice(1, -1).split('|');
                    const alignDefs = next.slice(1, -1).split('|');

                    const aligns = alignDefs.map((def, idx) => {
                        const trimmed = def.trim();
                        const left = trimmed.startsWith(':');
                        const right = trimmed.endsWith(':');
                        if (left && right) return 'center';
                        if (right) return 'right';
                        if (left) return 'left';
                        const h = (rawHeaders[idx] || '').trim().toLowerCase();
                        if (/value|amount|balance|price|total|due|rate|cost|₹/.test(h)) {
                            return 'right';
                        }
                        return 'left';
                    });

                    let tableHtml = '<div class="assistant-table-wrapper"><table class="assistant-table"><thead><tr>';
                    rawHeaders.forEach((h, idx) => {
                        const align = aligns[idx] || 'left';
                        const alignStyle = align !== 'left' ? ' style="text-align: ' + align + ';"' : '';
                        tableHtml += '<th' + alignStyle + '>' + formatInline(h.trim()) + '</th>';
                    });
                    tableHtml += '</tr></thead><tbody>';

                    i += 2; // skip header and delimiter row

                    while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
                        const rowLine = lines[i].trim();
                        const cells = rowLine.slice(1, -1).split('|');
                        tableHtml += '<tr>';
                        cells.forEach((c, idx) => {
                            const align = aligns[idx] || 'left';
                            const alignStyle = align !== 'left' ? ' style="text-align: ' + align + ';"' : '';
                            const content = c.trim();
                            const subTable = formatMeasurementCell(content);
                            if (subTable) {
                                tableHtml += '<td class="has-nested-table">' + subTable + '</td>';
                            } else {
                                tableHtml += '<td' + alignStyle + '>' + (content ? formatInline(content) : '') + '</td>';
                            }
                        });
                        tableHtml += '</tr>';
                        i++;
                    }

                    tableHtml += '</tbody></table></div>';
                    out.push(tableHtml);
                    continue;
                }
            }
            out.push(lines[i]);
            i++;
        }
        return out.join('\n');
    }

    function formatMeasurementCell(rawCell) {
        if (!rawCell) return null;
        const str = rawCell.trim();

        // Check if cell has delimiters indicating multiple items: <br>, &lt;br&gt;, \n
        if (!/(?:<br\s*\/?>|&lt;br\s*\/?&gt;|\n)/i.test(str)) {
            return null;
        }

        const items = str.split(/(?:<br\s*\/?>|&lt;br\s*\/?&gt;|\n)/gi)
            .map(s => s.trim())
            .filter(Boolean);

        if (items.length < 2) {
            return null;
        }

        const pairs = [];
        for (const item of items) {
            // Strip leading bullets/dashes/numbering: •, -, *, 1.
            const cleaned = item.replace(/^(?:[•\-\*]|\d+\.)\s*/, '').trim();
            const colonIdx = cleaned.indexOf(':');
            if (colonIdx <= 0) continue;

            const param = cleaned.substring(0, colonIdx).replace(/\*/g, '').trim();
            let val = cleaned.substring(colonIdx + 1).trim();

            if (val.startsWith('**') && !val.substring(2).includes('**')) {
                val = val.substring(2).trim();
            }

            if (param && val) {
                pairs.push({ param, val });
            }
        }

        // Only transform if all items matched key-value pairs
        if (pairs.length >= 2 && pairs.length === items.length) {
            let subHtml = '<table class="assistant-subtable"><thead><tr><th>Parameter</th><th style="text-align: right;">Value</th></tr></thead><tbody>';
            pairs.forEach(p => {
                subHtml += '<tr><td class="subtable-param"><strong>' + escapeHtml(p.param) + '</strong></td><td class="subtable-val" style="text-align: right;">' + formatInline(p.val) + '</td></tr>';
            });
            subHtml += '</tbody></table>';
            return subHtml;
        }

        return null;
    }

    function formatInline(str) {
        if (!str) return '';
        let s = escapeHtml(str);

        // Normalize unicode spaces and hyphens commonly emitted by LLMs
        s = s.replace(/[\u00A0\u202F\u2007]/g, ' ');
        s = s.replace(/[\u2010\u2011\u2012\u2013\u2014]/g, '-');

        // Support line breaks like <br> or <br/>
        s = s.replace(/&lt;br\s*\/?&gt;/gi, '<br>');

        // Bold: **text**
        s = s.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // Italics: *text*
        s = s.replace(/(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)/g, '<em>$1</em>');

        // Code: `code`
        s = s.replace(/`([^`]+)`/g, '<code class="assistant-inline-code">$1</code>');

        // Highlight Order numbers like #TS-2026-1001 or TS-2026-1001 or #1001
        s = s.replace(/(?:#|order\s*#?)?(TS-\d{4}-\d{3,5})/gi, '<span class="assistant-order-tag">$1</span>');
        s = s.replace(/(?:#|order\s*#)(\d{4,5})\b/gi, '<span class="assistant-order-tag">#$1</span>');

        // Highlight Statuses (parenthesized or badge keywords)
        s = s.replace(/\((Paid in Full|Delivered|Completed)\)/gi, '<span class="assistant-badge is-success">$1</span>');
        s = s.replace(/\((Active|Pending|In Progress)\)/gi, '<span class="assistant-badge is-neutral">$1</span>');
        s = s.replace(/\((Unpaid|Balance Due|Overdue)\)/gi, '<span class="assistant-badge is-warning">$1</span>');

        // Highlight Currency: ₹ 1,500.00 or Rs. 1500
        s = s.replace(/(₹\s?[\d,]+(?:\.\d{2})?|Rs\.?\s?[\d,]+(?:\.\d{2})?)/gi, '<span class="assistant-currency">$1</span>');

        // Highlight Measurements: e.g. 38.00 in, 42.50 inches, 34 cm
        s = s.replace(/\b(\d+(?:\.\d{1,2})?)\s*(inches|inch|in|cm|mm|mtr)\b/gi, '<span class="assistant-measurement">$1 $2</span>');

        return s;
    }

    function escapeHtml(str) {
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function scrollToBottom() {
        if (messagesContainer) {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    }

    function getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : '';
    }

    function getCsrfHeader() {
        const meta = document.querySelector('meta[name="_csrf_header"]');
        return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
    }

    // Auto initialize on DOMContentLoaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
