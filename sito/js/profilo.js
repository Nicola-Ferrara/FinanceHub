document.addEventListener("DOMContentLoaded", function() {
    setupSidebar();
    fetchProfilo();
    setupFormSubmission();
    setupDeleteAccount();
    setupChangePassword();
});

async function fetchProfilo() {
    try {
        const response = await fetch("/api/profilo");
        if (!response.ok) {
            throw new Error("Errore durante il recupero dei dati del profilo");
        }

        const data = await response.json();
        document.getElementById("nome").value = data.nome || "";
        document.getElementById("cognome").value = data.cognome || "";
        document.getElementById("email").value = data.email || "";
        document.getElementById("telefono").value = data.telefono || "";
        document.getElementById("dataIscrizione").value = formatDate(data.data) || "";
    } catch (error) {
        console.error("Errore:", error);
        showNotification("Errore durante il caricamento del profilo", "error");
    }
}

function setupFormSubmission() {
    const form = document.getElementById("profileForm");
    form.addEventListener("submit", async function(event) {
        event.preventDefault();

        const nome = document.getElementById("nome").value.trim();
        const cognome = document.getElementById("cognome").value.trim();
        const telefono = document.getElementById("telefono").value.trim();

        // Validazione lato client
        if (nome.length < 3 || nome.length > 30) {
            showNotification("Il nome deve essere tra 3 e 30 caratteri", "error");
            return;
        }

        if (cognome.length < 3 || cognome.length > 30) {
            showNotification("Il cognome deve essere tra 3 e 30 caratteri", "error");
            return;
        }

        if (telefono.length < 8 || telefono.length > 13 || !/^\d{8,13}$/.test(telefono)) {
            showNotification("Il telefono deve essere composto da 8 a 13 cifre", "error");
            return;
        }

        try {
            const response = await fetch("/api/profilo", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    nome,
                    cognome,
                    telefono,
                })
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || "Errore durante l'aggiornamento del profilo");
            }

            window.location.href = '/home?success=utente-modificato';
        } catch (error) {
            console.error("Errore:", error);
            showNotification(error.message, "error");
        }
    });
}

function setupDeleteAccount() {
    const deleteButton = document.getElementById("deleteAccountBtn");
    const deleteModal = document.getElementById("deleteModal");
    const closeDeleteModalBtn = document.getElementById("closeDeleteModal");
    const cancelDeleteBtn = document.getElementById("cancelDeleteBtn");
    const confirmDeleteBtn = document.getElementById("confirmDeleteBtn");

    // Mostra la modale
    deleteButton.addEventListener("click", function() {
        deleteModal.style.display = "block";
    });

    // Chiudi la modale (X o Annulla)
    closeDeleteModalBtn.addEventListener("click", closeDeleteModal);
    cancelDeleteBtn.addEventListener("click", closeDeleteModal);

    // Chiudi cliccando fuori dalla modale
    window.addEventListener("click", function(event) {
        if (event.target === deleteModal) {
            closeDeleteModal();
        }
    });

    // Conferma eliminazione
    confirmDeleteBtn.addEventListener("click", async function() {
        const deletePasswordInput = document.getElementById("deletePassword");
        const password = deletePasswordInput ? deletePasswordInput.value.trim() : "";
        if (!password) {
            showNotification("Inserisci la password per eliminare l'account", "error");
            return;
        }
        try {
            confirmDeleteBtn.disabled = true;
            confirmDeleteBtn.textContent = "Eliminazione...";
            // Verifica password prima di eliminare
            const verifyResponse = await fetch("/api/verifica-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ password })
            });
            const verifyResult = await verifyResponse.json();
            if (!verifyResponse.ok || !verifyResult.success) {
                throw new Error(verifyResult.error || "Password errata");
            }
            // Se la password è corretta, elimina l'account
            const response = await fetch("/api/profilo", {
                method: "DELETE",
                headers: { "Content-Type": "application/json" }
            });
            if (!response.ok) {
                const result = await response.json();
                throw new Error(result.error || "Errore durante l'eliminazione dell'account");
            }
            closeDeleteModal();
            window.location.href = '/login?success=utente-eliminato';
        } catch (error) {
            showNotification(error.message, "error");
        } finally {
            confirmDeleteBtn.disabled = false;
            confirmDeleteBtn.textContent = "Elimina";
            if (deletePasswordInput) deletePasswordInput.value = "";
        }
    });

    function closeDeleteModal() {
        deleteModal.style.display = "none";
    }

    const toggleDeletePassword = document.getElementById("toggleDeletePassword");
    const deletePasswordInput = document.getElementById("deletePassword");
    if (toggleDeletePassword && deletePasswordInput) {
        toggleDeletePassword.addEventListener("click", function() {
            const isPassword = deletePasswordInput.getAttribute("type") === "password";
            deletePasswordInput.setAttribute("type", isPassword ? "text" : "password");
        });
    }
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('it-IT', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

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

function hideNotification(notification) {
    notification.classList.remove('show');
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 300);
}

function setupSidebar() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    const closeSidebar = document.getElementById('closeSidebar');

    hamburgerMenu.addEventListener('click', function() {
        sidebar.classList.add('open');
        sidebarOverlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    });

    closeSidebar.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });

    sidebarOverlay.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            sidebarOverlay.classList.remove('show');
            document.body.style.overflow = '';
        }
    });
}

function setupChangePassword() {
    const changePasswordBtn = document.getElementById("changePasswordBtn");
    const passwordModal = document.getElementById("passwordModal");
    const closePasswordModal = document.getElementById("closePasswordModal");
    const cancelPasswordBtn = document.getElementById("cancelPasswordBtn");
    const passwordForm = document.getElementById("passwordForm");
    const newPasswordInput = document.getElementById("newPassword");
    const currentPasswordInput = document.getElementById("currentPassword");

    // Mostra modale
    changePasswordBtn.addEventListener("click", () => {
        passwordModal.style.display = "block";
        newPasswordInput.value = "";
        if (currentPasswordInput) currentPasswordInput.value = "";
        resetPasswordRequirements();
    });

    // Chiudi modale
    closePasswordModal.addEventListener("click", closeModal);
    cancelPasswordBtn.addEventListener("click", closeModal);

    window.addEventListener("click", function(event) {
        if (event.target === passwordModal) closeModal();
    });

    function closeModal() {
        passwordModal.style.display = "none";
        newPasswordInput.value = "";
        if (currentPasswordInput) currentPasswordInput.value = "";
        resetPasswordRequirements();
    }

    // Occhio per password attuale
    const toggleCurrentPassword = document.getElementById("toggleCurrentPassword");
    const eyeIconCurrentPassword = document.getElementById("eyeIconCurrentPassword");
    if (toggleCurrentPassword && currentPasswordInput && eyeIconCurrentPassword) {
        toggleCurrentPassword.addEventListener("click", function() {
            const isPassword = currentPasswordInput.getAttribute("type") === "password";
            currentPasswordInput.setAttribute("type", isPassword ? "text" : "password");
            if (isPassword) {
                eyeIconCurrentPassword.innerHTML = `
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94l.94.94A15.85 15.85 0 0 0 3.59 12s3.14 7 8.41 7a9.26 9.26 0 0 0 5.94-2.07l1 1Z"></path>
                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19l-.98-.98A16.5 16.5 0 0 0 22.14 12S19 5 12 5c-.69 0-1.36.07-2 .24l-1.1-1Z"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                `;
            } else {
                eyeIconCurrentPassword.innerHTML = `
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                `;
            }
            this.style.transform = "scale(0.95)";
            setTimeout(() => {
                this.style.transform = "scale(1)";
            }, 150);
        });
    }

    // Occhio per nuova password (già presente)
    const toggleNewPassword = document.getElementById("toggleNewPassword");
    const eyeIconNewPassword = document.getElementById("eyeIconNewPassword");
    if (toggleNewPassword && newPasswordInput && eyeIconNewPassword) {
        toggleNewPassword.addEventListener("click", function() {
            const isPassword = newPasswordInput.getAttribute("type") === "password";
            newPasswordInput.setAttribute("type", isPassword ? "text" : "password");
            if (isPassword) {
                eyeIconNewPassword.innerHTML = `
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94l.94.94A15.85 15.85 0 0 0 3.59 12s3.14 7 8.41 7a9.26 9.26 0 0 0 5.94-2.07l1 1Z"></path>
                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19l-.98-.98A16.5 16.5 0 0 0 22.14 12S19 5 12 5c-.69 0-1.36.07-2 .24l-1.1-1Z"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                `;
            } else {
                eyeIconNewPassword.innerHTML = `
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                `;
            }
            this.style.transform = "scale(0.95)";
            setTimeout(() => {
                this.style.transform = "scale(1)";
            }, 150);
        });
    }

    // Controllo requisiti password in tempo reale
    newPasswordInput.addEventListener("input", function() {
        const password = newPasswordInput.value;

        const lengthReq = document.getElementById("req-length-pw");
        if (password.length >= 6) {
            lengthReq.classList.add("valid");
            lengthReq.querySelector(".requirement-icon").textContent = "✅";
        } else {
            lengthReq.classList.remove("valid");
            lengthReq.querySelector(".requirement-icon").textContent = "⚪";
        }

        const uppercaseReq = document.getElementById("req-uppercase-pw");
        if (/[A-Z]/.test(password)) {
            uppercaseReq.classList.add("valid");
            uppercaseReq.querySelector(".requirement-icon").textContent = "✅";
        } else {
            uppercaseReq.classList.remove("valid");
            uppercaseReq.querySelector(".requirement-icon").textContent = "⚪";
        }

        const numberReq = document.getElementById("req-number-pw");
        if (/\d/.test(password)) {
            numberReq.classList.add("valid");
            numberReq.querySelector(".requirement-icon").textContent = "✅";
        } else {
            numberReq.classList.remove("valid");
            numberReq.querySelector(".requirement-icon").textContent = "⚪";
        }

        const specialReq = document.getElementById("req-special-pw");
        if (/[@$!%*?&]/.test(password)) {
            specialReq.classList.add("valid");
            specialReq.querySelector(".requirement-icon").textContent = "✅";
        } else {
            specialReq.classList.remove("valid");
            specialReq.querySelector(".requirement-icon").textContent = "⚪";
        }
    });

    function resetPasswordRequirements() {
        ["req-length-pw", "req-uppercase-pw", "req-number-pw", "req-special-pw"].forEach(id => {
            const el = document.getElementById(id);
            el.classList.remove("valid");
            el.querySelector(".requirement-icon").textContent = "⚪";
        });
    }

    // Gestione submit cambio password
    passwordForm.addEventListener("submit", async function(event) {
        event.preventDefault();
        const password = newPasswordInput.value.trim();
        const currentPassword = currentPasswordInput ? currentPasswordInput.value.trim() : "";

        // Validazione
        if (!currentPassword) {
            showNotification("Inserisci la password attuale", "error");
            return;
        }
        if (password.length < 6 || password.length > 30) {
            showNotification("La password deve essere tra 6 e 30 caratteri", "error");
            return;
        }
        if (!/^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).+$/.test(password)) {
            showNotification("La password deve contenere almeno una maiuscola, un numero e un carattere speciale (@$!%*?&)", "error");
            return;
        }

        try {
            const response = await fetch("/api/profilo-password", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ currentPassword, password })
            });

            // Gestione robusta della risposta
            const contentType = response.headers.get("content-type");
            let result = {};
            if (contentType && contentType.includes("application/json")) {
                result = await response.json();
            } else {
                result = { error: await response.text() };
            }

            if (!response.ok) throw new Error(result.error || "Errore durante il cambio password");
            closeModal();
            showNotification("Password aggiornata con successo!", "success");
        } catch (error) {
            showNotification(error.message, "error");
        }
    });
}

document.addEventListener("DOMContentLoaded", function() {
    // ...già presente...

    // Gestione modale conti nascosti
    const openHiddenAccountsModalBtn = document.getElementById("openHiddenAccountsModal");
    const hiddenAccountsModal = document.getElementById("hiddenAccountsModal");
    const closeHiddenAccountsModal = document.getElementById("closeHiddenAccountsModal");
    const cancelHiddenAccountsBtn = document.getElementById("cancelHiddenAccountsBtn");
    const hiddenAccountsForm = document.getElementById("hiddenAccountsForm");
    const hiddenAccountsPassword = document.getElementById("hiddenAccountsPassword");
    const toggleHiddenAccountsPassword = document.getElementById("toggleHiddenAccountsPassword");
    const eyeIconHiddenAccountsPassword = document.getElementById("eyeIconHiddenAccountsPassword");

    if (openHiddenAccountsModalBtn) {
        openHiddenAccountsModalBtn.addEventListener("click", function() {
            hiddenAccountsModal.style.display = "block";
            hiddenAccountsPassword.value = "";
        });
    }
    if (closeHiddenAccountsModal) {
        closeHiddenAccountsModal.addEventListener("click", closeModal);
    }
    if (cancelHiddenAccountsBtn) {
        cancelHiddenAccountsBtn.addEventListener("click", closeModal);
    }
    window.addEventListener("click", function(event) {
        if (event.target === hiddenAccountsModal) closeModal();
    });
    function closeModal() {
        hiddenAccountsModal.style.display = "none";
        hiddenAccountsPassword.value = "";
    }

    // Mostra/nascondi password
    if (toggleHiddenAccountsPassword && hiddenAccountsPassword && eyeIconHiddenAccountsPassword) {
        toggleHiddenAccountsPassword.addEventListener("click", function() {
            const isPassword = hiddenAccountsPassword.getAttribute("type") === "password";
            hiddenAccountsPassword.setAttribute("type", isPassword ? "text" : "password");
            // Cambia icona se vuoi
        });
    }

    // Gestione submit form
    if (hiddenAccountsForm) {
        hiddenAccountsForm.addEventListener("submit", async function(event) {
            event.preventDefault();
            const password = hiddenAccountsPassword.value.trim();
            if (!password) {
                showNotification("Inserisci la password", "error");
                return;
            }
            // Chiamata API per verifica password
            try {
                const response = await fetch("/api/verifica-password", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ password })
                });
                const result = await response.json();
                if (!response.ok || !result.success) {
                    throw new Error(result.error || "Password errata");
                }
                // Password corretta, vai alla pagina conti nascosti
                window.location.href = "/conti_nascosti";
            } catch (error) {
                showNotification(error.message, "error");
            }
        });
    }
});