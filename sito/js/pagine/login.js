// Toogle Password
document.getElementById("togglePassword").addEventListener("click", function() {
    const passwordField = document.getElementById("password");
    const eyeIcon = document.getElementById("eyeIcon");
    const isPassword = passwordField.getAttribute("type") === "password";

    passwordField.setAttribute("type", isPassword ? "text" : "password");
    
    if (isPassword) {
        eyeIcon.innerHTML = `
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94l.94.94A15.85 15.85 0 0 0 3.59 12s3.14 7 8.41 7a9.26 9.26 0 0 0 5.94-2.07l1 1Z"></path>
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19l-.98-.98A16.5 16.5 0 0 0 22.14 12S19 5 12 5c-.69 0-1.36.07-2 .24l-1.1-1Z"></path>
            <line x1="1" y1="1" x2="23" y2="23"></line>
        `;
    } else {
        eyeIcon.innerHTML = `
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
        `;
    }
    
    this.style.transform = "scale(0.95)";
    setTimeout(() => {
        this.style.transform = "scale(1)";
    }, 150);
});

// Rimuovi errori quando l'utente inizia a digitare
document.getElementById("email").addEventListener("input", clearErrors);
document.getElementById("password").addEventListener("input", clearErrors);

function clearErrors() {
    const errorMessage = document.getElementById("error-message");
    const emailField = document.getElementById("email");
    const passwordField = document.getElementById("password");
    
    errorMessage.style.display = "none";
    emailField.classList.remove("input-error");
    passwordField.classList.remove("input-error");
}

// Bottone invio
document.getElementById("loginForm").addEventListener("submit", async function(event) {
    event.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const submitButton = document.querySelector(".login-button");
    const buttonText = document.querySelector(".button-text");
    const loadingSpinner = document.querySelector(".loading-spinner");

    if (!email || !password) {
        showError("Compila tutti i campi");
        return;
    }

    if (!isValidEmail(email)) {
        showError("Inserisci un'email valida");
        document.getElementById("email").classList.add("input-error");
        return;
    }

    submitButton.disabled = true;
    buttonText.style.opacity = "0";
    loadingSpinner.style.display = "block";

    try {
        const response = await fetch("/login", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
        });

        if (response.status === 401) {
            showError("Email o password non corretti");
            document.getElementById("email").classList.add("input-error");
            document.getElementById("password").classList.add("input-error");
        } else if (response.status === 200 || response.redirected) {
            showSuccess();
            setTimeout(() => {
                window.location.href = "/home";
            }, 1000);
        } else {
            showError("Errore del server. Riprova più tardi.");
        }
    } catch (error) {
        console.error("Errore durante la richiesta:", error);
        showError("Errore di connessione. Controlla la tua connessione internet.");
    } finally {
        if (!document.getElementById("error-message").style.display !== "none") {
            submitButton.disabled = false;
            buttonText.style.opacity = "1";
            loadingSpinner.style.display = "none";
        }
    }
});

// Messaggio di errore
function showError(message) {
    const errorMessage = document.getElementById("error-message");
    const errorText = document.getElementById("error-text");
    
    errorText.textContent = message;
    errorMessage.style.display = "flex";
    
    const submitButton = document.querySelector(".login-button");
    const buttonText = document.querySelector(".button-text");
    const loadingSpinner = document.querySelector(".loading-spinner");
    
    submitButton.disabled = false;
    buttonText.style.opacity = "1";
    loadingSpinner.style.display = "none";
}

// Messaggio di successo
function showSuccess() {
    const submitButton = document.querySelector(".login-button");
    const buttonText = document.querySelector(".button-text");
    const loadingSpinner = document.querySelector(".loading-spinner");
    
    submitButton.style.background = "linear-gradient(135deg, #10b981, #059669)";
    buttonText.textContent = "Accesso effettuato!";
    buttonText.style.opacity = "1";
    loadingSpinner.style.display = "none";
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Animazione di focus sui campi
document.querySelectorAll('input').forEach(input => {
    input.addEventListener('focus', function() {
        this.parentElement.style.transform = 'translateY(-1px)';
    });
    
    input.addEventListener('blur', function() {
        this.parentElement.style.transform = 'translateY(0)';
    });
});

document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('success') === 'utente-eliminato') {
        showNotification('Utente eliminato con successo!', 'success');
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
});