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

// Funzione per recuperare i dati dal server
async function fetchBalanceData() {
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

async function fetchAccounts() {
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

// Chiamata iniziale per recuperare i dati
fetchBalanceData();
fetchAccounts();

// Carica la lista dei conti
const accountsList = document.getElementById("accountsList");
accounts.forEach(account => {
    const li = document.createElement("li");
    li.textContent = `${account.nome}: ${account.saldo} €`;
    accountsList.appendChild(li);
});

// Simulazione dati transazioni (da sostituire con una chiamata al server)
const transactions = [
    { data: "2025-07-01", conto: "Conto Corrente", categoria: "Spesa Alimentare", importo: -50 },
    { data: "2025-07-02", conto: "Conto Risparmi", categoria: "Stipendio", importo: 1500 },
    { data: "2025-07-03", conto: "Conto Corrente", categoria: "Abbonamento Palestra", importo: -30 },
    { data: "2025-07-04", conto: "Conto Corrente", categoria: "Vendita Usato", importo: 200 },
];

// Carica la lista delle transazioni
const transactionsList = document.getElementById("transactionsList");
transactions.forEach(transaction => {
    const li = document.createElement("li");

    // Indicatore di tipo (spesa o guadagno)
    const indicator = document.createElement("div");
    indicator.classList.add("transaction-indicator");
    indicator.classList.add(transaction.importo < 0 ? "expense" : "income");

    // Dettagli della transazione
    const details = document.createElement("div");
    details.classList.add("transaction-details");
    details.innerHTML = `
        <span>${transaction.conto}</span>
        <span class="transaction-date">${transaction.data}</span>
        <span class="transaction-category">${transaction.categoria}</span>
    `;

    // Importo della transazione
    const amount = document.createElement("span");
    amount.classList.add("transaction-amount");
    amount.classList.add(transaction.importo < 0 ? "expense" : "income");
    amount.textContent = `${transaction.importo < 0 ? "-" : "+"} ${Math.abs(transaction.importo)} €`;

    // Aggiungi gli elementi alla lista
    li.appendChild(indicator);
    li.appendChild(details);
    li.appendChild(amount);
    transactionsList.appendChild(li);
});