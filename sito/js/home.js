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
        document.getElementById("netBalance").textContent = bilancioNetto.toFixed(2);
        
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
        type: "pie",
        data: {
            labels: ["Entrate", "Uscite"],
            datasets: [{
                data: [entrate, uscite],
                backgroundColor: ["#4caf50", "#f44336"],
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: "bottom",
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
        accountsList.innerHTML = ""; // Pulisci la lista prima di aggiungere nuovi elementi
        
        if (accounts.length === 0) {
            const li = document.createElement("li");
            li.textContent = "Nessun conto disponibile";
            accountsList.appendChild(li);
        } else {
            accounts.forEach(account => {
                const li = document.createElement("li");
                li.textContent = `${account.nome} (${account.tipo}): ${account.saldo.toFixed(2)} €`;
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
                details.innerHTML = `
                    <span>${operazione.descrizione}</span>
                    <span class="transaction-date">${operazione.data}</span>
                    <span class="transaction-category">${operazione.categoria} - ${operazione.conto}</span>
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