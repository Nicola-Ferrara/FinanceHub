// Variabili globali
let operationsData = [];
let filteredData = [];

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupEventListeners();
    setupSidebar();
    fetchOperazioni();
});

// Setup event listeners
function setupEventListeners() {
    // Filtri
    document.getElementById('typeFilter').addEventListener('change', applyFilters);
    document.getElementById('categoryFilter').addEventListener('change', applyFilters);
    document.getElementById('sortFilter').addEventListener('change', applyFilters);
}

// Fetch operazioni dal server
async function fetchOperazioni() {
    try {
        showLoading(true);
        
        const response = await fetch('/api/operazioni');
        if (!response.ok) {
            throw new Error('Errore durante il recupero delle operazioni');
        }
        
        operationsData = await response.json();
        
        if (operationsData.length === 0) {
            showNoData();
            return;
        }
        
        filteredData = [...operationsData];
        
        updateSummary();
        applyFilters();
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante il caricamento delle operazioni');
    } finally {
        showLoading(false);
    }
}

// Applica filtri
function applyFilters() {
    const typeFilter = document.getElementById('typeFilter').value;
    const categoryFilter = document.getElementById('categoryFilter').value;
    const sortFilter = document.getElementById('sortFilter').value;
    
    // Filtra per tipo
    filteredData = operationsData.filter(operation => {
        if (!typeFilter) return true;
        return operation.tipo === typeFilter;
    });
    
    // Filtra per categoria (solo per transazioni)
    filteredData = filteredData.filter(operation => {
        if (!categoryFilter) return true;
        if (operation.tipo === 'trasferimento') return true;
        return operation.tipoCategoria === categoryFilter;
    });
    
    // Ordina
    filteredData.sort((a, b) => {
        switch (sortFilter) {
            case 'date-desc':
                return new Date(b.data) - new Date(a.data);
            case 'date-asc':
                return new Date(a.data) - new Date(b.data);
            case 'amount-desc':
                return b.importo - a.importo;
            case 'amount-asc':
                return a.importo - b.importo;
            default:
                return new Date(b.data) - new Date(a.data);
        }
    });
    
    renderOperazioni();
}

// Aggiorna riepilogo
function updateSummary() {
    const totalOperations = operationsData.length;
    
    const incomeTransactions = operationsData.filter(op => 
        op.tipo === 'transazione' && op.tipoCategoria === 'Guadagno'
    ).length;
    
    const expenseTransactions = operationsData.filter(op => 
        op.tipo === 'transazione' && op.tipoCategoria === 'Spesa'
    ).length;
    
    const transfers = operationsData.filter(op => op.tipo === 'trasferimento').length;
    
    document.getElementById('totalOperations').textContent = totalOperations;
    document.getElementById('totalIncomeTransactions').textContent = incomeTransactions;
    document.getElementById('totalExpenseTransactions').textContent = expenseTransactions;
    document.getElementById('totalTransfers').textContent = transfers;
}

// Renderizza lista operazioni
function renderOperazioni() {
    const container = document.getElementById('operationsContainer');
    container.innerHTML = '';
    
    if (filteredData.length === 0) {
        container.innerHTML = `
            <div class="no-results">
                <div style="text-align: center; padding: 40px; color: #6c757d;">
                    <div style="font-size: 48px; margin-bottom: 20px;">🔍</div>
                    <h3 style="margin: 0 0 10px 0; font-weight: 400;">Nessun risultato</h3>
                    <p style="margin: 0;">Nessuna operazione trovata per i filtri selezionati.</p>
                </div>
            </div>
        `;
        return;
    }
    
    filteredData.forEach(operation => {
        const card = createOperationCard(operation);
        container.appendChild(card);
    });
}

// Crea card singola operazione
function createOperationCard(operation) {
    const card = document.createElement('div');
    card.className = 'operation-card';
    
    // ✅ AGGIUNTO - Rendi la card cliccabile
    card.style.cursor = 'pointer';
    card.setAttribute('role', 'button');
    card.setAttribute('tabindex', '0');
    
    if (operation.tipo === 'transazione') {
        // Transazione
        const isIncome = operation.tipoCategoria === 'Guadagno';
        card.classList.add(isIncome ? 'income' : 'expense');
        
        card.innerHTML = `
            <div class="operation-header">
                <div class="operation-info">
                    <div class="operation-type">${isIncome ? 'Entrata' : 'Uscita'}</div>
                    <div class="operation-description">${operation.descrizione || 'Nessuna descrizione'}</div>
                    <div class="operation-details">
                        <strong>Categoria:</strong> ${operation.categoria} • 
                        <strong>Conto:</strong> ${operation.conto}
                    </div>
                </div>
                <div>
                    <div class="operation-amount ${isIncome ? 'positive' : 'negative'}">
                        ${isIncome ? '+' : '-'}${operation.importo.toFixed(2)} €
                    </div>
                    <div class="operation-date">${formatDate(operation.data)}</div>
                </div>
            </div>
        `;
        
        // ✅ AGGIUNTO - Click handler per transazioni
        card.addEventListener('click', function() {
            handleOperationClick(operation, card, `/transazione?id=${operation.id}`);
        });
        
        // ✅ AGGIUNTO - Keyboard accessibility
        card.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.click();
            }
        });
        
    } else {
        // Trasferimento
        card.classList.add('transfer');
        
        card.innerHTML = `
            <div class="operation-header">
                <div class="operation-info">
                    <div class="operation-type">Trasferimento</div>
                    <div class="operation-description">${operation.descrizione || 'Trasferimento tra conti'}</div>
                    <div class="operation-details">
                        <strong>Da:</strong> ${operation.contoMittente} → 
                        <strong>A:</strong> ${operation.contoDestinatario}
                    </div>
                </div>
                <div>
                    <div class="operation-amount neutral">${operation.importo.toFixed(2)} €</div>
                    <div class="operation-date">${formatDate(operation.data)}</div>
                </div>
            </div>
        `;
        
        // ✅ AGGIUNTO - Click handler per trasferimenti
        card.addEventListener('click', function() {
            handleOperationClick(operation, card, `/trasferimento?id=${operation.id}`);
        });
        
        // ✅ AGGIUNTO - Keyboard accessibility
        card.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.click();
            }
        });
    }
    
    return card;
}

// Formatta data per display
function formatDate(dateString) {
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

// Mostra/nascondi loading
function showLoading(show) {
    const loadingMessage = document.getElementById('loadingMessage');
    const operationsContainer = document.getElementById('operationsContainer');
    
    if (show) {
        loadingMessage.style.display = 'block';
        operationsContainer.style.display = 'none';
    } else {
        loadingMessage.style.display = 'none';
        operationsContainer.style.display = 'flex';
    }
}

// Mostra messaggio nessun dato
function showNoData() {
    document.getElementById('loadingMessage').style.display = 'none';
    document.getElementById('operationsContainer').style.display = 'none';
    document.getElementById('noDataMessage').style.display = 'block';
    
    // Nascondi anche le sezioni summary e filtri
    document.querySelector('.summary-section').style.display = 'none';
    document.querySelector('.filters-section').style.display = 'none';
}

// Mostra messaggio errore
function showErrorMessage(message) {
    const container = document.getElementById('operationsContainer');
    container.innerHTML = `
        <div class="error-message" style="text-align: center; padding: 60px 20px; color: #6c757d;">
            <div class="error-icon" style="font-size: 64px; margin-bottom: 20px;">❌</div>
            <h3 style="font-size: 24px; margin: 0 0 10px 0; font-weight: 400;">Errore</h3>
            <p style="font-size: 16px; margin: 0 0 30px 0;">${message}</p>
            <button onclick="window.location.reload()" class="btn-primary">
                Ricarica Pagina
            </button>
        </div>
    `;
    container.style.display = 'flex';
}

function handleOperationClick(operation, cardElement, targetUrl) {
    // Feedback visivo
    cardElement.style.transform = 'scale(0.98)';
    cardElement.style.transition = 'transform 0.15s ease';
    
    // Naviga dopo l'animazione
    setTimeout(() => {
        cardElement.style.transform = '';
        window.location.href = targetUrl;
    }, 150);
}