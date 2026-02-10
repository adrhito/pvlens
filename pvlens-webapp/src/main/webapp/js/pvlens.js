/**
 * PVLens Dashboard JavaScript
 * Handles interactive elements and dynamic UI behaviors
 */

(function() {
    'use strict';

    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', function() {
        initTabs();
        initSearch();
        initExpandButtons();
        initDateFilters();
    });

    /**
     * Tab Navigation for Detail Pages
     */
    function initTabs() {
        const tabButtons = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        if (tabButtons.length === 0) return;

        tabButtons.forEach(function(button) {
            button.addEventListener('click', function() {
                const targetTab = this.getAttribute('data-tab');

                // Update button states
                tabButtons.forEach(function(btn) {
                    btn.classList.remove('active');
                });
                this.classList.add('active');

                // Update content visibility
                tabContents.forEach(function(content) {
                    content.classList.remove('active');
                    if (content.id === targetTab) {
                        content.classList.add('active');
                    }
                });
            });
        });
    }

    /**
     * Header Search Functionality with Autocomplete
     */
    function initSearch() {
        const searchInputs = document.querySelectorAll('.search-input');
        console.log('PVLens: Found ' + searchInputs.length + ' search inputs');

        searchInputs.forEach(function(searchInput) {
            console.log('PVLens: Initializing autocomplete for', searchInput);
            initSearchAutocomplete(searchInput);
        });
    }

    /**
     * Initialize autocomplete for a search input
     */
    function initSearchAutocomplete(searchInput) {
        if (!searchInput) return;

        let debounceTimer;
        let selectedIndex = -1;
        let suggestions = [];

        // Create autocomplete dropdown
        const dropdown = document.createElement('div');
        dropdown.className = 'search-autocomplete';
        dropdown.style.display = 'none';
        searchInput.parentNode.style.position = 'relative';
        searchInput.parentNode.appendChild(dropdown);

        // Handle input changes
        searchInput.addEventListener('input', function() {
            clearTimeout(debounceTimer);
            const query = this.value.trim();
            selectedIndex = -1;

            if (query.length < 1) {
                hideDropdown();
                return;
            }

            debounceTimer = setTimeout(function() {
                fetchSuggestions(query);
            }, 200);
        });

        // Handle keyboard navigation
        searchInput.addEventListener('keydown', function(e) {
            if (dropdown.style.display === 'none') {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    performSearch(this.value.trim(), getSearchType());
                }
                return;
            }

            switch (e.key) {
                case 'ArrowDown':
                    e.preventDefault();
                    selectedIndex = Math.min(selectedIndex + 1, suggestions.length - 1);
                    updateSelection();
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    selectedIndex = Math.max(selectedIndex - 1, -1);
                    updateSelection();
                    break;
                case 'Enter':
                    e.preventDefault();
                    if (selectedIndex >= 0 && suggestions[selectedIndex]) {
                        selectSuggestion(suggestions[selectedIndex]);
                    } else {
                        performSearch(this.value.trim(), getSearchType());
                    }
                    hideDropdown();
                    break;
                case 'Escape':
                    hideDropdown();
                    break;
            }
        });

        // Hide dropdown when clicking outside
        document.addEventListener('click', function(e) {
            if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
                hideDropdown();
            }
        });

        // Focus shows dropdown if there are cached suggestions
        searchInput.addEventListener('focus', function() {
            if (suggestions.length > 0 && this.value.trim().length >= 1) {
                showDropdown();
            }
        });

        function getSearchType() {
            // Determine search type based on current page
            const path = window.location.pathname;
            if (path.includes('AdverseEvents')) return 'adverse_events';
            if (path.includes('Indications')) return 'indications';
            if (path.includes('Substances')) return 'substances';
            return 'all';
        }

        function fetchSuggestions(query) {
            const type = getSearchType();
            const apiUrl = '/pvlens/api/search/suggest?q=' + encodeURIComponent(query) + '&type=' + type + '&limit=10';
            console.log('PVLens: Fetching suggestions from', apiUrl);

            fetch(apiUrl)
                .then(function(response) {
                    if (!response.ok) throw new Error('Network error');
                    return response.json();
                })
                .then(function(data) {
                    console.log('PVLens: Received', data.length, 'suggestions');
                    suggestions = data;
                    renderSuggestions(query);
                })
                .catch(function(error) {
                    console.error('Autocomplete error:', error);
                    // Try to get spelling suggestions
                    fetchSpellingSuggestions(query);
                });
        }

        function fetchSpellingSuggestions(query) {
            const apiUrl = '/pvlens/api/search/spellcheck?q=' + encodeURIComponent(query);

            fetch(apiUrl)
                .then(function(response) {
                    return response.json();
                })
                .then(function(data) {
                    if (data.length > 0) {
                        renderSpellingSuggestions(query, data);
                    }
                })
                .catch(function(error) {
                    console.error('Spellcheck error:', error);
                });
        }

        function renderSuggestions(query) {
            if (suggestions.length === 0) {
                // No results - try spellcheck
                fetchSpellingSuggestions(query);
                return;
            }

            dropdown.innerHTML = '';

            // Group suggestions by category
            const grouped = {};
            suggestions.forEach(function(s) {
                const cat = s.category || 'Results';
                if (!grouped[cat]) grouped[cat] = [];
                grouped[cat].push(s);
            });

            Object.keys(grouped).forEach(function(category) {
                const catHeader = document.createElement('div');
                catHeader.className = 'autocomplete-category';
                catHeader.textContent = category;
                dropdown.appendChild(catHeader);

                grouped[category].forEach(function(suggestion, index) {
                    const item = document.createElement('div');
                    item.className = 'autocomplete-item';
                    item.dataset.index = suggestions.indexOf(suggestion);

                    // Highlight matching text
                    const term = suggestion.term || '';
                    const lowerTerm = term.toLowerCase();
                    const lowerQuery = query.toLowerCase();
                    const matchIndex = lowerTerm.indexOf(lowerQuery);

                    let html = '<div class="autocomplete-term">';
                    if (matchIndex >= 0) {
                        html += escapeHtml(term.substring(0, matchIndex));
                        html += '<strong>' + escapeHtml(term.substring(matchIndex, matchIndex + query.length)) + '</strong>';
                        html += escapeHtml(term.substring(matchIndex + query.length));
                    } else {
                        html += escapeHtml(term);
                    }
                    html += '</div>';

                    if (suggestion.code) {
                        html += '<div class="autocomplete-code">' + escapeHtml(suggestion.code) + '</div>';
                    }

                    if (suggestion.usageCount > 0) {
                        html += '<div class="autocomplete-count">' + suggestion.usageCount + ' records</div>';
                    }

                    item.innerHTML = html;

                    item.addEventListener('click', function() {
                        selectSuggestion(suggestion);
                        hideDropdown();
                    });

                    item.addEventListener('mouseenter', function() {
                        selectedIndex = parseInt(this.dataset.index);
                        updateSelection();
                    });

                    dropdown.appendChild(item);
                });
            });

            showDropdown();
        }

        function renderSpellingSuggestions(query, corrections) {
            dropdown.innerHTML = '';

            const header = document.createElement('div');
            header.className = 'autocomplete-spellcheck';
            header.innerHTML = '<span class="spellcheck-label">Did you mean:</span>';
            dropdown.appendChild(header);

            corrections.forEach(function(correction) {
                const item = document.createElement('div');
                item.className = 'autocomplete-item autocomplete-correction';
                item.innerHTML = '<div class="autocomplete-term">' + escapeHtml(correction.suggestion) + '</div>';

                item.addEventListener('click', function() {
                    searchInput.value = correction.suggestion;
                    hideDropdown();
                    fetchSuggestions(correction.suggestion);
                });

                dropdown.appendChild(item);
            });

            showDropdown();
        }

        function updateSelection() {
            const items = dropdown.querySelectorAll('.autocomplete-item');
            items.forEach(function(item, index) {
                if (parseInt(item.dataset.index) === selectedIndex) {
                    item.classList.add('selected');
                    item.scrollIntoView({ block: 'nearest' });
                } else {
                    item.classList.remove('selected');
                }
            });
        }

        function selectSuggestion(suggestion) {
            searchInput.value = suggestion.term;

            // Navigate based on suggestion type
            const type = suggestion.type || 'all';
            performSearch(suggestion.term, type);
        }

        function performSearch(query, type) {
            if (!query) return;

            const baseUrl = window.location.pathname.replace(/\/app\/.*$/, '/app/');
            let targetScreen = 'Substances';

            if (type === 'adverse_event' || type === 'adverse_events') {
                targetScreen = 'AdverseEvents';
            } else if (type === 'indication' || type === 'indications') {
                targetScreen = 'Indications';
            }

            window.location.href = baseUrl + targetScreen + '?search=' + encodeURIComponent(query);
        }

        function showDropdown() {
            dropdown.style.display = 'block';
        }

        function hideDropdown() {
            dropdown.style.display = 'none';
            selectedIndex = -1;
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    }

    /**
     * Perform global search navigation
     */
    function performGlobalSearch(query) {
        if (!query) return;

        // Navigate to substances page with search query
        const baseUrl = window.location.pathname.replace(/\/app\/.*$/, '/app/');
        window.location.href = baseUrl + 'screen/Substances?search=' + encodeURIComponent(query);
    }

    /**
     * Expand/Collapse Buttons (for insight cards, etc.)
     */
    function initExpandButtons() {
        const expandButtons = document.querySelectorAll('.expand-btn');

        expandButtons.forEach(function(button) {
            button.addEventListener('click', function() {
                const card = this.closest('.insight-card');
                if (card) {
                    card.classList.toggle('expanded');
                    // Rotate the icon
                    const icon = this.querySelector('svg');
                    if (icon) {
                        icon.style.transform = card.classList.contains('expanded')
                            ? 'rotate(45deg)'
                            : 'rotate(0deg)';
                    }
                }
            });
        });
    }

    /**
     * Date Filter Handling
     */
    function initDateFilters() {
        const dateInputs = document.querySelectorAll('.date-input');

        dateInputs.forEach(function(input) {
            input.addEventListener('change', function() {
                // Could trigger filter update via AJAX or form submission
                console.log('Date changed:', this.value);
            });
        });
    }

    /**
     * Format numbers with thousands separators
     */
    function formatNumber(num) {
        return new Intl.NumberFormat('en-US').format(num);
    }

    /**
     * Show loading state on element
     */
    function showLoading(element) {
        element.classList.add('loading');
        element.setAttribute('aria-busy', 'true');
    }

    /**
     * Hide loading state on element
     */
    function hideLoading(element) {
        element.classList.remove('loading');
        element.setAttribute('aria-busy', 'false');
    }

    /**
     * Toast notification system
     */
    const Toast = {
        container: null,

        init: function() {
            if (this.container) return;
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            this.container.setAttribute('aria-live', 'polite');
            document.body.appendChild(this.container);
        },

        show: function(message, type) {
            this.init();
            type = type || 'info';

            const toast = document.createElement('div');
            toast.className = 'toast toast-' + type;
            toast.textContent = message;

            this.container.appendChild(toast);

            // Trigger animation
            setTimeout(function() {
                toast.classList.add('show');
            }, 10);

            // Auto-dismiss after 3 seconds
            setTimeout(function() {
                toast.classList.remove('show');
                setTimeout(function() {
                    toast.remove();
                }, 300);
            }, 3000);
        },

        success: function(message) {
            this.show(message, 'success');
        },

        error: function(message) {
            this.show(message, 'error');
        },

        info: function(message) {
            this.show(message, 'info');
        }
    };

    /**
     * JSON-RPC Client for AJAX calls
     */
    const JsonRpc = {
        endpoint: '/pvlens/jsonrpc/DatabaseService',

        call: function(method, params) {
            return fetch(this.endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    jsonrpc: '2.0',
                    method: method,
                    params: params || [],
                    id: Date.now()
                })
            })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(function(data) {
                if (data.error) {
                    throw new Error(data.error.message || 'RPC Error');
                }
                return data.result;
            });
        }
    };

    /**
     * Keyboard shortcuts
     */
    document.addEventListener('keydown', function(e) {
        // Ctrl/Cmd + K for search focus
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            const searchInput = document.querySelector('.header .search-input');
            if (searchInput) {
                searchInput.focus();
                searchInput.select();
            }
        }

        // Escape to close modals or clear search
        if (e.key === 'Escape') {
            const searchInput = document.querySelector('.header .search-input');
            if (searchInput && document.activeElement === searchInput) {
                searchInput.blur();
                searchInput.value = '';
            }
        }
    });

    /**
     * Smooth scroll to anchor links
     */
    document.querySelectorAll('a[href^="#"]').forEach(function(anchor) {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    /**
     * Table row click handler for better UX
     */
    document.querySelectorAll('.data-table tbody tr').forEach(function(row) {
        const link = row.querySelector('a.substance-link');
        if (link) {
            row.style.cursor = 'pointer';
            row.addEventListener('click', function(e) {
                // Don't trigger if clicking on the link itself or other interactive elements
                if (e.target.tagName === 'A' || e.target.tagName === 'BUTTON') {
                    return;
                }
                link.click();
            });
        }
    });

    // Expose utilities to global scope if needed
    window.PVLens = {
        Toast: Toast,
        JsonRpc: JsonRpc,
        formatNumber: formatNumber
    };

})();
