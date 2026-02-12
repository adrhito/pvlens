// PVLens Static Site Application
const PAGE_SIZE = 20;

// State
let currentPage = {
    substances: 1,
    adverseEvents: 1,
    indications: 1
};

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    checkConnection();
    loadOverview();
});

// Navigation
function initNavigation() {
    // Nav links in header
    document.querySelectorAll('.nav-link, .stat-link, .activity-link, .logo').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.dataset.page;
            if (page) navigateTo(page);
        });
    });

    // Modal close
    document.querySelector('.modal-close')?.addEventListener('click', closeModal);
    document.getElementById('substance-modal')?.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) closeModal();
    });

    // Search and filter handlers
    setupFilters();
}

function navigateTo(page) {
    // Update nav
    document.querySelectorAll('.nav-link').forEach(item => {
        item.classList.toggle('active', item.dataset.page === page);
    });

    // Show page
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(`page-${page}`)?.classList.add('active');

    // Load data
    switch(page) {
        case 'overview': loadOverview(); break;
        case 'substances': loadSubstances(); break;
        case 'adverse-events': loadAdverseEvents(); break;
        case 'indications': loadIndications(); break;
    }
}

function setupFilters() {
    // Substance filters
    document.getElementById('substance-search')?.addEventListener('input', debounce(() => {
        currentPage.substances = 1;
        loadSubstances();
    }, 300));
    document.getElementById('substance-type')?.addEventListener('change', () => {
        currentPage.substances = 1;
        loadSubstances();
    });

    // AE filters
    document.getElementById('ae-search')?.addEventListener('input', debounce(() => {
        currentPage.adverseEvents = 1;
        loadAdverseEvents();
    }, 300));
    document.getElementById('ae-severity')?.addEventListener('change', () => {
        currentPage.adverseEvents = 1;
        loadAdverseEvents();
    });

    // Indication filters
    document.getElementById('ind-search')?.addEventListener('input', debounce(() => {
        currentPage.indications = 1;
        loadIndications();
    }, 300));
}

// Check Supabase connection
async function checkConnection() {
    const statusEl = document.getElementById('db-status');
    const dot = statusEl.querySelector('.status-dot');
    const text = statusEl.querySelector('span:last-child');

    try {
        const { count, error } = await window.db
            .from('substance')
            .select('*', { count: 'exact', head: true });

        if (error) throw error;

        dot.classList.add('connected');
        dot.classList.remove('error');
        text.textContent = 'Connected';
    } catch (err) {
        dot.classList.add('error');
        dot.classList.remove('connected');
        text.textContent = 'Not connected';
        console.error('Connection error:', err);
    }
}

// Load Overview
async function loadOverview() {
    try {
        // Load stats
        const [substances, adverseEvents, indications] = await Promise.all([
            window.db.from('substance').select('*', { count: 'exact', head: true }),
            window.db.from('product_ae').select('*', { count: 'exact', head: true }),
            window.db.from('product_ind').select('*', { count: 'exact', head: true })
        ]);

        document.getElementById('stat-substances').textContent = formatNumber(substances.count || 0);
        document.getElementById('stat-adverse-events').textContent = formatNumber(adverseEvents.count || 0);
        document.getElementById('stat-indications').textContent = formatNumber(indications.count || 0);

        // Load breakdown counts
        const [blackbox, warnings, exactMatch] = await Promise.all([
            window.db.from('product_ae').select('*', { count: 'exact', head: true }).eq('blackbox', true),
            window.db.from('product_ae').select('*', { count: 'exact', head: true }).eq('warning', true),
            window.db.from('product_ind').select('*', { count: 'exact', head: true }).eq('exact_match', true)
        ]);

        document.getElementById('stat-blackbox').textContent = formatNumber(blackbox.count || 0);
        document.getElementById('stat-warnings').textContent = formatNumber(warnings.count || 0);
        document.getElementById('stat-exact').textContent = formatNumber(exactMatch.count || 0);
        document.getElementById('stat-nlp').textContent = formatNumber((indications.count || 0) - (exactMatch.count || 0));

        // Placeholder for prescription/OTC counts (would need source_type join)
        document.getElementById('stat-prescription').textContent = '-';
        document.getElementById('stat-otc').textContent = '-';

        // Load recent substances
        const { data: recentSubstances } = await window.db
            .from('substance')
            .select(`
                id,
                spl_srcfile(nda_sponsor),
                product_ndc(ndc_code(product_name))
            `)
            .limit(5);

        renderRecentSubstances(recentSubstances || []);

        // Load recent adverse events
        const { data: recentAE } = await window.db
            .from('product_ae')
            .select(`
                id,
                product_id,
                blackbox,
                warning,
                meddra(meddra_term)
            `)
            .limit(5);

        renderRecentAdverseEvents(recentAE || []);

    } catch (err) {
        console.error('Error loading overview:', err);
    }
}

function renderRecentSubstances(substances) {
    const container = document.getElementById('recent-substances');
    if (!substances.length) {
        container.innerHTML = '<div class="empty-state">No substances found</div>';
        return;
    }

    container.innerHTML = substances.map(s => {
        const name = getSubstanceName(s);
        return `
            <div class="activity-item" onclick="showSubstanceDetail(${s.id})">
                <div class="activity-content">
                    <span class="activity-name">${escapeHtml(name)}</span>
                    <span class="activity-meta">ID: ${s.id}</span>
                </div>
                <a href="#" class="activity-action" onclick="showSubstanceDetail(${s.id}); return false;">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="9 18 15 12 9 6"/>
                    </svg>
                </a>
            </div>
        `;
    }).join('');
}

function renderRecentAdverseEvents(events) {
    const container = document.getElementById('recent-adverse-events');
    if (!events.length) {
        container.innerHTML = '<div class="empty-state">No adverse events found</div>';
        return;
    }

    container.innerHTML = events.map(ae => {
        const term = ae.meddra?.meddra_term || 'Unknown';
        const severity = ae.blackbox ? 'Black Box' : ae.warning ? 'Warning' : 'Standard';
        const badgeClass = ae.blackbox ? 'badge-blackbox' : ae.warning ? 'badge-warning' : 'badge-standard';
        return `
            <div class="activity-item">
                <div class="activity-content">
                    <span class="activity-name">${escapeHtml(term)}</span>
                    <span class="activity-meta">Substance #${ae.product_id}</span>
                </div>
                <span class="badge ${badgeClass}">${severity}</span>
            </div>
        `;
    }).join('');
}

// Load Substances
async function loadSubstances() {
    const search = document.getElementById('substance-search')?.value || '';
    const type = document.getElementById('substance-type')?.value || 'all';
    const tbody = document.getElementById('substances-table');

    tbody.innerHTML = '<tr><td colspan="4" class="loading">Loading...</td></tr>';

    try {
        let query = window.db
            .from('substance')
            .select(`
                id,
                spl_srcfile(nda_sponsor, source_type_id, source_type(source_type)),
                product_ndc(ndc_code(product_name))
            `, { count: 'exact' })
            .range((currentPage.substances - 1) * PAGE_SIZE, currentPage.substances * PAGE_SIZE - 1);

        const { data, count, error } = await query;
        if (error) throw error;

        document.getElementById('substances-count').textContent = `${formatNumber(count || 0)} total substances`;

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No substances found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(s => {
            const name = getSubstanceName(s);
            const sponsor = s.spl_srcfile?.[0]?.nda_sponsor || '-';
            const sourceType = s.spl_srcfile?.[0]?.source_type?.source_type || '-';
            const badgeClass = sourceType === 'PRESCRIPTION' ? 'badge-prescription' :
                              sourceType === 'OTC' ? 'badge-otc' : 'badge-other';
            return `
                <tr>
                    <td>
                        <div class="cell-main">
                            <a href="#" onclick="showSubstanceDetail(${s.id}); return false;" class="substance-link">${escapeHtml(name)}</a>
                        </div>
                    </td>
                    <td><span class="badge ${badgeClass}">${escapeHtml(sourceType)}</span></td>
                    <td>${escapeHtml(sponsor)}</td>
                    <td>
                        <a href="#" onclick="showSubstanceDetail(${s.id}); return false;" class="btn btn-sm btn-outline">View Details</a>
                    </td>
                </tr>
            `;
        }).join('');

        renderPagination('substances-pagination', count, currentPage.substances, (page) => {
            currentPage.substances = page;
            loadSubstances();
        });

    } catch (err) {
        console.error('Error loading substances:', err);
        tbody.innerHTML = '<tr><td colspan="4" class="empty-state">Error loading data</td></tr>';
    }
}

// Load Adverse Events
async function loadAdverseEvents() {
    const search = document.getElementById('ae-search')?.value || '';
    const severity = document.getElementById('ae-severity')?.value || 'all';
    const tbody = document.getElementById('adverse-events-table');

    tbody.innerHTML = '<tr><td colspan="4" class="loading">Loading...</td></tr>';

    try {
        let query = window.db
            .from('product_ae')
            .select(`
                id,
                product_id,
                blackbox,
                warning,
                exact_match,
                meddra(meddra_term, meddra_code)
            `, { count: 'exact' })
            .range((currentPage.adverseEvents - 1) * PAGE_SIZE, currentPage.adverseEvents * PAGE_SIZE - 1);

        if (severity === 'blackbox') query = query.eq('blackbox', true);
        else if (severity === 'warning') query = query.eq('warning', true).eq('blackbox', false);
        else if (severity === 'standard') query = query.eq('warning', false).eq('blackbox', false);

        const { data, count, error } = await query;
        if (error) throw error;

        document.getElementById('ae-count').textContent = `${formatNumber(count || 0)} total events`;

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No adverse events found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(ae => {
            const term = ae.meddra?.meddra_term || 'Unknown';
            const severityLabel = ae.blackbox ? 'Black Box' : ae.warning ? 'Warning' : 'Standard';
            const severityClass = ae.blackbox ? 'badge-blackbox' : ae.warning ? 'badge-warning' : 'badge-standard';
            const matchType = ae.exact_match ? 'Exact' : 'NLP';
            const matchClass = ae.exact_match ? 'badge-exact' : 'badge-nlp';
            return `
                <tr>
                    <td>${escapeHtml(term)}</td>
                    <td>Substance #${ae.product_id}</td>
                    <td><span class="badge ${severityClass}">${severityLabel}</span></td>
                    <td><span class="badge ${matchClass}">${matchType}</span></td>
                </tr>
            `;
        }).join('');

        renderPagination('ae-pagination', count, currentPage.adverseEvents, (page) => {
            currentPage.adverseEvents = page;
            loadAdverseEvents();
        });

    } catch (err) {
        console.error('Error loading adverse events:', err);
        tbody.innerHTML = '<tr><td colspan="4" class="empty-state">Error loading data</td></tr>';
    }
}

// Load Indications
async function loadIndications() {
    const search = document.getElementById('ind-search')?.value || '';
    const tbody = document.getElementById('indications-table');

    tbody.innerHTML = '<tr><td colspan="3" class="loading">Loading...</td></tr>';

    try {
        const { data, count, error } = await window.db
            .from('product_ind')
            .select(`
                id,
                product_id,
                exact_match,
                meddra(meddra_term, meddra_code)
            `, { count: 'exact' })
            .range((currentPage.indications - 1) * PAGE_SIZE, currentPage.indications * PAGE_SIZE - 1);

        if (error) throw error;

        document.getElementById('ind-count').textContent = `${formatNumber(count || 0)} total indications`;

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="empty-state">No indications found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(ind => {
            const term = ind.meddra?.meddra_term || 'Unknown';
            const matchType = ind.exact_match ? 'Exact' : 'NLP';
            const matchClass = ind.exact_match ? 'badge-exact' : 'badge-nlp';
            return `
                <tr>
                    <td>${escapeHtml(term)}</td>
                    <td>Substance #${ind.product_id}</td>
                    <td><span class="badge ${matchClass}">${matchType}</span></td>
                </tr>
            `;
        }).join('');

        renderPagination('ind-pagination', count, currentPage.indications, (page) => {
            currentPage.indications = page;
            loadIndications();
        });

    } catch (err) {
        console.error('Error loading indications:', err);
        tbody.innerHTML = '<tr><td colspan="3" class="empty-state">Error loading data</td></tr>';
    }
}

// Substance Detail Modal
async function showSubstanceDetail(id) {
    const modal = document.getElementById('substance-modal');
    const title = document.getElementById('modal-title');
    const body = document.getElementById('modal-body');

    modal.classList.add('active');
    title.textContent = 'Loading...';
    body.innerHTML = '<div class="loading">Loading substance details...</div>';

    try {
        const { data: substance, error } = await window.db
            .from('substance')
            .select(`
                id,
                spl_srcfile(guid, nda_sponsor, source_type(source_type)),
                product_ndc(ndc_code(product_name, ndc_code)),
                product_ae(id, blackbox, warning, meddra(meddra_term)),
                product_ind(id, meddra(meddra_term))
            `)
            .eq('id', id)
            .single();

        if (error) throw error;

        const name = getSubstanceName(substance);
        title.textContent = name;

        const sponsor = substance.spl_srcfile?.[0]?.nda_sponsor || 'Unknown';
        const sourceType = substance.spl_srcfile?.[0]?.source_type?.source_type || 'Unknown';
        const ndcCodes = substance.product_ndc?.map(p => p.ndc_code?.ndc_code).filter(Boolean) || [];
        const aeCount = substance.product_ae?.length || 0;
        const indCount = substance.product_ind?.length || 0;

        body.innerHTML = `
            <div class="detail-section">
                <h4>Sponsor</h4>
                <div class="detail-value">${escapeHtml(sponsor)}</div>
            </div>
            <div class="detail-section">
                <h4>Source Type</h4>
                <div class="detail-value"><span class="badge badge-prescription">${escapeHtml(sourceType)}</span></div>
            </div>
            <div class="detail-section">
                <h4>NDC Codes</h4>
                <div class="detail-value">${ndcCodes.length ? ndcCodes.map(c => `<span class="badge badge-ndc">${escapeHtml(c)}</span>`).join(' ') : 'None'}</div>
            </div>
            <div class="detail-section">
                <h4>Adverse Events</h4>
                <div class="detail-value">${aeCount} events found</div>
            </div>
            <div class="detail-section">
                <h4>Indications</h4>
                <div class="detail-value">${indCount} indications found</div>
            </div>
        `;

    } catch (err) {
        console.error('Error loading substance:', err);
        body.innerHTML = '<div class="empty-state">Error loading substance details</div>';
    }
}

function closeModal() {
    document.getElementById('substance-modal').classList.remove('active');
}

// Helper Functions
function getSubstanceName(substance) {
    // Try to get name from NDC
    const ndcName = substance.product_ndc?.[0]?.ndc_code?.product_name;
    if (ndcName) return ndcName;

    // Try sponsor name
    const sponsor = substance.spl_srcfile?.[0]?.nda_sponsor;
    if (sponsor) return `${sponsor} Product`;

    return `Unknown Substance (ID: ${substance.id})`;
}

function renderPagination(containerId, total, current, onPageChange) {
    const container = document.getElementById(containerId);
    const totalPages = Math.ceil(total / PAGE_SIZE);

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-controls">';

    if (current > 1) {
        html += `<button class="pagination-btn" onclick="window.paginationHandlers['${containerId}'](${current - 1})">Previous</button>`;
    }

    const startPage = Math.max(1, current - 2);
    const endPage = Math.min(totalPages, startPage + 4);

    for (let i = startPage; i <= endPage; i++) {
        html += `<button class="pagination-btn ${i === current ? 'active' : ''}" onclick="window.paginationHandlers['${containerId}'](${i})">${i}</button>`;
    }

    if (current < totalPages) {
        html += `<button class="pagination-btn" onclick="window.paginationHandlers['${containerId}'](${current + 1})">Next</button>`;
    }

    html += '</div>';
    html += `<div class="pagination-info">Page ${current} of ${totalPages}</div>`;

    container.innerHTML = html;

    // Store handler globally
    window.paginationHandlers = window.paginationHandlers || {};
    window.paginationHandlers[containerId] = onPageChange;
}

function formatNumber(num) {
    return new Intl.NumberFormat().format(num);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Make functions globally available
window.showSubstanceDetail = showSubstanceDetail;
window.closeModal = closeModal;
