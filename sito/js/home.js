// Ottieni il mese e l'anno corrente
const now = new Date();
const mesi = [
    "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
    "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
];
const meseCorrente = mesi[now.getMonth()];
const annoCorrente = now.getFullYear();

// Variabili globali per i dati del bilancio
let chartInstance = null;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    const balanceTitle = document.getElementById("balanceTitle");
    if (balanceTitle) {
        balanceTitle.textContent = `Bilancio ${meseCorrente} ${annoCorrente}`;
    }
    
    fetchBilancio();
    fetchConti();
    fetchOperazioni();
    fetchCategorie(); // ✅ MANTIENI - Solo per visualizzazione
    setupBalanceClickListener();
    setupCategoriesClickListener();
    setupAccountsClickListener();
    setupTransactionsClickListener();
    setupSidebar();
    
    // Listener per il bottone aggiungi conto
    const addAccountBtn = document.getElementById('addAccountBtn');
    if (addAccountBtn) {
        addAccountBtn.addEventListener('click', () => {
            window.location.href = '/aggiungiConto';
        });
    }
    
    checkUrlParams();
});

// Funzione per recuperare i dati del bilancio dal server
async function fetchBilancio() {
    try {
        const response = await fetch("/api/bilancio");
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei dati");
        }

        const data = await response.json();
        const { entrate, uscite } = data;

        const bilancioNetto = entrate - uscite;

        document.getElementById("totalIncome").textContent = entrate.toFixed(2);
        document.getElementById("totalExpenses").textContent = uscite.toFixed(2);
        
        const netBalanceElement = document.getElementById("netBalance");
        netBalanceElement.textContent = bilancioNetto.toFixed(2);
        netBalanceElement.className = bilancioNetto >= 0 ? "positive" : "negative";
        
        createOrUpdateChart(entrate, uscite);
    } catch (error) {
        console.error("Errore:", error);
    }
}

// Funzione per creare il grafico a torta
function createOrUpdateChart(entrate, uscite) {
    const ctx = document.getElementById("balanceChart").getContext("2d");
    const chartContainer = document.getElementById("balanceChart");
    const balanceContainer = document.querySelector('.balance-container');
    
    if (entrate === 0 && uscite === 0) {
        chartContainer.style.display = 'none';

        if (chartInstance) {
            chartInstance.destroy();
            chartInstance = null;
        }
        
        balanceContainer.style.justifyContent = 'center';
        return;
    }
    
    chartContainer.style.display = 'block';
    balanceContainer.style.justifyContent = 'space-between';
    
    if (chartInstance) {
        chartInstance.destroy();
    }
    
    chartInstance = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Entrate", "Uscite"],
            datasets: [{
                data: [entrate, uscite],
                backgroundColor: ["#28a745", "#dc3545"],
                borderWidth: 0,
                cutout: "60%"
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: "bottom",
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

// Funzione per recuperare i conti dell'utente
async function fetchConti() {
    try {
        const response = await fetch("/api/conti");
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei conti");
        }

        const accounts = await response.json();
        
        const accountsList = document.getElementById("accountsList");
        accountsList.innerHTML = ""; 
        
        if (accounts.length === 0) {
            const li = document.createElement("li");
            li.textContent = "Nessun conto disponibile";
            accountsList.appendChild(li);
        } else {
            accounts.forEach(account => {
                const li = document.createElement("li");
                
                li.style.cursor = "pointer";
                li.addEventListener("click", function() {
                    window.location.href = `/conti?id=${account.id}`;
                });
                
                const accountInfo = document.createElement("div");
                accountInfo.className = "account-info";
                
                const accountDetails = document.createElement("div");
                accountDetails.className = "account-details";
                
                const accountName = document.createElement("h3");
                accountName.textContent = account.nome;
                
                const accountType = document.createElement("p");
                accountType.textContent = account.tipo;
                
                accountDetails.appendChild(accountName);
                accountDetails.appendChild(accountType);
                
                const accountBalance = document.createElement("div");
                accountBalance.className = `account-balance ${account.saldo >= 0 ? "positive" : "negative"}`;
                accountBalance.textContent = `${account.saldo.toFixed(2)} €`;
                
                accountInfo.appendChild(accountDetails);
                accountInfo.appendChild(accountBalance);
                li.appendChild(accountInfo);
                
                accountsList.appendChild(li);
            });
        }
    } catch (error) {
        console.error("Errore:", error);
        const accountsList = document.getElementById("accountsList");
        accountsList.innerHTML = "<li>Errore durante il caricamento dei conti</li>";
    }
}

// Funzione per recuperare le operazioni dal server
async function fetchOperazioni() {
    try {
        const response = await fetch("/api/operazioni-home");
        if (!response.ok) {
            throw new Error("Errore durante il recupero delle operazioni");
        }

        const operazioni = await response.json();
        
        const transactionsList = document.getElementById("transactionsList");
        transactionsList.innerHTML = "";
        
        if (operazioni.length === 0) {
            const li = document.createElement("li");
            li.textContent = "Nessuna operazione disponibile";
            transactionsList.appendChild(li);
        } else {
            operazioni.forEach(operazione => {
                const li = document.createElement("li");

                const indicator = document.createElement("div");
                indicator.classList.add("transaction-indicator");
                
                if (operazione.tipo === "Guadagno") {
                    indicator.classList.add("income");
                } else if (operazione.tipo === "Spesa") {
                    indicator.classList.add("expense");
                } else if (operazione.tipo === "Trasferimento") {
                    indicator.classList.add("transfer");
                }

                const details = document.createElement("div");
                details.classList.add("transaction-details");
                
                let categoriaText = operazione.categoria;
                if (operazione.tipo === "Trasferimento") {
                    categoriaText = operazione.categoria;
                }
                
                details.innerHTML = `
                    <span>${operazione.descrizione}</span>
                    <span class="transaction-date">${formatDate(operazione.data)}</span>
                    <span class="transaction-category">${categoriaText}</span>
                `;

                const amount = document.createElement("span");
                amount.classList.add("transaction-amount");
                
                if (operazione.tipo === "Guadagno") {
                    amount.classList.add("income");
                    amount.textContent = `+${operazione.importo.toFixed(2)} €`;
                } else if (operazione.tipo === "Spesa") {
                    amount.classList.add("expense");
                    amount.textContent = `-${operazione.importo.toFixed(2)} €`;
                } else if (operazione.tipo === "Trasferimento") {
                    amount.classList.add("transfer");
                    amount.textContent = `${operazione.importo.toFixed(2)} €`;
                }

                li.appendChild(indicator);
                li.appendChild(details);
                li.appendChild(amount);
                transactionsList.appendChild(li);
            });
        }
    } catch (error) {
        console.error("Errore:", error);
        const transactionsList = document.getElementById("transactionsList");
        transactionsList.innerHTML = "<li>Errore durante il caricamento delle operazioni</li>";
    }
}

// ✅ MANTIENI - Solo per visualizzazione categorie
async function fetchCategorie() {
    try {
        const incomeResponse = await fetch("/api/categorie/guadagno");
        const expenseResponse = await fetch("/api/categorie/spesa");
        
        if (!incomeResponse.ok || !expenseResponse.ok) {
            throw new Error("Errore durante il recupero delle categorie");
        }

        const incomeCategories = await incomeResponse.json();
        const expenseCategories = await expenseResponse.json();
        
        // Carica le categorie di guadagno
        const incomeCategoriesList = document.getElementById("incomeCategories");
        incomeCategoriesList.innerHTML = "";
        
        if (incomeCategories.length === 0) {
            const li = document.createElement("li");
            li.className = "empty-categories";
            li.textContent = "Nessuna categoria di guadagno";
            incomeCategoriesList.appendChild(li);
        } else {
            incomeCategories.forEach(categoria => {
                const li = document.createElement("li");
                
                const categoryItem = document.createElement("div");
                categoryItem.className = "category-item";
                
                const categoryIcon = document.createElement("div");
                categoryIcon.className = "category-icon income";
                
                const categoryName = document.createElement("span");
                categoryName.className = "category-name";
                categoryName.textContent = categoria.nome;
                
                categoryItem.appendChild(categoryIcon);
                categoryItem.appendChild(categoryName);
                li.appendChild(categoryItem);
                
                incomeCategoriesList.appendChild(li);
            });
        }
        
        // Carica le categorie di spesa
        const expenseCategoriesList = document.getElementById("expenseCategories");
        expenseCategoriesList.innerHTML = "";
        
        if (expenseCategories.length === 0) {
            const li = document.createElement("li");
            li.className = "empty-categories";
            li.textContent = "Nessuna categoria di spesa";
            expenseCategoriesList.appendChild(li);
        } else {
            expenseCategories.forEach(categoria => {
                const li = document.createElement("li");
                
                const categoryItem = document.createElement("div");
                categoryItem.className = "category-item";
                
                const categoryIcon = document.createElement("div");
                categoryIcon.className = "category-icon expense";
                
                const categoryName = document.createElement("span");
                categoryName.className = "category-name";
                categoryName.textContent = categoria.nome;
                
                categoryItem.appendChild(categoryIcon);
                categoryItem.appendChild(categoryName);
                li.appendChild(categoryItem);
                
                expenseCategoriesList.appendChild(li);
            });
        }
        
    } catch (error) {
        console.error("Errore:", error);
        document.getElementById("incomeCategories").innerHTML = '<li class="empty-categories">Errore durante il caricamento</li>';
        document.getElementById("expenseCategories").innerHTML = '<li class="empty-categories">Errore durante il caricamento</li>';
    }
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
        hour12: false
    });
    
    return `${dateFormatted} - ${timeFormatted}`;
}

// Controllo notifica
function checkUrlParams() {
    const urlParams = new URLSearchParams(window.location.search);
    
    if (urlParams.get('success') === 'conto-aggiunto') {
        showNotification('Conto aggiunto con successo!', 'success');
        
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    } else if (urlParams.get('success') === 'conto-eliminato') {
        showNotification('Conto eliminato con successo!', 'success');
        
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
}

// Funzione per mostrare notifiche
function showNotification(message, type) {
    const existingNotifications = document.querySelectorAll('.notification');
    existingNotifications.forEach(notif => notif.remove());
    
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <span class="notification-icon">${type === 'success' ? '✅' : '❌'}</span>
            <span class="notification-message">${message}</span>
            <button class="notification-close">&times;</button>
        </div>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    setTimeout(() => {
        hideNotification(notification);
    }, 3000);
    
    notification.querySelector('.notification-close').addEventListener('click', () => {
        hideNotification(notification);
    });
}

// Funzione per rimuovere la notifica
function hideNotification(notification) {
    notification.classList.remove('show');
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 300);
}

function setupBalanceClickListener() {
    const balanceSection = document.getElementById('balanceSection');
    if (balanceSection) {
        balanceSection.addEventListener('click', function() {
            this.style.transform = 'scale(0.98)';
            setTimeout(() => {
                this.style.transform = '';
                window.location.href = '/bilanci';
            }, 150);
        });
        
        balanceSection.setAttribute('role', 'button');
        balanceSection.setAttribute('tabindex', '0');
        balanceSection.setAttribute('aria-label', 'Visualizza tutti i bilanci');
        
        balanceSection.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.click();
            }
        });
    }
}

function setupCategoriesClickListener() {
    const categoriesSection = document.getElementById('categoriesSection');
    if (categoriesSection) {
        categoriesSection.addEventListener('click', function(e) {
            // Aggiungi feedback visivo
            this.style.transform = 'scale(0.98)';
            setTimeout(() => {
                this.style.transform = '';
                // Naviga alla pagina Categorie
                window.location.href = '/categorie';
            }, 150);
        });
        
        categoriesSection.setAttribute('role', 'button');
        categoriesSection.setAttribute('tabindex', '0');
        categoriesSection.setAttribute('aria-label', 'Gestisci tutte le categorie');
        
        categoriesSection.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.click();
            }
        });
    }
}

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

function setupAccountsClickListener() {
    const accountsSection = document.getElementById('accountsSection');
    if (accountsSection) {
        accountsSection.addEventListener('click', function(e) {
            // Previeni il click se l'utente ha cliccato su un singolo conto o sul bottone aggiungi
            if (e.target.closest('.accounts-list li') || e.target.closest('.add-account-btn')) {
                return;
            }
            
            // Aggiungi feedback visivo
            this.style.transform = 'scale(0.98)';
            setTimeout(() => {
                this.style.transform = '';
                // Naviga alla pagina lista conti
                window.location.href = '/lista_conti';
            }, 150);
        });
        
        accountsSection.setAttribute('role', 'button');
        accountsSection.setAttribute('tabindex', '0');
        accountsSection.setAttribute('aria-label', 'Visualizza tutti i conti');
        
        accountsSection.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.click();
            }
        });
    }
}

function setupTransactionsClickListener() {
    const transactionsSection = document.querySelector('.transactions-section');
    if (transactionsSection) {
        transactionsSection.addEventListener('click', function(e) {
            
            // Aggiungi feedback visivo
            this.style.transform = 'scale(0.98)';
            setTimeout(() => {
                this.style.transform = '';
                window.location.href = '/operazioni';
            }, 150);
        });
        
        transactionsSection.setAttribute('role', 'button');
        transactionsSection.setAttribute('tabindex', '0');
        transactionsSection.setAttribute('aria-label', 'Visualizza tutte le operazioni');
        
        transactionsSection.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.click();
            }
        });
    }
}