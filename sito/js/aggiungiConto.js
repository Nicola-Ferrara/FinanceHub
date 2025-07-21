// Variabili globali
let isSubmitting = false;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    setupFormValidation();
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

// Gestisce il submit del form
async function handleFormSubmit(event) {
    event.preventDefault();
    
    if (isSubmitting) return;
    
    const formData = new FormData(event.target);
    const accountData = {
        nome: formData.get('nome').trim(),
        tipo: formData.get('tipo'),
        saldo: formData.get('saldo')
    };
    
    // Validazione
    if (!validateForm(accountData)) {
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
        alert('❌ ' + (error.message || 'Errore durante l\'aggiunta del conto'));
    } finally {
        isSubmitting = false;
        showLoading(false);
    }
}

// Validazione del form
function validateForm(data) {
    // Rimuovi messaggi precedenti
    removeMessages();
    
    const errors = [];
    
    if (!data.nome || data.nome.length < 2) {
        errors.push('Il nome del conto deve contenere almeno 2 caratteri');
    }
    
    if (!data.tipo) {
        errors.push('Seleziona un tipo di conto');
    }
    
    const saldo = parseFloat(data.saldo);
    if (isNaN(saldo) || saldo < 0) {
        errors.push('Il saldo deve essere un numero positivo');
    }
    
    if (errors.length > 0) {
        showErrorMessage(errors.join('<br>'));
        return false;
    }
    
    return true;
}

// Formatta il campo saldo
function formatBalance(event) {
    const input = event.target;
    let value = input.value;
    const cursorPosition = input.selectionStart;
    const oldLength = value.length;
    
    let cleanValue = value.replace(/[^0-9.,]/g, '');
    
    cleanValue = cleanValue.replace(/,/g, '.');
    
    const parts = cleanValue.split('.');
    if (parts.length > 2) {
        cleanValue = parts[0] + '.' + parts.slice(1).join('');
    }
    
    if (parts.length === 2 && parts[1].length > 2) {
        cleanValue = parts[0] + '.' + parts[1].substring(0, 2);
    }
    if (cleanValue !== value) {
        const newLength = cleanValue.length;
        const lengthDiff = newLength - oldLength;
        
        input.value = cleanValue;
        
        const newCursorPosition = Math.max(0, cursorPosition + lengthDiff);
        input.setSelectionRange(newCursorPosition, newCursorPosition);
    }
}

// Imposta la validazione del form
function setupFormValidation() {
    const inputs = document.querySelectorAll('input[required], select[required]');
    
    inputs.forEach(input => {
        input.addEventListener('blur', validateField);
        input.addEventListener('input', clearFieldError);
    });
}

// Valida un singolo campo
function validateField(event) {
    const field = event.target;
    const value = field.value.trim();
    
    // Rimuovi errori precedenti
    clearFieldError(event);
    
    if (field.hasAttribute('required') && !value) {
        showFieldError(field, 'Questo campo è obbligatorio');
        return;
    }
    
    // Validazioni specifiche
    if (field.name === 'nome' && value.length < 2) {
        showFieldError(field, 'Il nome deve contenere almeno 2 caratteri');
    } else if (field.name === 'saldo') {
        const saldo = parseFloat(value);
        if (isNaN(saldo) || saldo < 0) {
            showFieldError(field, 'Inserisci un importo valido');
        }
    }
}

// Mostra errore per un campo specifico
function showFieldError(field, message) {
    field.style.borderColor = '#dc3545';
    
    // Rimuovi messaggio precedente
    const existingError = field.parentNode.querySelector('.field-error');
    if (existingError) {
        existingError.remove();
    }
    
    // Aggiungi nuovo messaggio
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.style.color = '#dc3545';
    errorDiv.style.fontSize = '12px';
    errorDiv.style.marginTop = '5px';
    errorDiv.textContent = message;
    
    field.parentNode.appendChild(errorDiv);
}

// Rimuove errore da un campo
function clearFieldError(event) {
    const field = event.target;
    field.style.borderColor = '';
    
    const errorDiv = field.parentNode.querySelector('.field-error');
    if (errorDiv) {
        errorDiv.remove();
    }
}

// Mostra/nasconde loading
function showLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    const submitBtn = document.querySelector('.btn-primary');
    
    if (show) {
        overlay.style.display = 'flex';
        submitBtn.disabled = true;
        submitBtn.textContent = 'Aggiunta in corso...';
    } else {
        overlay.style.display = 'none';
        submitBtn.disabled = false;
        submitBtn.textContent = 'Aggiungi Conto';
    }
}

function removeMessages() {
    const messages = document.querySelectorAll('.message');
    messages.forEach(msg => msg.remove());
}