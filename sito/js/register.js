// Gestione del pulsante Mostra/Nascondi password
document.getElementById("togglePassword").addEventListener("click", function() {
    const passwordField = document.getElementById("password");
    const eyeIcon = document.getElementById("eyeIcon");
    const type = passwordField.getAttribute("type") === "password" ? "text" : "password";
    passwordField.setAttribute("type", type);

    // Cambia l'icona dell'occhio
    if (type === "password") {
        eyeIcon.setAttribute("fill", "currentColor"); // Occhio chiuso
    } else {
        eyeIcon.setAttribute("fill", "#2575fc"); // Occhio aperto
    }
});

// Gestione del form di registrazione
document.getElementById("registerForm").addEventListener("submit", async function(event) {
    event.preventDefault(); // Previeni il comportamento predefinito del form

    const nome = document.getElementById("nome").value;
    const cognome = document.getElementById("cognome").value;
    const telefono = document.getElementById("telefono").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const errorMessage = document.getElementById("error-message");
    const successMessage = document.getElementById("success-message");

    // Resetta i messaggi
    errorMessage.style.display = "none";
    successMessage.style.display = "none";

    try {
        const response = await fetch("/register", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `nome=${encodeURIComponent(nome)}&cognome=${encodeURIComponent(cognome)}&telefono=${encodeURIComponent(telefono)}&email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
        });

        if (response.status === 201) {
            successMessage.textContent = "Registrazione completata con successo!";
            successMessage.style.display = "block";
        } else if (response.status === 400) {
            errorMessage.textContent = "Email già registrata.";
            errorMessage.style.display = "block";
        } else {
            errorMessage.textContent = "Errore durante la registrazione.";
            errorMessage.style.display = "block";
        }
    } catch (error) {
        errorMessage.textContent = "Errore durante la richiesta.";
        errorMessage.style.display = "block";
        console.error("Errore:", error);
    }
});