// Variabili globali
let isSubmitting = false;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupSidebar();
    setupEventListeners();
});

// Imposta i listener per gli eventi
function setupEventListeners() {
    const form = document.getElementById('addAccountForm');
    const cancelBtn = document.getElementById('cancelBtn');
    
    form.addEventListener('submit', handleFormSubmit);
    cancelBtn.addEventListener('click', () => window.location.href = '/home');
    
    // Formattazione automatica del saldo
    const balanceInput = document.getElementById('initialBalance');
    balanceInput.addEventListener('input', formatBalance);
}

// ✅ VALIDAZIONI IDENTICHE A CONTO.JS
async function handleFormSubmit(event) {
    event.preventDefault();
    
    if (isSubmitting) return;
    
    const formData = new FormData(event.target);
    const accountData = {
        nome: formData.get('nome').trim(),
        tipo: formData.get('tipo'),
        saldo: parseFloat(formData.get('saldo')),
        visibilita: document.getElementById('visibilita').checked.toString()
    };
    
    // ✅ VALIDAZIONI PERSONALIZZATE IDENTICHE
    
    // Validazione nome conto
    if (!accountData.nome || accountData.nome.length === 0) {
        showNotification('Inserisci il nome del conto', 'error');
        return;
    }
    
    if (accountData.nome.length < 2) {
        showNotification('Il nome del conto deve essere di almeno 2 caratteri', 'error');
        return;
    }
    
    if (accountData.nome.length > 20) {
        showNotification('Il nome del conto non può superare i 20 caratteri', 'error');
        return;
    }
    
    // Validazione tipo conto
    if (!accountData.tipo || accountData.tipo === '') {
        showNotification('Seleziona il tipo di conto', 'error');
        return;
    }
    
    // Validazione saldo iniziale
    if (isNaN(accountData.saldo)) {
        showNotification('Inserisci un saldo iniziale valido', 'error');
        return;
    }
    
    // ✅ CONTROLLO LIMITE DATABASE PostgreSQL NUMERIC(15,2) - IDENTICO
    if (accountData.saldo > 9999999999999.99) {
        showNotification(`Il saldo iniziale non può superare i 9999999999999.99 €`, 'error');
        return;
    }
    
    if (accountData.saldo < -9999999999999.99) {
        showNotification(`Il saldo iniziale non può essere inferiore a -9999999999999.99 €`, 'error');
        return;
    }
    
    try {
        isSubmitting = true;
        showLoading(true);
        
        const response = await fetch('/api/conto', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(accountData)
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.error || 'Errore durante l\'aggiunta del conto');
        }

        window.location.href = '/home?success=conto-aggiunto';
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification(error.message || 'Errore durante l\'aggiunta del conto', 'error');
    } finally {
        isSubmitting = false;
        showLoading(false);
    }
}

// ✅ FORMATTAZIONE SALDO
function formatBalance(event) {
    const input = event.target;
    let value = input.value;
    
    let cleanValue = value.replace(/[^0-9.,-]/g, '');
    
    cleanValue = cleanValue.replace(',', '.');
    
    // ✅ GESTISCI IL SEGNO MENO (solo all'inizio)
    let isNegative = cleanValue.startsWith('-');
    if (isNegative) {
        cleanValue = cleanValue.substring(1);
    }
    
    // Gestisci multipli punti decimali
    const parts = cleanValue.split('.');
    if (parts.length > 2) {
        cleanValue = parts[0] + '.' + parts.slice(1).join('');
    }
    
    // Limita a 2 decimali
    if (parts.length === 2 && parts[1].length > 2) {
        cleanValue = parts[0] + '.' + parts[1].substring(0, 2);
    }
    
    // ✅ RIAPPLICA IL SEGNO MENO se necessario
    if (isNegative && cleanValue !== '' && cleanValue !== '0') {
        cleanValue = '-' + cleanValue;
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

// Mostra/nasconde loading
function showLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    const submitBtn = document.querySelector('.btn-primary');
    
    if (show) {
        if (overlay) overlay.style.display = 'flex';
        submitBtn.disabled = true;
        submitBtn.textContent = 'Aggiunta in corso...';
    } else {
        if (overlay) overlay.style.display = 'none';
        submitBtn.disabled = false;
        submitBtn.textContent = 'Aggiungi Conto';
    }
}