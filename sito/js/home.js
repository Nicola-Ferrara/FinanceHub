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
    fetchCategorie();
    setupCategoryListeners();
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
        const response = await fetch("/api/operazioni");
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

                // Importo dell'operazione
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

                // Aggiungi gli elementi alla lista
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

// Listener per il bottone aggiungi conto
document.addEventListener("DOMContentLoaded", function() {
    const addAccountBtn = document.getElementById('addAccountBtn');
    if (addAccountBtn) {
        addAccountBtn.addEventListener('click', () => {
            window.location.href = '/aggiungiConto';
        });
    }
    checkUrlParams();
});

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

// Funzione per recuperare le categorie dal server
async function fetchCategorie() {
    try {
        // Recupera le categorie di guadagno
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

// Setup listeners per i modali delle categorie
function setupCategoryListeners() {
    // Bottoni per aprire i modali
    document.getElementById('addCategoryBtn').addEventListener('click', openAddCategoryModal);
    document.getElementById('deleteCategoryBtn').addEventListener('click', openDeleteCategoryModal);
    
    // Form submissions
    document.getElementById('addCategoryForm').addEventListener('submit', handleAddCategorySubmit);
    document.getElementById('deleteCategoryForm').addEventListener('submit', handleDeleteCategorySubmit);
    
    // Bottoni di chiusura
    document.getElementById('cancelAddCategory').addEventListener('click', closeAddCategoryModal);
    document.getElementById('cancelDeleteCategory').addEventListener('click', closeDeleteCategoryModal);
    
    // Listener per il cambio di tipo nella eliminazione
    document.getElementById('deleteCategoryType').addEventListener('change', handleDeleteTypeChange);
    document.getElementById('categorySelect').addEventListener('change', handleCategorySelectChange);
    
    // Chiudi modali con X e click fuori
    setupModalCloseListeners();
}

// Apri modale aggiungi categoria
function openAddCategoryModal() {
    document.getElementById('addCategoryForm').reset();
    document.getElementById('addCategoryModal').style.display = 'block';
}

// Chiudi modale aggiungi categoria
function closeAddCategoryModal() {
    document.getElementById('addCategoryModal').style.display = 'none';
}

// Apri modale elimina categoria
function openDeleteCategoryModal() {
    document.getElementById('deleteCategoryForm').reset();
    document.getElementById('categorySelectGroup').style.display = 'none';
    document.getElementById('deleteWarning').style.display = 'none';
    document.getElementById('confirmDeleteCategory').disabled = true;
    document.getElementById('deleteCategoryModal').style.display = 'block';
}

// Chiudi modale elimina categoria
function closeDeleteCategoryModal() {
    document.getElementById('deleteCategoryModal').style.display = 'none';
}

// Gestisci cambio tipo per eliminazione
function handleDeleteTypeChange() {
    const typeSelect = document.getElementById('deleteCategoryType');
    const categoryGroup = document.getElementById('categorySelectGroup');
    const categorySelect = document.getElementById('categorySelect');
    
    if (typeSelect.value) {
        categoryGroup.style.display = 'block';
        loadCategoriesForDelete(typeSelect.value);
    } else {
        categoryGroup.style.display = 'none';
        document.getElementById('deleteWarning').style.display = 'none';
        document.getElementById('confirmDeleteCategory').disabled = true;
    }
}

// Gestisci selezione categoria per eliminazione
function handleCategorySelectChange() {
    const categorySelect = document.getElementById('categorySelect');
    const deleteWarning = document.getElementById('deleteWarning');
    const confirmButton = document.getElementById('confirmDeleteCategory');
    const typeSelect = document.getElementById('deleteCategoryType'); // ✅ AGGIUNGI QUESTO
    
    if (categorySelect.value) {
        deleteWarning.style.display = 'block';
        confirmButton.disabled = false;
        
        // ✅ AGGIORNA IL MESSAGGIO ANCHE QUI (per sicurezza)
        if (typeSelect.value === 'Spesa') {
            deleteWarning.innerHTML = "⚠️ Attenzione: Eliminando questa categoria, tutte le transazioni associate verranno riassegnate alla categoria 'Spesa'.";
        } else if (typeSelect.value === 'Guadagno') {
            deleteWarning.innerHTML = "⚠️ Attenzione: Eliminando questa categoria, tutte le transazioni associate verranno riassegnate alla categoria 'Guadagno'.";
        }
    } else {
        deleteWarning.style.display = 'none';
        confirmButton.disabled = true;
    }
}

// Carica categorie per eliminazione
async function loadCategoriesForDelete(tipo) {
    try {
        const endpoint = tipo === 'Spesa' ? '/api/categorie/spesa' : '/api/categorie/guadagno';
        const response = await fetch(endpoint);
        
        if (!response.ok) {
            throw new Error('Errore durante il caricamento delle categorie');
        }
        
        const categorie = await response.json();
        const categorySelect = document.getElementById('categorySelect');
        
        // Svuota le opzioni esistenti
        categorySelect.innerHTML = '<option value="">Seleziona una categoria</option>';
        
        // Filtra le categorie eliminabili (escludi ID 1 e 2)
        const categorieEliminabili = categorie.filter(categoria => categoria.id !== 1 && categoria.id !== 2);
        
        if (categorieEliminabili.length === 0) {
            const option = document.createElement('option');
            option.value = '';
            option.textContent = 'Nessuna categoria eliminabile';
            option.disabled = true;
            categorySelect.appendChild(option);
        } else {
            categorieEliminabili.forEach(categoria => {
                const option = document.createElement('option');
                option.value = categoria.id;
                option.textContent = categoria.nome;
                categorySelect.appendChild(option);
            });
        }
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification('Errore durante il caricamento delle categorie', 'error');
    }
}

// Gestisci submit aggiungi categoria
async function handleAddCategorySubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const categoryData = {
        nome: formData.get('nome').trim(),
        tipo: formData.get('tipo')
    };
    
    // Validazione
    if (!categoryData.nome) {
        showNotification('Inserisci il nome della categoria', 'error');
        return;
    }
    
    if (!categoryData.tipo) {
        showNotification('Seleziona il tipo di categoria', 'error');
        return;
    }
    
    try {
        const response = await fetch('/api/categoria', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(categoryData)
        });
        
        if (!response.ok) {
            throw new Error('Errore durante l\'aggiunta della categoria');
        }
        
        // Chiudi il modale
        closeAddCategoryModal();
        
        // ✅ RICARICA TUTTE LE CATEGORIE
        await fetchCategorie();
        
        // Mostra messaggio di successo
        showNotification('Categoria aggiunta con successo!', 'success');
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification('Errore durante l\'aggiunta della categoria', 'error');
    }
}

// Gestisci submit elimina categoria
async function handleDeleteCategorySubmit(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const categoriaId = parseInt(formData.get('categoria'));
    
    if (!categoriaId) {
        showNotification('Seleziona una categoria da eliminare', 'error');
        return;
    }
    
    try {
        const response = await fetch(`/api/categoria/${categoriaId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            const result = await response.json();
            throw new Error(result.error || 'Errore durante l\'eliminazione della categoria');
        }
        
        // Chiudi il modale
        closeDeleteCategoryModal();
        
        // ✅ RICARICA TUTTI I DATI DOPO L'ELIMINAZIONE
        await Promise.all([
            fetchCategorie(),
            fetchBilancio(),
            fetchOperazioni(),
            fetchConti()
        ]);
        
        // Mostra messaggio di successo
        showNotification('Categoria eliminata con successo!', 'success');
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification(error.message || 'Errore durante l\'eliminazione della categoria', 'error');
    }
}

// Setup listeners per chiudere modali
function setupModalCloseListeners() {
    // Chiudi con X
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', () => {
            closeAddCategoryModal();
            closeDeleteCategoryModal();
        });
    });
    
    // Chiudi cliccando fuori
    window.addEventListener('click', (event) => {
        const addModal = document.getElementById('addCategoryModal');
        const deleteModal = document.getElementById('deleteCategoryModal');
        
        if (event.target === addModal) {
            closeAddCategoryModal();
        }
        if (event.target === deleteModal) {
            closeDeleteCategoryModal();
        }
    });
}