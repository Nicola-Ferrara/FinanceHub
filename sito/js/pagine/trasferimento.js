// Variabili globali
let transferData = null;
let originalData = null;
let isEditing = false;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupEventListeners();
    setupSidebar();
    loadTransfer();
});

// Ottieni ID trasferimento dall'URL
function getTransferIdFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('id');
}

// Setup event listeners
function setupEventListeners() {
    // Form
    document.getElementById('editTransferForm').addEventListener('submit', handleSaveTransfer);
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

// Carica dati trasferimento
async function loadTransfer() {
    try {
        const transferId = getTransferIdFromUrl();
        
        if (!transferId) {
            showError('ID trasferimento mancante nell\'URL');
            return;
        }
        
        showLoading(true);
        
        const response = await fetch(`/api/trasferimento?id=${transferId}`);
        
        if (!response.ok) {
            throw new Error('Trasferimento non trovato');
        }
        
        transferData = await response.json();
        originalData = JSON.parse(JSON.stringify(transferData)); // Deep copy
        
        displayTransfer();
        showMainContent();
        
    } catch (error) {
        console.error('Errore caricamento trasferimento:', error);
        showError(error.message);
    } finally {
        showLoading(false);
    }
}

// Mostra dati trasferimento
function displayTransfer() {
    // Header
    const amount = document.getElementById('transferAmount');
    
    amount.textContent = `${transferData.importo.toFixed(2)} €`;
    amount.className = `transfer-amount`;
    
    // Meta informazioni
    document.getElementById('transferAccount').textContent = (transferData.contoDestinatario || 'N/D') + ' ⭠ ' + (transferData.contoMittente || 'N/D');
    document.getElementById('transferCreatedDate').textContent = formatDate(transferData.data);

    // Form
    document.getElementById('editImporto').value = transferData.importo;
    document.getElementById('editData').value = formatDateForInput(transferData.data);
    document.getElementById('editDescrizione').value = transferData.descrizione || '';
    
    // Imposta data massima ad ora
    const now = new Date();
    const maxDate = now.toISOString().slice(0, 16); // Format: YYYY-MM-DDTHH:MM
    document.getElementById('editData').max = maxDate;
}

// Gestisce salvataggio trasferimento
async function handleSaveTransfer(event) {
    event.preventDefault();
    
    if (isEditing) return;
    
    try {
        isEditing = true;
        document.getElementById('saveBtn').disabled = true;
        document.getElementById('saveBtn').textContent = 'Salvataggio...';
        
        const formData = new FormData(event.target);
        const updatedData = {
            id: transferData.id,
            importo: parseFloat(formData.get('importo')),
            data: formData.get('data'),
            descrizione: formData.get('descrizione') || '',
            idContoMittente: transferData.idContoMittente,
            idContoDestinatario: transferData.idContoDestinatario,

        };
        
        // Validazione importo
        if (isNaN(updatedData.importo) || updatedData.importo <= 0) {
            showNotification('L\'importo deve essere un numero maggiore di zero', 'error');
            return;
        }
        
        // Controllo limite massimo importo
        if (updatedData.importo > 9999999999999.99) {
            showNotification('L\'importo non può superare i 9.999.999.999.999,99 €', 'error');
            return;
        }
        
        // Validazione data
        if (!updatedData.data) {
            showNotification('La data è obbligatoria', 'error');
            return;
        }
        
        if (new Date(updatedData.data) > new Date()) {
            showNotification('La data non può essere futura', 'error');
            return;
        }
        
        // Validazione descrizione obbligatoria
        if (!updatedData.descrizione || updatedData.descrizione.trim().length === 0) {
            showNotification('La descrizione è obbligatoria', 'error');
            return;
        }
        
        // Validazione lunghezza minima descrizione
        if (updatedData.descrizione.trim().length < 3) {
            showNotification('La descrizione deve essere di almeno 3 caratteri', 'error');
            return;
        }
        
        // Validazione lunghezza massima descrizione
        if (updatedData.descrizione.trim().length > 500) {
            showNotification('La descrizione non può superare i 500 caratteri', 'error');
            return;
        }
        
        // Pulisci la descrizione (trim)
        updatedData.descrizione = updatedData.descrizione.trim();
        
        const response = await fetch('/api/trasferimento', {
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
        
        window.location.href = '/home?success=trasferimento-modificato';
        
    } catch (error) {
        console.error('Errore salvataggio:', error);
        showNotification(error.message || 'Errore durante il salvataggio', 'error');
    } finally {
        isEditing = false;
        document.getElementById('saveBtn').disabled = false;
        document.getElementById('saveBtn').textContent = 'Salva Modifiche';
    }
}

// Gestisce annullamento modifiche
function handleCancelEdit() {
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
        
        const response = await fetch(`/api/trasferimento?id=${transferData.id}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Errore durante l\'eliminazione');
        }
        
        closeDeleteModal();
        
        // ✅ CAMBIATO - Vai alla home con notifica di successo
        window.location.href = '/home?success=trasferimento-eliminato';
        
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
    if (!dateString) return '';
    dateString = dateString.replace(' ', 'T').replace('Z', '');
    const match = dateString.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (match) {
        const [, year, month, day, hour, minute] = match;
        return `${year}-${month}-${day}T${hour}:${minute}`;
    }
    return '';
}