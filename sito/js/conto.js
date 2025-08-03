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

    const initialBalanceElement = document.getElementById("accountInitialBalance");
    const saldoIniziale = contoData.saldo_iniziale !== undefined ? contoData.saldo_iniziale : contoData.saldo;
    initialBalanceElement.textContent = `Saldo iniziale: € ${saldoIniziale.toFixed(2)}`;
    
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
        openAddTransactionModal();
    });
    
    document.getElementById('addTransferBtn').addEventListener('click', () => {
        openAddTransferModal();
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
    
    const saldoIniziale = contoData.saldo_iniziale !== undefined ? contoData.saldo_iniziale : contoData.saldo;
    
    document.getElementById('editAccountBalance').value = saldoIniziale.toFixed(2);
    
    document.getElementById('editAccountModal').style.display = 'block';
}

// Funzione helper per formattare la data
function formatDate(dateString) {
    const date = new Date(dateString);
    const dateFormatted = date.toLocaleDateString('it-IT', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
    
    const timeFormatted = date.toLocaleTimeString('it-IT', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false  // Formato 24 ore (0-23)
    });
    
    return `${dateFormatted} - ${timeFormatted}`;
}

// Funzione per mostrare errori
function showError(message) {
    const operationsList = document.getElementById("operationsList");
    operationsList.innerHTML = `<li class="empty-state" style="color: #dc3545;">${message}</li>`;
}

function setupModalListeners() {
    const editModal = document.getElementById('editAccountModal');
    const deleteModal = document.getElementById('deleteConfirmModal');
    const transactionModal = document.getElementById('addTransactionModal');
    const transferModal = document.getElementById('addTransferModal');
    
    // Chiudi modali cliccando sulla X
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', () => {
            editModal.style.display = 'none';
            deleteModal.style.display = 'none';
            transactionModal.style.display = 'none';
            transferModal.style.display = 'none';
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
        if (event.target === transactionModal) {
            transactionModal.style.display = 'none';
        }
        if (event.target === transferModal) {
            transferModal.style.display = 'none';
        }
    });
    
    // Form submissions
    document.getElementById('editAccountForm').addEventListener('submit', handleEditAccountSubmit);
    document.getElementById('addTransactionForm').addEventListener('submit', handleAddTransactionSubmit);
    document.getElementById('addTransferForm').addEventListener('submit', handleAddTransferSubmit);
    
    // Listeners per i campi
    document.getElementById('transactionType').addEventListener('change', handleTransactionTypeChange);
    document.getElementById('destinationAccount').addEventListener('change', handleDestinationAccountChange);
    
    // Bottoni di cancellazione
    document.getElementById('cancelEdit').addEventListener('click', () => {
        editModal.style.display = 'none';
    });
    
    document.getElementById('cancelTransaction').addEventListener('click', () => {
        transactionModal.style.display = 'none';
    });
    
    document.getElementById('cancelTransfer').addEventListener('click', () => {
        transferModal.style.display = 'none';
    });
    
    // Altri listener esistenti...
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
        nome: formData.get('name').trim(),
        tipo: formData.get('type'),
        saldo_iniziale: parseFloat(formData.get('balance'))
    };
    
    // Validazione nome conto
    if (!updateData.nome || updateData.nome.length === 0) {
        showErrorMessage('Inserisci il nome del conto');
        return;
    }
    
    if (updateData.nome.length < 2) {
        showErrorMessage('Il nome del conto deve essere di almeno 2 caratteri');
        return;
    }
    
    if (updateData.nome.length > 20) {
        showErrorMessage('Il nome del conto non può superare i 20 caratteri');
        return;
    }
    
    if (!updateData.tipo || updateData.tipo === '') {
        showErrorMessage('Seleziona il tipo di conto');
        return;
    }
    
    if (isNaN(updateData.saldo_iniziale)) {
        showErrorMessage('Inserisci un saldo iniziale valido');
        return;
    }

    if (updateData.saldo_iniziale > 9999999999999.99) {
        showErrorMessage(`Il saldo iniziale non può superare i 9999999999999.99 €`);
        return;
    }
    
    if (updateData.saldo_iniziale < -9999999999999.99) {
        showErrorMessage(`Il saldo iniziale non può essere inferiore a -9999999999999.99 €`);
        return;
    }
    
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
        
        document.getElementById('editAccountModal').style.display = 'none';
        
        // Ricarica i dati del conto
        await fetchContoData();
        
        // Mostra messaggio di successo
        showSuccessMessage('Conto modificato con successo!');
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante la modifica del conto');
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
        
        // ✅ REINDIRIZZA IMMEDIATAMENTE ALLA HOME CON PARAMETRO
        window.location.href = '/home?success=conto-eliminato';
        
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

// Funzione per aprire il modale aggiungi transazione
function openAddTransactionModal() {
    // Reset del form
    document.getElementById('addTransactionForm').reset();
    document.getElementById('categoryGroup').style.display = 'none';
    document.getElementById('transactionCategory').innerHTML = '<option value="">Seleziona una categoria</option>';

    const now = new Date();
    const localDateTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    document.getElementById('transactionDate').value = localDateTime;
    document.getElementById('transactionDate').max = localDateTime;
    
    document.getElementById('addTransactionModal').style.display = 'block';
}

// Funzione per gestire il cambio di tipo transazione
function handleTransactionTypeChange() {
    const typeSelect = document.getElementById('transactionType');
    const categoryGroup = document.getElementById('categoryGroup');
    const categorySelect = document.getElementById('transactionCategory');
    
    if (typeSelect.value) {
        // Mostra il gruppo categoria
        categoryGroup.style.display = 'block';
        
        // Cambia il colore del gruppo in base al tipo
        categoryGroup.className = 'form-group ' + typeSelect.value.toLowerCase();
        
        // Carica le categorie appropriate
        loadCategories(typeSelect.value);
    } else {
        categoryGroup.style.display = 'none';
        categorySelect.innerHTML = '<option value="">Seleziona una categoria</option>';
    }
}

// Funzione per caricare le categorie
async function loadCategories(tipo) {
    try {
        const endpoint = tipo === 'Spesa' ? '/api/categorie/spesa' : '/api/categorie/guadagno';
        const response = await fetch(endpoint);
        
        if (!response.ok) {
            throw new Error('Errore durante il caricamento delle categorie');
        }
        
        const categorie = await response.json();
        const categorySelect = document.getElementById('transactionCategory');
        
        // Svuota le opzioni esistenti
        categorySelect.innerHTML = '<option value="">Seleziona una categoria</option>';
        
        // Aggiungi le nuove categorie
        categorie.forEach(categoria => {
            const option = document.createElement('option');
            option.value = categoria.id;
            option.textContent = categoria.nome;
            if (tipo === 'Spesa') {
                option.style.color = '#dc3545';
            } else {
                option.style.color = '#28a745';
            }
            categorySelect.appendChild(option);
        });
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante il caricamento delle categorie');
    }
}

// Funzione per gestire il submit della transazione
async function handleAddTransactionSubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const transactionData = {
        importo: parseFloat(formData.get('importo')),
        descrizione: formData.get('descrizione').trim(),
        categoriaId: parseInt(formData.get('categoria')),
        data: formData.get('data')
    };
    
    if (!transactionData.importo || isNaN(transactionData.importo) || transactionData.importo <= 0) {
        showErrorMessage('Inserisci un importo valido maggiore di zero');
        return;
    }
    
    if (transactionData.importo < 0.01) {
        showErrorMessage('L\'importo minimo è €0.01');
        return;
    }
    
    if (transactionData.importo > 9999999999999.99) {
        showErrorMessage('L\'importo è troppo grande. Limite: €9.999.999.999.999,99');
        return;
    }
    
    if (!transactionData.descrizione || transactionData.descrizione.length === 0) {
        showErrorMessage('Inserisci una descrizione per la transazione');
        return;
    }
    
    if (transactionData.descrizione.length < 3) {
        showErrorMessage('La descrizione deve essere di almeno 3 caratteri');
        return;
    }
    
    if (transactionData.descrizione.length > 500) {
        showErrorMessage('La descrizione non può superare i 500 caratteri');
        return;
    }
    
    if (!transactionData.categoriaId || isNaN(transactionData.categoriaId)) {
        showErrorMessage('Seleziona una categoria valida');
        return;
    }
    
    if (!transactionData.data) {
        showErrorMessage('Inserisci la data della transazione');
        return;
    }
    
    const selectedDate = new Date(transactionData.data);
    const now = new Date();
    if (selectedDate > now) {
        showErrorMessage('La data non può essere futura');
        return;
    }
    
    // ✅ OPZIONALE: Controllo data troppo vecchia (se vuoi)
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
    if (selectedDate < oneYearAgo) {
        showErrorMessage('La data non può essere più vecchia di 1 anno');
        return;
    }
    
    try {
        const response = await fetch(`/api/conto/${contoId}/transazione`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(transactionData)
        });
        
        if (!response.ok) {
            throw new Error('Errore durante l\'aggiunta della transazione');
        }
        
        // Chiudi il modale
        document.getElementById('addTransactionModal').style.display = 'none';
        
        // Ricarica i dati
        await fetchContoData();
        await fetchOperazioni();
        
        // Mostra messaggio di successo
        showSuccessMessage('Transazione aggiunta con successo!');
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage('Errore durante l\'aggiunta della transazione');
    }
}

// Funzione per aprire il modale aggiungi trasferimento
function openAddTransferModal() {
    // Reset del form
    document.getElementById('addTransferForm').reset();
    document.getElementById('transferPreview').style.display = 'none';
    
    // Carica i conti disponibili
    loadDestinationAccounts();
    
    document.getElementById('addTransferModal').style.display = 'block';
}

// Funzione per caricare i conti di destinazione
async function loadDestinationAccounts() {
    try {
        const response = await fetch(`/api/conto/${contoId}/conti-disponibili`);

        if (!response.ok) {
            throw new Error('Errore durante il caricamento dei conti');
        }
        
        const conti = await response.json();
        const destinationSelect = document.getElementById('destinationAccount');
        
        // Svuota le opzioni esistenti
        destinationSelect.innerHTML = '<option value="">Seleziona il conto di destinazione</option>';
        
        if (conti.length === 0) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = 'Nessun conto disponibile';
            option.disabled = true;
            destinationSelect.appendChild(option);
        } else {
            conti.forEach(conto => {
                const option = document.createElement('option');
                option.value = conto.id;
                option.textContent = `${conto.nome} (${conto.tipo}) - €${conto.saldo.toFixed(2)}`;
                destinationSelect.appendChild(option);
            });
        }
        
    } catch (error) {
        console.error('Errore Completo:', error);
        showErrorMessage('Errore durante il caricamento dei conti');
    }
}

// Funzione per gestire il cambio di conto destinazione
function handleDestinationAccountChange() {
    const destinationSelect = document.getElementById('destinationAccount');
    const transferPreview = document.getElementById('transferPreview');
    const fromAccountName = document.getElementById('fromAccountName');
    const toAccountName = document.getElementById('toAccountName');
    
    if (destinationSelect.value && contoData) {
        // Mostra l'anteprima del trasferimento
        fromAccountName.textContent = contoData.nome;
        toAccountName.textContent = destinationSelect.options[destinationSelect.selectedIndex].text.split(' (')[0];
        transferPreview.style.display = 'block';
    } else {
        transferPreview.style.display = 'none';
    }
}

// Funzione per gestire il submit del trasferimento
async function handleAddTransferSubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const transferData = {
        importo: parseFloat(formData.get('importo')),
        descrizione: formData.get('descrizione').trim(),
        contoDestinatario: parseInt(formData.get('contoDestinatario'))
    };
    
    if (!transferData.importo || isNaN(transferData.importo) || transferData.importo <= 0) {
        showErrorMessage('Inserisci un importo valido maggiore di zero');
        return;
    }
    
    if (transferData.importo < 0.01) {
        showErrorMessage('L\'importo minimo per un trasferimento è €0.01');
        return;
    }
    
    if (!transferData.descrizione || transferData.descrizione.length === 0) {
        showErrorMessage('Inserisci una descrizione per il trasferimento');
        return;
    }
    
    if (transferData.descrizione.length < 3) {
        showErrorMessage('La descrizione deve essere di almeno 3 caratteri');
        return;
    }
    
    if (transferData.descrizione.length > 500) {
        showErrorMessage('La descrizione non può superare i 500 caratteri');
        return;
    }
    
    // Validazione conto destinazione
    if (!transferData.contoDestinatario || isNaN(transferData.contoDestinatario)) {
        showErrorMessage('Seleziona il conto di destinazione');
        return;
    }
    
    // Verifica che non sia lo stesso conto
    if (transferData.contoDestinatario === parseInt(contoId)) {
        showErrorMessage('Non puoi trasferire denaro sullo stesso conto');
        return;
    }
    
    // Verifica fondi sufficienti
    if (transferData.importo > contoData.saldo) {
        showErrorMessage(`Fondi insufficienti. Saldo disponibile: €${contoData.saldo.toFixed(2)}`);
        return;
    }
    
    try {
        const response = await fetch(`/api/conto/${contoId}/trasferimento`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(transferData)
        });
        
        if (!response.ok) {
            const result = await response.json();
            throw new Error(result.error || 'Errore durante il trasferimento');
        }
        
        // Chiudi il modale
        document.getElementById('addTransferModal').style.display = 'none';
        
        // Ricarica i dati
        await fetchContoData();
        await fetchOperazioni();
        
        // Mostra messaggio di successo
        showSuccessMessage('Trasferimento effettuato con successo!');
        
    } catch (error) {
        console.error('Errore:', error);
        showErrorMessage(error.message || 'Errore durante il trasferimento');
    }
}