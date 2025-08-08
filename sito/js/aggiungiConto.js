// Variabili globali
let isSubmitting = false;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupSidebar();
    setupEventListeners();
});

function setupSidebar() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    const closeSidebar = document.getElementById('closeSidebar');
    
    // Apri sidebar
    hamburgerMenu.addEventListener('click', function() {
        sidebar.classList.add('open');
        sidebarOverlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    });
    
    // Chiudi sidebar con X
    closeSidebar.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });
    
    // Chiudi sidebar cliccando overlay
    sidebarOverlay.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });
    
    // Chiudi sidebar con ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            sidebarOverlay.classList.remove('show');
            document.body.style.overflow = '';
        }
    });
}

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

// ✅ SISTEMA NOTIFICHE IDENTICO A CONTO.JS
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
        showErrorMessage('Inserisci il nome del conto');
        return;
    }
    
    if (accountData.nome.length < 2) {
        showErrorMessage('Il nome del conto deve essere di almeno 2 caratteri');
        return;
    }
    
    if (accountData.nome.length > 20) {
        showErrorMessage('Il nome del conto non può superare i 20 caratteri');
        return;
    }
    
    // Validazione tipo conto
    if (!accountData.tipo || accountData.tipo === '') {
        showErrorMessage('Seleziona il tipo di conto');
        return;
    }
    
    // Validazione saldo iniziale
    if (isNaN(accountData.saldo)) {
        showErrorMessage('Inserisci un saldo iniziale valido');
        return;
    }
    
    // ✅ CONTROLLO LIMITE DATABASE PostgreSQL NUMERIC(15,2) - IDENTICO
    if (accountData.saldo > 9999999999999.99) {
        showErrorMessage(`Il saldo iniziale non può superare i 9999999999999.99 €`);
        return;
    }
    
    if (accountData.saldo < -9999999999999.99) {
        showErrorMessage(`Il saldo iniziale non può essere inferiore a -9999999999999.99 €`);
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
        showErrorMessage(error.message || 'Errore durante l\'aggiunta del conto');
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