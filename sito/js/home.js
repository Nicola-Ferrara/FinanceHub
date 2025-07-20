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

// Funzione per recuperare i dati del bilancio dal server
async function fetchBilancio() {
    try {
        const response = await fetch("/api/bilancio");
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei dati");
        }

        const data = await response.json();
        const { entrate, uscite } = data;

        // Calcola il bilancio netto
        const bilancioNetto = entrate - uscite;

        // Aggiorna i dati nella pagina
        document.getElementById("totalIncome").textContent = entrate.toFixed(2);
        document.getElementById("totalExpenses").textContent = uscite.toFixed(2);
        
        // Imposta il colore del totale in base al segno
        const netBalanceElement = document.getElementById("netBalance");
        netBalanceElement.textContent = bilancioNetto.toFixed(2);
        netBalanceElement.className = bilancioNetto >= 0 ? "positive" : "negative";
        
        // Aggiorna o crea il grafico
        createOrUpdateChart(entrate, uscite);
    } catch (error) {
        console.error("Errore:", error);
    }
}

// Funzione per creare o aggiornare il grafico
function createOrUpdateChart(entrate, uscite) {
    const ctx = document.getElementById("balanceChart").getContext("2d");
    
    // Se il grafico esiste già, distruggilo
    if (chartInstance) {
        chartInstance.destroy();
    }
    
    // Crea un nuovo grafico
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

// Funzione per recuperare i conti dal server
async function fetchConti() {
    try {
        const response = await fetch("/api/conti");
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei conti");
        }

        const accounts = await response.json();
        
        // Carica la lista dei conti
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
        
        // Carica la lista delle operazioni
        const transactionsList = document.getElementById("transactionsList");
        transactionsList.innerHTML = ""; // Pulisci la lista prima di aggiungere nuovi elementi
        
        if (operazioni.length === 0) {
            const li = document.createElement("li");
            li.textContent = "Nessuna operazione disponibile";
            transactionsList.appendChild(li);
        } else {
            operazioni.forEach(operazione => {
                const li = document.createElement("li");

                // Indicatore di tipo (spesa, guadagno o trasferimento)
                const indicator = document.createElement("div");
                indicator.classList.add("transaction-indicator");
                
                // Determina il colore in base al tipo
                if (operazione.tipo === "Guadagno") {
                    indicator.classList.add("income");
                } else if (operazione.tipo === "Spesa") {
                    indicator.classList.add("expense");
                } else if (operazione.tipo === "Trasferimento") {
                    indicator.classList.add("transfer");
                }

                // Dettagli dell'operazione
                const details = document.createElement("div");
                details.classList.add("transaction-details");
                
                // Per i trasferimenti, mostra "Destinatario ← Mittente"
                let categoriaText = operazione.categoria;
                if (operazione.tipo === "Trasferimento") {
                    // Assumendo che il server invii i nomi dei conti nel campo categoria
                    categoriaText = operazione.categoria; // Il server dovrà essere modificato per inviare "ContoA ← ContoB"
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
    return date.toLocaleDateString('it-IT', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    // Aggiorna il titolo del bilancio con il mese corrente
    const balanceTitle = document.getElementById("balanceTitle");
    if (balanceTitle) {
        balanceTitle.textContent = `Bilancio ${meseCorrente} ${annoCorrente}`;
    }
    
    // Carica tutti i dati
    fetchBilancio();
    fetchConti();
    fetchOperazioni();
});

// Listener per il bottone aggiungi conto
document.addEventListener("DOMContentLoaded", function() {
    // ... codice esistente ...
    
    // Aggiungi questo listener
    const addAccountBtn = document.getElementById('addAccountBtn');
    if (addAccountBtn) {
        addAccountBtn.addEventListener('click', () => {
            window.location.href = '/aggiungiConto';
        });
    }
});