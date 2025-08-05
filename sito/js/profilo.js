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
        try {
            confirmDeleteBtn.disabled = true;
            confirmDeleteBtn.textContent = "Eliminazione...";
            const response = await fetch("/api/profilo", {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                }
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
        }
    });

    function closeDeleteModal() {
        deleteModal.style.display = "none";
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

    // Mostra modale
    changePasswordBtn.addEventListener("click", () => {
        passwordModal.style.display = "block";
        newPasswordInput.value = "";
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
    }

    // Gestione submit cambio password
    passwordForm.addEventListener("submit", async function(event) {
        event.preventDefault();
        const password = newPasswordInput.value.trim();

        // Validazione
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
                body: JSON.stringify({ password })
            });
            const result = await response.json();

            if (!response.ok) throw new Error(result.error || "Errore durante il cambio password");
            closeModal();
            showNotification("Password aggiornata con successo!", "success");
        } catch (error) {
            showNotification(error.message, "error");
        }
    });
}