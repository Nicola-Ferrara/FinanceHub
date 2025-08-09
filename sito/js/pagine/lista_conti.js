// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    fetchConti();
    setupSidebar(); // ✅ RIMETTI
    
    // Listener per il bottone aggiungi conto
    const addAccountBtn = document.getElementById('addAccountBtn');
    if (addAccountBtn) {
        addAccountBtn.addEventListener('click', () => {
            console.log("Bottone cliccato!"); // ✅ DEBUG
            window.location.href = '/aggiungiConto';
        });
    } else {
        console.error("Bottone addAccountBtn non trovato!"); // ✅ DEBUG
    }
    
    checkUrlParams();
});

// ...resto del codice esistente rimane identico...

// Funzione per recuperare tutti i conti dal server
async function fetchConti() {
    try {
        const response = await fetch("/api/conti");
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei conti");
        }

        const accounts = await response.json();
        renderAccounts(accounts);
        
    } catch (error) {
        console.error("Errore:", error);
        showNotification('Errore durante il caricamento dei conti', 'error');
    }
}

// Funzione per renderizzare i conti
function renderAccounts(accounts) {
    const accountsGrid = document.getElementById("accountsGrid");
    const emptyMessage = document.getElementById("emptyMessage");
    
    accountsGrid.innerHTML = "";
    
    if (accounts.length === 0) {
        emptyMessage.style.display = "block";
        return;
    }
    
    emptyMessage.style.display = "none";
    
    accounts.forEach(account => {
        const accountCard = createAccountCard(account);
        accountsGrid.appendChild(accountCard);
    });
}

// Funzione per creare una card conto
function createAccountCard(account) {
    const card = document.createElement("div");
    card.className = "account-card";
    card.setAttribute('data-account-id', account.id);
    
    // Determina la classe del saldo
    let balanceClass = 'zero';
    if (account.saldo > 0) {
        balanceClass = 'positive';
    } else if (account.saldo < 0) {
        balanceClass = 'negative';
    }
    
    // Formatta il saldo
    const formattedBalance = account.saldo.toFixed(2);
    const balanceSign = account.saldo >= 0 ? '+' : '';
    
    card.innerHTML = `
        <div class="account-header">
            <div class="account-info">
                <h3>${account.nome}</h3>
                <span class="account-type">${account.tipo}</span>
            </div>
        </div>
        <div class="account-balance ${balanceClass}">
            ${balanceSign}${formattedBalance} €
        </div>
    `;
    
    // Aggiungi click listener per navigare al singolo conto
    card.addEventListener('click', function() {
        // Feedback visivo
        this.style.transform = 'scale(0.98)';
        setTimeout(() => {
            this.style.transform = '';
            window.location.href = `/conti?id=${account.id}`;
        }, 100);
    });
    
    // Aggiungi hover effect per accessibilità
    card.setAttribute('role', 'button');
    card.setAttribute('tabindex', '0');
    card.setAttribute('aria-label', `Visualizza dettagli conto ${account.nome}`);
    
    card.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            this.click();
        }
    });
    
    return card;
}

// Controllo parametri URL per notifiche
function checkUrlParams() {
    const urlParams = new URLSearchParams(window.location.search);
    
    if (urlParams.get('success') === 'conto-aggiunto') {
        showNotification('Conto aggiunto con successo!', 'success');
        
        // Rimuovi il parametro dall'URL
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    } else if (urlParams.get('success') === 'conto-eliminato') {
        showNotification('Conto eliminato con successo!', 'success');
        
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    } else if (urlParams.get('success') === 'conto-modificato') {
        showNotification('Conto modificato con successo!', 'success');
        
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
}