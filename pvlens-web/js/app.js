// PVLens Static Site Application
const PAGE_SIZE = 20;

// State
let currentPage = {
    substances: 1,
    adverseEvents: 1,
    indications: 1,
    srlc: 1
};

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    checkConnection();
    loadOverview();
});

// Navigation
function initNavigation() {
    document.querySelectorAll('.nav-item, .link[data-page]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.dataset.page;
            navigateTo(page);
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
    document.querySelectorAll('.nav-item').forEach(item => {
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
        case 'srlc': loadSrlc(); break;
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

    // SRLC filters
    document.getElementById('srlc-search')?.addEventListener('input', debounce(() => {
        currentPage.srlc = 1;
        loadSrlc();
    }, 300));
    document.getElementById('srlc-year')?.addEventListener('change', () => {
        currentPage.srlc = 1;
        loadSrlc();
    });
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
        const [substances, adverseEvents, indications, srlc] = await Promise.all([
            window.db.from('substance').select('*', { count: 'exact', head: true }),
            window.db.from('product_ae').select('*', { count: 'exact', head: true }),
            window.db.from('product_ind').select('*', { count: 'exact', head: true }),
            window.db.from('srlc').select('*', { count: 'exact', head: true })
        ]);

        document.getElementById('stat-substances').textContent = formatNumber(substances.count || 0);
        document.getElementById('stat-adverse-events').textContent = formatNumber(adverseEvents.count || 0);
        document.getElementById('stat-indications').textContent = formatNumber(indications.count || 0);
        document.getElementById('stat-srlc').textContent = formatNumber(srlc.count || 0);

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
            <div class="list-item" onclick="showSubstanceDetail(${s.id})">
                <span class="list-item-name">${escapeHtml(name)}</span>
                <span class="list-item-meta">ID: ${s.id}</span>
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
        const badgeClass = ae.blackbox ? 'badge-red' : ae.warning ? 'badge-orange' : 'badge-blue';
        return `
            <div class="list-item">
                <span class="list-item-name">${escapeHtml(term)}</span>
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

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No substances found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(s => {
            const name = getSubstanceName(s);
            const sponsor = s.spl_srcfile?.[0]?.nda_sponsor || '-';
            const sourceType = s.spl_srcfile?.[0]?.source_type?.source_type || '-';
            return `
                <tr>
                    <td><a href="#" onclick="showSubstanceDetail(${s.id}); return false;">${escapeHtml(name)}</a></td>
                    <td><span class="badge badge-blue">${escapeHtml(sourceType)}</span></td>
                    <td>${escapeHtml(sponsor)}</td>
                    <td><button class="btn btn-outline" onclick="showSubstanceDetail(${s.id})">View</button></td>
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

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No adverse events found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(ae => {
            const term = ae.meddra?.meddra_term || 'Unknown';
            const severityLabel = ae.blackbox ? 'Black Box' : ae.warning ? 'Warning' : 'Standard';
            const severityClass = ae.blackbox ? 'badge-red' : ae.warning ? 'badge-orange' : 'badge-blue';
            const matchType = ae.exact_match ? 'Exact' : 'NLP';
            return `
                <tr>
                    <td>${escapeHtml(term)}</td>
                    <td>Substance #${ae.product_id}</td>
                    <td><span class="badge ${severityClass}">${severityLabel}</span></td>
                    <td><span class="badge badge-${ae.exact_match ? 'green' : 'purple'}">${matchType}</span></td>
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

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="empty-state">No indications found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(ind => {
            const term = ind.meddra?.meddra_term || 'Unknown';
            const matchType = ind.exact_match ? 'Exact' : 'NLP';
            return `
                <tr>
                    <td>${escapeHtml(term)}</td>
                    <td>Substance #${ind.product_id}</td>
                    <td><span class="badge badge-${ind.exact_match ? 'green' : 'purple'}">${matchType}</span></td>
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

// Load SRLC
async function loadSrlc() {
    const search = document.getElementById('srlc-search')?.value || '';
    const year = document.getElementById('srlc-year')?.value || 'all';
    const tbody = document.getElementById('srlc-table');

    tbody.innerHTML = '<tr><td colspan="5" class="loading">Loading...</td></tr>';

    try {
        let query = window.db
            .from('srlc')
            .select('*', { count: 'exact' })
            .order('supplement_date', { ascending: false })
            .range((currentPage.srlc - 1) * PAGE_SIZE, currentPage.srlc * PAGE_SIZE - 1);

        if (search) {
            query = query.or(`drug_name.ilike.%${search}%,active_ingredient.ilike.%${search}%`);
        }

        const { data, count, error } = await query;
        if (error) throw error;

        if (!data?.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No SRLC updates found</td></tr>';
            return;
        }

        tbody.innerHTML = data.map(s => {
            const date = s.supplement_date ? new Date(s.supplement_date).toLocaleDateString() : '-';
            return `
                <tr>
                    <td>${escapeHtml(s.drug_name || '-')}</td>
                    <td>${escapeHtml(s.active_ingredient || '-')}</td>
                    <td>NDA ${String(s.application_number).padStart(6, '0')}</td>
                    <td>${date}</td>
                    <td>
                        ${s.url ? `<a href="${escapeHtml(s.url)}" target="_blank" class="btn btn-outline">FDA Link</a>` : '-'}
                    </td>
                </tr>
            `;
        }).join('');

        renderPagination('srlc-pagination', count, currentPage.srlc, (page) => {
            currentPage.srlc = page;
            loadSrlc();
        });

    } catch (err) {
        console.error('Error loading SRLC:', err);
        tbody.innerHTML = '<tr><td colspan="5" class="empty-state">Error loading data</td></tr>';
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
                <div class="detail-value"><span class="badge badge-blue">${escapeHtml(sourceType)}</span></div>
            </div>
            <div class="detail-section">
                <h4>NDC Codes</h4>
                <div class="detail-value">${ndcCodes.length ? ndcCodes.map(c => `<span class="badge badge-purple">${escapeHtml(c)}</span>`).join(' ') : 'None'}</div>
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

    let html = '';
    html += `<button ${current === 1 ? 'disabled' : ''} onclick="(${onPageChange})(${current - 1})">Prev</button>`;

    for (let i = 1; i <= Math.min(totalPages, 5); i++) {
        html += `<button class="${i === current ? 'active' : ''}" onclick="(${onPageChange})(${i})">${i}</button>`;
    }

    html += `<button ${current === totalPages ? 'disabled' : ''} onclick="(${onPageChange})(${current + 1})">Next</button>`;

    container.innerHTML = html;
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
