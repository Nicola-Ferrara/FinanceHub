// Variabili globali
let bilanciData = [];
let filteredData = [];
let detailChartInstance = null;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupEventListeners();
    fetchBilanci();
});

// Setup event listeners
function setupEventListeners() {
    // Filtri
    document.getElementById('yearFilter').addEventListener('change', applyFilters);
    document.getElementById('sortFilter').addEventListener('change', applyFilters);
    
    // Modale
    document.querySelector('.close').addEventListener('click', closeModal);
    window.addEventListener('click', function(event) {
        const modal = document.getElementById('detailModal');
        if (event.target === modal) {
            closeModal();
        }
    });
}

// Fetch bilanci dal server
async function fetchBilanci() {
    try {
        showLoading(true);
        
        const response = await fetch('/api/bilanci');
        if (!response.ok) {
            throw new Error('Errore durante il recupero dei bilanci');
        }
        
        bilanciData = await response.json();
        
        if (bilanciData.length === 0) {
            showNoData();
            return;
        }
        
        filteredData = [...bilanciData];
        
        setupYearFilter();
        updateSummary();
        applyFilters();
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante il caricamento dei bilanci');
    } finally {
        showLoading(false);
    }
}

// Setup filtro anni
function setupYearFilter() {
    const yearFilter = document.getElementById('yearFilter');
    const years = [...new Set(bilanciData.map(bilancio => 
        new Date(bilancio.anno, bilancio.mese - 1).getFullYear()
    ))].sort((a, b) => b - a);
    
    // Pulisci opzioni esistenti (tranne "Tutti gli anni")
    while (yearFilter.children.length > 1) {
        yearFilter.removeChild(yearFilter.lastChild);
    }
    
    years.forEach(year => {
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year;
        yearFilter.appendChild(option);
    });
}

// Applica filtri
function applyFilters() {
    const yearFilter = document.getElementById('yearFilter').value;
    const sortFilter = document.getElementById('sortFilter').value;
    
    // Filtra per anno
    filteredData = bilanciData.filter(bilancio => {
        if (!yearFilter) return true;
        const year = new Date(bilancio.anno, bilancio.mese - 1).getFullYear();
        return year.toString() === yearFilter;
    });
    
    // Ordina
    filteredData.sort((a, b) => {
        switch (sortFilter) {
            case 'date-desc':
                return new Date(b.anno, b.mese - 1) - new Date(a.anno, a.mese - 1);
            case 'date-asc':
                return new Date(a.anno, a.mese - 1) - new Date(b.anno, b.mese - 1);
            case 'income-desc':
                return b.entrate - a.entrate;
            case 'expense-desc':
                return b.uscite - a.uscite;
            case 'balance-desc':
                return (b.entrate - b.uscite) - (a.entrate - a.uscite);
            case 'balance-asc':
                return (a.entrate - a.uscite) - (b.entrate - b.uscite);
            default:
                return new Date(b.anno, b.mese - 1) - new Date(a.anno, a.mese - 1);
        }
    });
    
    renderBilanci();
}

// Aggiorna riepilogo generale
function updateSummary() {
    const totalIncome = bilanciData.reduce((sum, bilancio) => sum + bilancio.entrate, 0);
    const totalExpense = bilanciData.reduce((sum, bilancio) => sum + bilancio.uscite, 0);
    const netBalance = totalIncome - totalExpense;
    const monthsCount = bilanciData.length;
    
    document.getElementById('totalIncomeAll').textContent = totalIncome.toFixed(2) + ' €';
    document.getElementById('totalExpenseAll').textContent = totalExpense.toFixed(2) + ' €';
    
    const netBalanceElement = document.getElementById('netBalanceAll');
    netBalanceElement.textContent = netBalance.toFixed(2) + ' €';
    netBalanceElement.className = `amount ${netBalance >= 0 ? 'positive' : 'negative'}`;
    
    document.getElementById('monthsCount').textContent = monthsCount;
}

// Renderizza lista bilanci
function renderBilanci() {
    const container = document.getElementById('bilanciContainer');
    container.innerHTML = '';
    
    if (filteredData.length === 0) {
        container.innerHTML = `
            <div class="no-results">
                <p>Nessun bilancio trovato per i filtri selezionati.</p>
            </div>
        `;
        return;
    }
    
    filteredData.forEach(bilancio => {
        const card = createBilancioCard(bilancio);
        container.appendChild(card);
    });
}

// Crea card singolo bilancio
function createBilancioCard(bilancio) {
    const netBalance = bilancio.entrate - bilancio.uscite;
    const monthNames = [
        'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno',
        'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre'
    ];
    
    const card = document.createElement('div');
    card.className = 'bilancio-card';
    card.onclick = () => openDetailModal(bilancio);
    
    card.innerHTML = `
        <div class="bilancio-indicator ${netBalance >= 0 ? 'positive' : 'negative'}"></div>
        <div class="bilancio-header">
            <h3 class="bilancio-title">${monthNames[bilancio.mese - 1]}</h3>
            <span class="bilancio-year">${bilancio.anno}</span>
        </div>
        <div class="bilancio-stats">
            <div class="stat-row">
                <span class="stat-label">Entrate:</span>
                <span class="stat-value positive">${bilancio.entrate.toFixed(2)} €</span>
            </div>
            <div class="stat-row">
                <span class="stat-label">Uscite:</span>
                <span class="stat-value negative">${bilancio.uscite.toFixed(2)} €</span>
            </div>
            <div class="stat-row">
                <span class="stat-label">Bilancio:</span>
                <span class="stat-value ${netBalance >= 0 ? 'positive' : 'negative'}">${netBalance.toFixed(2)} €</span>
            </div>
            <div class="stat-row">
                <span class="stat-label">Transazioni:</span>
                <span class="stat-value neutral">${bilancio.transazioni || 0}</span>
            </div>
        </div>
    `;
    
    return card;
}

// Apri modale dettaglio
function openDetailModal(bilancio) {
    const monthNames = [
        'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno',
        'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre'
    ];
    
    const netBalance = bilancio.entrate - bilancio.uscite;
    
    document.getElementById('modalTitle').textContent = 
        `${monthNames[bilancio.mese - 1]} ${bilancio.anno}`;
    
    document.getElementById('modalIncome').textContent = bilancio.entrate.toFixed(2) + ' €';
    document.getElementById('modalExpense').textContent = bilancio.uscite.toFixed(2) + ' €';
    
    const modalBalance = document.getElementById('modalBalance');
    modalBalance.textContent = netBalance.toFixed(2) + ' €';
    modalBalance.className = `stat-value ${netBalance >= 0 ? 'positive' : 'negative'}`;
    
    document.getElementById('modalTransactions').textContent = bilancio.transazioni || 0;
    
    createDetailChart(bilancio.entrate, bilancio.uscite);
    
    document.getElementById('detailModal').style.display = 'block';
}

// Crea grafico dettaglio
function createDetailChart(entrate, uscite) {
    const ctx = document.getElementById('detailChart').getContext('2d');
    
    if (detailChartInstance) {
        detailChartInstance.destroy();
    }
    
    if (entrate === 0 && uscite === 0) {
        ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
        ctx.font = '16px Roboto';
        ctx.fillStyle = '#6c757d';
        ctx.textAlign = 'center';
        ctx.fillText('Nessun dato disponibile', ctx.canvas.width / 2, ctx.canvas.height / 2);
        return;
    }
    
    detailChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Entrate', 'Uscite'],
            datasets: [{
                data: [entrate, uscite],
                backgroundColor: ['#28a745', '#dc3545'],
                borderWidth: 0,
                cutout: '60%'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 20,
                        usePointStyle: true,
                        font: {
                            size: 14
                        }
                    }
                }
            }
        }
    });
}

// Chiudi modale
function closeModal() {
    document.getElementById('detailModal').style.display = 'none';
    if (detailChartInstance) {
        detailChartInstance.destroy();
        detailChartInstance = null;
    }
}

// Mostra/nascondi loading
function showLoading(show) {
    const loadingMessage = document.getElementById('loadingMessage');
    const bilanciContainer = document.getElementById('bilanciContainer');
    
    if (show) {
        loadingMessage.style.display = 'block';
        bilanciContainer.style.display = 'none';
    } else {
        loadingMessage.style.display = 'none';
        bilanciContainer.style.display = 'grid';
    }
}

// Mostra messaggio nessun dato
function showNoData() {
    document.getElementById('loadingMessage').style.display = 'none';
    document.getElementById('bilanciContainer').style.display = 'none';
    document.getElementById('noDataMessage').style.display = 'block';
    
    // Nascondi anche le sezioni summary e filtri
    document.querySelector('.summary-section').style.display = 'none';
    document.querySelector('.filters-section').style.display = 'none';
}

// Mostra messaggio errore
function showErrorMessage(message) {
    const container = document.getElementById('bilanciContainer');
    container.innerHTML = `
        <div class="error-message">
            <div class="error-icon">❌</div>
            <h3>Errore</h3>
            <p>${message}</p>
            <button onclick="window.location.reload()" class="btn-primary">
                Ricarica Pagina
            </button>
        </div>
    `;
    container.style.display = 'block';
}

// Formatta data per display
function formatDate(year, month) {
    const monthNames = [
        'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno',
        'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre'
    ];
    return `${monthNames[month - 1]} ${year}`;
}