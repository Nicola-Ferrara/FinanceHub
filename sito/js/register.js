// Gestione del pulsante Mostra/Nascondi password
document.getElementById("togglePassword").addEventListener("click", function() {
    const passwordField = document.getElementById("password");
    const eyeIcon = document.getElementById("eyeIcon");
    const isPassword = passwordField.getAttribute("type") === "password";
    
    // Mantieni le classi CSS durante il cambio
    const currentClasses = passwordField.className;
    const currentValue = passwordField.value;
    const isFocused = document.activeElement === passwordField;
    
    // Cambia il tipo del campo
    passwordField.setAttribute("type", isPassword ? "text" : "password");
    
    // Ripristina le classi e il valore
    passwordField.className = currentClasses;
    passwordField.value = currentValue;
    
    // Ripristina il focus se era presente
    if (isFocused) {
        passwordField.focus();
        passwordField.setSelectionRange(currentValue.length, currentValue.length);
    }
    
    // Cambia l'icona dell'occhio
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
    
    // Animazione del bottone
    this.style.transform = "scale(0.95)";
    setTimeout(() => {
        this.style.transform = "scale(1)";
    }, 150);
});

// Validazione password in tempo reale
document.getElementById("password").addEventListener("input", function() {
    const password = this.value;
    
    // Controlla lunghezza
    const lengthReq = document.getElementById("req-length");
    if (password.length >= 6) {
        lengthReq.classList.add("valid");
        lengthReq.querySelector(".requirement-icon").textContent = "✅";
    } else {
        lengthReq.classList.remove("valid");
        lengthReq.querySelector(".requirement-icon").textContent = "⚪";
    }
    
    // Controlla maiuscola
    const uppercaseReq = document.getElementById("req-uppercase");
    if (/[A-Z]/.test(password)) {
        uppercaseReq.classList.add("valid");
        uppercaseReq.querySelector(".requirement-icon").textContent = "✅";
    } else {
        uppercaseReq.classList.remove("valid");
        uppercaseReq.querySelector(".requirement-icon").textContent = "⚪";
    }
    
    // Controlla numero
    const numberReq = document.getElementById("req-number");
    if (/\d/.test(password)) {
        numberReq.classList.add("valid");
        numberReq.querySelector(".requirement-icon").textContent = "✅";
    } else {
        numberReq.classList.remove("valid");
        numberReq.querySelector(".requirement-icon").textContent = "⚪";
    }
    
    // Controlla carattere speciale
    const specialReq = document.getElementById("req-special");
    if (/[@$!%*?&]/.test(password)) {
        specialReq.classList.add("valid");
        specialReq.querySelector(".requirement-icon").textContent = "✅";
    } else {
        specialReq.classList.remove("valid");
        specialReq.querySelector(".requirement-icon").textContent = "⚪";
    }
    
    // Rimuovi errori quando l'utente digita
    clearErrors();
});

// Rimuovi errori quando l'utente inizia a digitare negli altri campi
document.getElementById("nome").addEventListener("input", clearErrors);
document.getElementById("cognome").addEventListener("input", clearErrors);
document.getElementById("email").addEventListener("input", clearErrors);
document.getElementById("telefono").addEventListener("input", clearErrors);

function clearErrors() {
    const errorMessage = document.getElementById("error-message");
    const successMessage = document.getElementById("success-message");
    
    errorMessage.style.display = "none";
    successMessage.style.display = "none";
    
    // Rimuovi le classi di errore da tutti i campi
    document.querySelectorAll('input').forEach(input => {
        input.classList.remove("input-error");
    });
}

// Gestione del form di registrazione
document.getElementById("registerForm").addEventListener("submit", async function(event) {
    event.preventDefault();

    const nome = document.getElementById("nome").value.trim();
    const cognome = document.getElementById("cognome").value.trim();
    const telefono = document.getElementById("telefono").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    
    const submitButton = document.querySelector(".register-button");
    const buttonText = document.querySelector(".button-text");
    const loadingSpinner = document.querySelector(".loading-spinner");

    // Validazione lato client
    let hasErrors = false;

    if (!nome) {
        showFieldError("nome", "Il nome è obbligatorio");
        hasErrors = true;
    }

    if (!cognome) {
        showFieldError("cognome", "Il cognome è obbligatorio");
        hasErrors = true;
    }

    if (!email || !isValidEmail(email)) {
        showFieldError("email", "Inserisci un'email valida");
        hasErrors = true;
    }

    if (!telefono || !/^[0-9]{10}$/.test(telefono)) {
        showFieldError("telefono", "Inserisci un numero di telefono valido (10 cifre)");
        hasErrors = true;
    }

    if (!isValidPassword(password)) {
        showFieldError("password", "La password non soddisfa tutti i requisiti");
        hasErrors = true;
    }

    if (hasErrors) {
        showError("Correggi gli errori evidenziati");
        return;
    }

    // Mostra loading
    submitButton.disabled = true;
    buttonText.style.opacity = "0";
    loadingSpinner.style.display = "block";

    try {
        const response = await fetch("/register", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: `nome=${encodeURIComponent(nome)}&cognome=${encodeURIComponent(cognome)}&telefono=${encodeURIComponent(telefono)}&email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
        });

        if (response.status === 201) {
            showSuccess("Registrazione completata con successo!");
            
            // Reset del form
            document.getElementById("registerForm").reset();
            
            // Reindirizza al login dopo 2 secondi
            setTimeout(() => {
                window.location.href = "/login";
            }, 2000);
            
        } else if (response.status === 400) {
            showError("Email già registrata. Prova con un'altra email.");
            showFieldError("email", "Questa email è già in uso");
        } else {
            showError("Errore durante la registrazione. Riprova più tardi.");
        }
    } catch (error) {
        console.error("Errore durante la richiesta:", error);
        showError("Errore di connessione. Controlla la tua connessione internet.");
    } finally {
        // Reset button state
        submitButton.disabled = false;
        buttonText.style.opacity = "1";
        loadingSpinner.style.display = "none";
    }
});

function showError(message) {
    const errorMessage = document.getElementById("error-message");
    const errorText = document.getElementById("error-text");
    const successMessage = document.getElementById("success-message");
    
    successMessage.style.display = "none";
    errorText.textContent = message;
    errorMessage.style.display = "flex";
}

function showSuccess(message) {
    const successMessage = document.getElementById("success-message");
    const successText = document.getElementById("success-text");
    const errorMessage = document.getElementById("error-message");
    const submitButton = document.querySelector(".register-button");
    const buttonText = document.querySelector(".button-text");
    
    errorMessage.style.display = "none";
    successText.textContent = message;
    successMessage.style.display = "flex";
    
    // Cambia lo stile del bottone per il successo
    submitButton.style.background = "linear-gradient(135deg, #10b981, #059669)";
    buttonText.textContent = "Account Creato!";
    buttonText.style.opacity = "1";
}

function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    field.classList.add("input-error");
    
    // Opzionalmente, potresti mostrare un tooltip o messaggio specifico
    field.setAttribute("title", message);
}

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function isValidPassword(password) {
    const hasLength = password.length >= 6;
    const hasUppercase = /[A-Z]/.test(password);
    const hasNumber = /\d/.test(password);
    const hasSpecial = /[@$!%*?&]/.test(password);
    
    return hasLength && hasUppercase && hasNumber && hasSpecial;
}

// Animazione di focus sui campi
document.querySelectorAll('input').forEach(input => {
    input.addEventListener('focus', function() {
        this.style.transform = 'translateY(-1px)';
    });
    
    input.addEventListener('blur', function() {
        this.style.transform = 'translateY(0)';
    });
});

// Formattazione automatica del numero di telefono
document.getElementById("telefono").addEventListener("input", function() {
    let value = this.value.replace(/\D/g, ''); // Rimuovi tutto tranne i numeri
    if (value.length > 10) {
        value = value.substring(0, 10); // Limita a 10 cifre
    }
    this.value = value;
});