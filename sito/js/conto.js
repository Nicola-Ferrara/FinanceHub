// Variabili globali
let contoId = null;
let contoData = null;
let allOperations = [];
let currentFilter = 'all';

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    // Estrai l'ID del conto dall'URL
    const urlParams = new URLSearchParams(window.location.search);
    contoId = urlParams.get('id');
    
    if (!contoId) {
        // Se non c'è ID, reindirizza alla home
        window.location.href = '/home';
        return;
    }
    
    // Carica i dati del conto
    fetchContoData();
    
    // Imposta i listener per i filtri
    setupFilterListeners();
    
    // Imposta i listener per i bottoni azione
    setupActionListeners();
});

// Funzione per recuperare i dati del conto specifico
async function fetchContoData() {
    try {
        const response = await fetch(`/api/conto/${contoId}`);
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei dati del conto");
        }

        contoData = await response.json();
        
        // Aggiorna le informazioni del conto
        updateAccountInfo();
        
        // Carica le operazioni
        fetchOperazioni();
        
    } catch (error) {
        console.error("Errore:", error);
        showError("Errore durante il caricamento dei dati del conto");
    }
}

// Funzione per aggiornare le informazioni del conto
function updateAccountInfo() {
    if (!contoData) return;
    
    document.getElementById("accountName").textContent = contoData.nome;
    document.getElementById("accountType").textContent = contoData.tipo;
    
    const balanceElement = document.getElementById("accountBalance");
    balanceElement.textContent = `€ ${contoData.saldo.toFixed(2)}`;
    balanceElement.className = `account-balance ${contoData.saldo >= 0 ? "positive" : "negative"}`;
}

// Funzione per recuperare le operazioni del conto
async function fetchOperazioni() {
    try {
        const response = await fetch(`/api/conto/${contoId}/operazioni`);
        if (!response.ok) {
            throw new Error("Errore durante il recupero delle operazioni");
        }

        allOperations = await response.json();
        
        // Applica il filtro corrente
        filterOperations(currentFilter);
        
    } catch (error) {
        console.error("Errore:", error);
        showError("Errore durante il caricamento delle operazioni");
    }
}

// Funzione per filtrare e visualizzare le operazioni
function filterOperations(filter) {
    currentFilter = filter;
    
    let filteredOperations;
    switch (filter) {
        case 'transazioni':
            filteredOperations = allOperations.filter(op => op.tipo !== 'Trasferimento');
            break;
        case 'trasferimenti':
            filteredOperations = allOperations.filter(op => op.tipo === 'Trasferimento');
            break;
        default:
            filteredOperations = allOperations;
    }
    
    displayOperations(filteredOperations);
    updateFilterButtons();
}

// Funzione per visualizzare le operazioni
function displayOperations(operations) {
    const operationsList = document.getElementById("operationsList");
    operationsList.innerHTML = "";
    
    if (operations.length === 0) {
        const emptyState = document.createElement("li");
        emptyState.className = "empty-state";
        emptyState.textContent = "Nessuna operazione trovata";
        operationsList.appendChild(emptyState);
        return;
    }
    
    operations.forEach(operazione => {
        const li = document.createElement("li");
        
        // Indicatore di tipo operazione
        const indicator = document.createElement("div");
        indicator.classList.add("operation-indicator");
        
        // Determina il tipo e il colore dell'indicatore
        if (operazione.tipo === "Guadagno") {
            indicator.classList.add("income");
        } else if (operazione.tipo === "Spesa") {
            indicator.classList.add("expense");
        } else if (operazione.tipo === "Trasferimento") {
            if (operazione.isIncoming) {
                indicator.classList.add("transfer-in");
            } else {
                indicator.classList.add("transfer-out");
            }
        }
        
        // Dettagli dell'operazione
        const details = document.createElement("div");
        details.classList.add("operation-details");
        details.innerHTML = `
            <span>${operazione.descrizione}</span>
            <span class="operation-date">${formatDate(operazione.data)}</span>
            <span class="operation-category">${operazione.categoria}</span>
        `;
        
        // Importo dell'operazione
        const amount = document.createElement("span");
        amount.classList.add("operation-amount");
        
        if (operazione.tipo === "Guadagno") {
            amount.classList.add("income");
            amount.textContent = `+€${operazione.importo.toFixed(2)}`;
        } else if (operazione.tipo === "Spesa") {
            amount.classList.add("expense");
            amount.textContent = `-€${operazione.importo.toFixed(2)}`;
        } else if (operazione.tipo === "Trasferimento") {
            if (operazione.isIncoming) {
                amount.classList.add("income");
                amount.textContent = `+€${operazione.importo.toFixed(2)}`;
            } else {
                amount.classList.add("expense");
                amount.textContent = `-€${operazione.importo.toFixed(2)}`;
            }
        }
        
        // Aggiungi gli elementi alla lista
        li.appendChild(indicator);
        li.appendChild(details);
        li.appendChild(amount);
        operationsList.appendChild(li);
    });
}

// Funzione per aggiornare i bottoni filtro
function updateFilterButtons() {
    const filterButtons = document.querySelectorAll('.filter-btn');
    filterButtons.forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.filter === currentFilter) {
            btn.classList.add('active');
        }
    });
}

// Funzione per impostare i listener dei filtri
function setupFilterListeners() {
    const filterButtons = document.querySelectorAll('.filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            filterOperations(btn.dataset.filter);
        });
    });
}

// Funzione per impostare i listener delle azioni
function setupActionListeners() {
    document.getElementById('addTransactionBtn').addEventListener('click', () => {
        alert('Funzionalità in fase di sviluppo: Aggiungi Transazione');
    });
    
    document.getElementById('addTransferBtn').addEventListener('click', () => {
        alert('Funzionalità in fase di sviluppo: Nuovo Trasferimento');
    });
    
    document.getElementById('editAccountBtn').addEventListener('click', () => {
        openEditAccountModal();
    });
    
    setupModalListeners();
}

function openEditAccountModal() {
    if (!contoData) return;
    
    document.getElementById('editAccountName').value = contoData.nome;
    document.getElementById('editAccountType').value = contoData.tipo;
    
    document.getElementById('editAccountModal').style.display = 'block';
}

// Funzione helper per formattare la data
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('it-IT', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

// Funzione per mostrare errori
function showError(message) {
    const operationsList = document.getElementById("operationsList");
    operationsList.innerHTML = `<li class="empty-state" style="color: #dc3545;">${message}</li>`;
}

function setupModalListeners() {
    const editModal = document.getElementById('editAccountModal');
    const deleteModal = document.getElementById('deleteConfirmModal');
    
    // Chiudi modali cliccando sulla X
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', () => {
            editModal.style.display = 'none';
            deleteModal.style.display = 'none';
        });
    });
    
    // Chiudi modali cliccando fuori
    window.addEventListener('click', (event) => {
        if (event.target === editModal) {
            editModal.style.display = 'none';
        }
        if (event.target === deleteModal) {
            deleteModal.style.display = 'none';
        }
    });
    
    // Listener per il form di modifica
    document.getElementById('editAccountForm').addEventListener('submit', handleEditAccountSubmit);
    
    // Listener per i bottoni
    document.getElementById('cancelEdit').addEventListener('click', () => {
        editModal.style.display = 'none';
    });
    
    document.getElementById('deleteAccount').addEventListener('click', () => {
        document.getElementById('deleteAccountName').textContent = contoData.nome;
        deleteModal.style.display = 'block';
    });
    
    document.getElementById('cancelDelete').addEventListener('click', () => {
        deleteModal.style.display = 'none';
    });
    
    document.getElementById('confirmDelete').addEventListener('click', handleDeleteAccount);
}

// Funzione per gestire la modifica del conto
async function handleEditAccountSubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const updateData = {
        nome: formData.get('name'),
        tipo: formData.get('type')
    };
    
    try {
        const response = await fetch(`/api/conto/${contoId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updateData)
        });
        
        if (!response.ok) {
            throw new Error('Errore durante la modifica del conto');
        }
        
        // ✅ SPOSTA LA CHIUSURA DEL MODALE QUI (dopo il successo)
        document.getElementById('editAccountModal').style.display = 'none';
        
        // Ricarica i dati del conto
        await fetchContoData();
        
        // Mostra messaggio di successo
        showSuccessMessage('Conto modificato con successo!');
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante la modifica del conto');
        // Il modale resta aperto in caso di errore
    }
}

// Funzione per gestire l'eliminazione del conto
async function handleDeleteAccount() {
    try {
        const response = await fetch(`/api/conto/${contoId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('Errore durante l\'eliminazione del conto');
        }
        
        // ✅ CHIUDI ENTRAMBI I MODALI IMMEDIATAMENTE
        document.getElementById('deleteConfirmModal').style.display = 'none';
        document.getElementById('editAccountModal').style.display = 'none';
        
        // Mostra messaggio di successo con notifica elegante
        showSuccessMessage('Conto eliminato con successo!');
        
        // ✅ REINDIRIZZA IMMEDIATAMENTE (non aspettare 3 secondi)
        setTimeout(() => {
            window.location.href = '/home';
        }, 500);
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante l\'eliminazione del conto');
    }
}

// Funzioni per mostrare messaggi
function showSuccessMessage(message) {
    showNotification(message, 'success');
}

function showErrorMessage(message) {
    showNotification(message, 'error');
}

function showNotification(message, type) {
    // Rimuovi notifiche precedenti
    const existingNotifications = document.querySelectorAll('.notification');
    existingNotifications.forEach(notif => notif.remove());
    
    // Crea la notifica
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <span class="notification-icon">${type === 'success' ? '✅' : '❌'}</span>
            <span class="notification-message">${message}</span>
            <button class="notification-close">&times;</button>
        </div>
    `;
    
    // Aggiungi al body
    document.body.appendChild(notification);
    
    // Animazione di entrata
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    
    // Rimuovi automaticamente dopo 3 secondi
    setTimeout(() => {
        hideNotification(notification);
    }, 3000);
    
    // Event listener per chiudere manualmente
    notification.querySelector('.notification-close').addEventListener('click', () => {
        hideNotification(notification);
    });
}

function hideNotification(notification) {
    notification.classList.remove('show');
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 300);
}