// Variabili globali
let transactionData = null;
let originalData = null;
let isEditing = false;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupEventListeners();
    setupSidebar();
    loadTransaction();
});

// Ottieni ID transazione dall'URL
function getTransactionIdFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('id');
}

// Setup event listeners
function setupEventListeners() {
    // Form
    document.getElementById('editTransactionForm').addEventListener('submit', handleSaveTransaction);
    document.getElementById('cancelBtn').addEventListener('click', handleCancelEdit);
    
    // Delete button
    document.getElementById('deleteBtn').addEventListener('click', showDeleteModal);
    
    // Delete modal
    document.getElementById('closeDeleteModal').addEventListener('click', closeDeleteModal);
    document.getElementById('cancelDeleteBtn').addEventListener('click', closeDeleteModal);
    document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);

    // Click fuori dal modal per chiudere
    window.addEventListener('click', function(event) {
        const deleteModal = document.getElementById('deleteModal');
        
        if (event.target === deleteModal) {
            closeDeleteModal();
        }
    });
}

function formatImporto(event) {
    const input = event.target;
    let value = input.value;
    
    // Rimuovi caratteri non validi (tranne numeri, punti e virgole)
    let cleanValue = value.replace(/[^0-9.,]/g, '');
    
    // Sostituisci virgola con punto
    cleanValue = cleanValue.replace(',', '.');
    
    // Gestisci multipli punti decimali
    const parts = cleanValue.split('.');
    if (parts.length > 2) {
        cleanValue = parts[0] + '.' + parts.slice(1).join('');
    }
    
    // Limita a 2 decimali
    if (parts.length === 2 && parts[1].length > 2) {
        cleanValue = parts[0] + '.' + parts[1].substring(0, 2);
    }
    
    // Aggiorna solo se diverso
    if (cleanValue !== value) {
        const cursorPosition = input.selectionStart;
        input.value = cleanValue;
        
        // Ripristina posizione cursore
        const newPosition = Math.min(cursorPosition, cleanValue.length);
        input.setSelectionRange(newPosition, newPosition);
    }
}

// Carica dati transazione
async function loadTransaction() {
    try {
        const transactionId = getTransactionIdFromUrl();
        
        if (!transactionId) {
            showError('ID transazione mancante nell\'URL');
            return;
        }
        
        showLoading(true);
        
        const response = await fetch(`/api/transazione?id=${transactionId}`);
        
        if (!response.ok) {
            throw new Error('Transazione non trovata');
        }
        
        transactionData = await response.json();
        originalData = JSON.parse(JSON.stringify(transactionData)); // Deep copy
        
        await loadCategories();
        displayTransaction();
        showMainContent();
        
    } catch (error) {
        console.error('Errore caricamento transazione:', error);
        showError(error.message);
    } finally {
        showLoading(false);
    }
}

// Carica categorie
async function loadCategories() {
    try {
        const response = await fetch('/api/transazione-categorie');
        
        if (!response.ok) {
            throw new Error('Errore nel caricamento delle categorie');
        }
        
        const categories = await response.json();
        const select = document.getElementById('editCategoria');
        
        // Pulisci opzioni esistenti (tranne la prima)
        while (select.children.length > 1) {
            select.removeChild(select.lastChild);
        }
        
        // Filtra categorie dello stesso tipo della transazione corrente
        const currentType = transactionData.tipoCategoria;
        const filteredCategories = categories.filter(cat => cat.tipo === currentType);
        
        filteredCategories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = category.nome;
            select.appendChild(option);
        });
        
    } catch (error) {
        console.error('Errore caricamento categorie:', error);
        showNotification('Errore nel caricamento delle categorie', 'error');
    }
}

// Mostra dati transazione
function displayTransaction() {
    const isIncome = transactionData.tipoCategoria === 'Guadagno';
    
    // Header
    const typeBadge = document.getElementById('transactionTypeBadge');
    const typeText = document.getElementById('transactionTypeText');
    const amount = document.getElementById('transactionAmount');
    
    typeBadge.className = `transaction-type-badge ${isIncome ? 'income' : 'expense'}`;
    typeText.textContent = isIncome ? 'Entrata' : 'Uscita';
    
    amount.textContent = `${isIncome ? '+' : '-'}${transactionData.importo.toFixed(2)} €`;
    amount.className = `transaction-amount ${isIncome ? 'positive' : 'negative'}`;
    
    // Meta informazioni
    document.getElementById('transactionAccount').textContent = transactionData.conto || 'N/D';
    document.getElementById('transactionCreatedDate').textContent = formatDate(transactionData.data);
    
    // Form
    document.getElementById('editImporto').value = transactionData.importo;
    document.getElementById('editData').value = formatDateForInput(transactionData.data);
    document.getElementById('editDescrizione').value = transactionData.descrizione || '';
    document.getElementById('editCategoria').value = transactionData.idCategoria;
    
    // Imposta data massima ad ora
    const now = new Date();
    const maxDate = now.toISOString().slice(0, 16); // Format: YYYY-MM-DDTHH:MM
    document.getElementById('editData').max = maxDate;
}

// Gestisce salvataggio transazione
async function handleSaveTransaction(event) {
    event.preventDefault();
    
    if (isEditing) return;
    
    try {
        isEditing = true;
        document.getElementById('saveBtn').disabled = true;
        document.getElementById('saveBtn').textContent = 'Salvataggio...';
        
        const formData = new FormData(event.target);
        const updatedData = {
            id: transactionData.id,
            importo: parseFloat(formData.get('importo')),
            data: formData.get('data'),
            descrizione: formData.get('descrizione') || '',
            idCategoria: parseInt(formData.get('categoria')),
            idConto: transactionData.idConto
        };
        
        // Validazione importo
        if (isNaN(updatedData.importo) || updatedData.importo <= 0) {
            showErrorMessage('L\'importo deve essere un numero maggiore di zero');
            return;
        }
        
        // Controllo limite massimo importo
        if (updatedData.importo > 9999999999999.99) {
            showErrorMessage('L\'importo non può superare i 9.999.999.999.999,99 €');
            return;
        }
        
        // Validazione data
        if (!updatedData.data) {
            showErrorMessage('La data è obbligatoria');
            return;
        }
        
        if (new Date(updatedData.data) > new Date()) {
            showErrorMessage('La data non può essere futura');
            return;
        }
        
        // Validazione descrizione obbligatoria
        if (!updatedData.descrizione || updatedData.descrizione.trim().length === 0) {
            showErrorMessage('La descrizione è obbligatoria');
            return;
        }
        
        // Validazione lunghezza minima descrizione
        if (updatedData.descrizione.trim().length < 3) {
            showErrorMessage('La descrizione deve essere di almeno 3 caratteri');
            return;
        }
        
        // Validazione lunghezza massima descrizione
        if (updatedData.descrizione.trim().length > 500) {
            showErrorMessage('La descrizione non può superare i 500 caratteri');
            return;
        }
        
        // Validazione categoria obbligatoria
        if (isNaN(updatedData.idCategoria) || updatedData.idCategoria <= 0) {
            showErrorMessage('Seleziona una categoria valida');
            return;
        }
        
        // Pulisci la descrizione (trim)
        updatedData.descrizione = updatedData.descrizione.trim();
        
        const response = await fetch('/api/transazione', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(updatedData)
        });
        
        const responseText = await response.text();
        
        if (!response.ok) {
            let errorData;
            try {
                errorData = JSON.parse(responseText);
            } catch (e) {
                throw new Error('Errore del server: ' + responseText);
            }
            throw new Error(errorData.error || 'Errore durante il salvataggio');
        }
        
        window.location.href = '/home?success=transazione-modificata';
        
    } catch (error) {
        console.error('Errore salvataggio:', error);
        showErrorMessage(error.message || 'Errore durante il salvataggio');
    } finally {
        isEditing = false;
        document.getElementById('saveBtn').disabled = false;
        document.getElementById('saveBtn').textContent = 'Salva Modifiche';
    }
}

// Gestisce annullamento modifiche
function handleCancelEdit() {
    // ✅ CAMBIATO - Torna direttamente alla home
    window.location.href = '/home';
}

// Mostra modal eliminazione (SEMPLIFICATO)
function showDeleteModal() {
    const modal = document.getElementById('deleteModal');
    modal.style.display = 'block';
}

// Chiudi modal eliminazione
function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
}

// Conferma eliminazione
async function confirmDelete() {
    try {
        document.getElementById('confirmDeleteBtn').disabled = true;
        document.getElementById('confirmDeleteBtn').textContent = 'Eliminazione...';
        
        const response = await fetch(`/api/transazione?id=${transactionData.id}&idConto=${transactionData.idConto}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Errore durante l\'eliminazione');
        }
        
        closeDeleteModal();
        
        // ✅ CAMBIATO - Vai alla home con notifica di successo
        window.location.href = '/home?success=transazione-eliminata';
        
    } catch (error) {
        console.error('Errore eliminazione:', error);
        showNotification(error.message, 'error');
    } finally {
        document.getElementById('confirmDeleteBtn').disabled = false;
        document.getElementById('confirmDeleteBtn').textContent = 'Elimina';
    }
}

// Utility functions
function showLoading(show) {
    const loading = document.getElementById('loadingMessage');
    const main = document.getElementById('mainContent');
    const error = document.getElementById('errorMessage');
    
    if (show) {
        loading.style.display = 'block';
        main.style.display = 'none';
        error.style.display = 'none';
    } else {
        loading.style.display = 'none';
    }
}

function showMainContent() {
    document.getElementById('mainContent').style.display = 'block';
    document.getElementById('loadingMessage').style.display = 'none';
    document.getElementById('errorMessage').style.display = 'none';
}

function showError(message) {
    document.getElementById('errorText').textContent = message;
    document.getElementById('errorMessage').style.display = 'block';
    document.getElementById('loadingMessage').style.display = 'none';
    document.getElementById('mainContent').style.display = 'none';
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

function formatDateForInput(dateString) {
    const date = new Date(dateString);
    return date.toISOString().slice(0, 16); // Format: YYYY-MM-DDTHH:MM
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

// Setup sidebar (stesso codice delle altre pagine)
function setupSidebar() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    const closeSidebar = document.getElementById('closeSidebar');
    
    hamburgerMenu.addEventListener('click', function() {
        sidebar.classList.add('open');
        sidebarOverlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    });
    
    closeSidebar.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });
    
    sidebarOverlay.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });
    
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            sidebarOverlay.classList.remove('show');
            document.body.style.overflow = '';
        }
    });
}

function showSuccessMessage(message) {
    showNotification(message, 'success');
}

function showErrorMessage(message) {
    showNotification(message, 'error');
}

function hideNotification(notification) {
    notification.classList.remove('show');
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 300);
}