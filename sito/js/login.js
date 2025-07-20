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

// Gestione del form di login
document.getElementById("loginForm").addEventListener("submit", async function(event) {
    event.preventDefault(); // Previeni il comportamento predefinito del form

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    try {
        const response = await fetch("/login", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
        });

        if (response.status === 401) {
            const errorMessage = document.getElementById("error-message");
            errorMessage.textContent = "Credenziali non valide";
            errorMessage.style.display = "block";

            // Evidenzia i campi con un bordo rosso
            document.getElementById("email").classList.add("input-error");
            document.getElementById("password").classList.add("input-error");
        } else if (response.status === 200) {
            window.location.href = "/home";
        } else {
            console.error("Errore sconosciuto:", response.status);
        }
    } catch (error) {
        console.error("Errore durante la richiesta:", error);
    }
});