// Variabili globali
let incomeCategories = [];
let expenseCategories = [];
let currentCategory = null;

// Inizializzazione quando il DOM è caricato
document.addEventListener("DOMContentLoaded", function() {
    fetchCategories();
    setupEventListeners();
    setupSidebar();
});

// Setup listeners per tutti gli eventi
function setupEventListeners() {
    // Bottone aggiungi categoria
    document.getElementById('addCategoryBtn').addEventListener('click', openAddCategoryModal);
    
    // Form submissions
    document.getElementById('addCategoryForm').addEventListener('submit', handleAddCategorySubmit);
    document.getElementById('editCategoryForm').addEventListener('submit', handleEditCategorySubmit);
    
    // Bottoni annulla
    document.getElementById('cancelAddCategory').addEventListener('click', closeAddCategoryModal);
    document.getElementById('cancelEditCategory').addEventListener('click', closeEditCategoryModal);
    document.getElementById('cancelDeleteCategory').addEventListener('click', closeDeleteCategoryModal);
    
    // Menu contestuale
    document.getElementById('editCategoryOption').addEventListener('click', handleEditCategoryOption);
    document.getElementById('deleteCategoryOption').addEventListener('click', handleDeleteCategoryOption);
    document.getElementById('confirmDeleteCategory').addEventListener('click', handleConfirmDelete);
    
    // Chiudi modali e menu contestuale
    setupModalCloseListeners();
    setupContextMenuListeners();
}

// Funzione per recuperare tutte le categorie
async function fetchCategories() {
    try {
        showLoading();
        
        // Recupera entrambe le tipologie
        const [incomeResponse, expenseResponse] = await Promise.all([
            fetch("/api/categorie/guadagno"),
            fetch("/api/categorie/spesa")
        ]);
        
        if (!incomeResponse.ok || !expenseResponse.ok) {
            throw new Error("Errore durante il recupero delle categorie");
        }

        incomeCategories = await incomeResponse.json();
        expenseCategories = await expenseResponse.json();
        
        renderCategories();
        hideLoading();
        
    } catch (error) {
        console.error("Errore:", error);
        showNotification('Errore durante il caricamento delle categorie', 'error');
        hideLoading();
    }
}

// Renderizza le categorie nell'interfaccia
function renderCategories() {
    renderIncomeCategories();
    renderExpenseCategories();
}

// Renderizza categorie di guadagno
function renderIncomeCategories() {
    const container = document.getElementById("incomeCategories");
    container.innerHTML = "";
    
    if (incomeCategories.length === 0) {
        const emptyMessage = document.createElement("li");
        emptyMessage.className = "empty-categories";
        emptyMessage.textContent = "Nessuna categoria di guadagno disponibile";
        container.appendChild(emptyMessage);
        return;
    }
    
    incomeCategories.forEach(categoria => {
        const li = document.createElement("li");
        li.setAttribute('data-category-id', categoria.id);
        li.setAttribute('data-category-type', 'Guadagno');
        
        li.innerHTML = `
            <div class="category-info">
                <div class="category-icon income">G</div>
                <div class="category-details">
                    <h3>${categoria.nome}</h3>
                    <p>Categoria di Guadagno</p>
                </div>
            </div>
            ${categoria.nome !== "Guadagno" && categoria.nome !== "Spesa" ? 
                '<button class="category-menu-btn" data-category-id="' + categoria.id + '" data-category-name="' + categoria.nome + '" data-category-type="Guadagno">⋮</button>' : 
                ''
            }
        `;
        
        container.appendChild(li);
    });
}

// Renderizza categorie di spesa
function renderExpenseCategories() {
    const container = document.getElementById("expenseCategories");
    container.innerHTML = "";
    
    if (expenseCategories.length === 0) {
        const emptyMessage = document.createElement("li");
        emptyMessage.className = "empty-categories";
        emptyMessage.textContent = "Nessuna categoria di spesa disponibile";
        container.appendChild(emptyMessage);
        return;
    }
    
    expenseCategories.forEach(categoria => {
        const li = document.createElement("li");
        li.setAttribute('data-category-id', categoria.id);
        li.setAttribute('data-category-type', 'Spesa');
        
        li.innerHTML = `
            <div class="category-info">
                <div class="category-icon expense">S</div>
                <div class="category-details">
                    <h3>${categoria.nome}</h3>
                    <p>Categoria di Spesa</p>
                </div>
            </div>
            ${categoria.nome !== "Guadagno" && categoria.nome !== "Spesa" ? 
                '<button class="category-menu-btn" data-category-id="' + categoria.id + '" data-category-name="' + categoria.nome + '" data-category-type="Spesa">⋮</button>' : 
                ''
            }
        `;
        
        container.appendChild(li);
    });
}

// Setup menu contestuale
function setupContextMenuListeners() {
    // Click sui pulsanti menu
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('category-menu-btn')) {
            e.preventDefault();
            e.stopPropagation();
            
            currentCategory = {
                id: parseInt(e.target.dataset.categoryId),
                name: e.target.dataset.categoryName,
                type: e.target.dataset.categoryType
            };
            
            showContextMenu(e);
        } else {
            hideContextMenu();
        }
    });
    
    // Chiudi menu con ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            hideContextMenu();
        }
    });
}

function showContextMenu(event) {
    const menu = document.getElementById('contextMenu');
    const rect = event.target.getBoundingClientRect();
    
    menu.style.left = rect.left + 'px';
    menu.style.top = (rect.bottom + 5) + 'px';
    menu.style.display = 'block';
    
    // Assicurati che il menu sia visibile nella viewport
    const menuRect = menu.getBoundingClientRect();
    if (menuRect.right > window.innerWidth) {
        menu.style.left = (rect.right - menuRect.width) + 'px';
    }
    if (menuRect.bottom > window.innerHeight) {
        menu.style.top = (rect.top - menuRect.height - 5) + 'px';
    }
}

function hideContextMenu() {
    document.getElementById('contextMenu').style.display = 'none';
}

function handleEditCategoryOption() {
    hideContextMenu();
    if (currentCategory) {
        document.getElementById('editCategoryName').value = currentCategory.name;
        document.getElementById('editCategoryModal').style.display = 'block';
        document.getElementById('editCategoryName').focus();
        document.getElementById('editCategoryName').select();
    }
}

function handleDeleteCategoryOption() {
    hideContextMenu();
    if (currentCategory) {
        const warningText = document.getElementById('deleteWarningText');
        if (currentCategory.type === 'Spesa') {
            warningText.textContent = `Eliminando "${currentCategory.name}", tutte le transazioni associate verranno riassegnate alla categoria predefinita "Spesa".`;
        } else {
            warningText.textContent = `Eliminando "${currentCategory.name}", tutte le transazioni associate verranno riassegnate alla categoria predefinita "Guadagno".`;
        }
        document.getElementById('deleteCategoryModal').style.display = 'block';
    }
}

// === MODALE AGGIUNGI CATEGORIA ===
function openAddCategoryModal() {
    document.getElementById('addCategoryForm').reset();
    document.getElementById('addCategoryModal').style.display = 'block';
    document.getElementById('categoryName').focus();
}

function closeAddCategoryModal() {
    document.getElementById('addCategoryModal').style.display = 'none';
}

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

    if (categoryData.nome.length < 3) {
        showNotification('Il nome della categoria deve avere almeno 3 caratteri', 'error');
        return;
    }

    if (categoryData.nome.length > 20) {
        showNotification('Il nome della categoria non può superare i 20 caratteri', 'error');
        return;
    }

    if (categoryData.nome.toLowerCase() === 'guadagno') {
        showNotification('Il nome della categoria non può essere "Guadagno"', 'error');
        return;
    }

    if (categoryData.nome.toLowerCase() === 'spesa') {
        showNotification('Il nome della categoria non può essere "Spesa"', 'error');
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
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.error || 'Errore durante l\'aggiunta della categoria');
        }
        
        closeAddCategoryModal();
        await fetchCategories();
        showNotification('Categoria aggiunta con successo!', 'success');
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification(error.message || 'Errore durante l\'aggiunta della categoria', 'error');
    }
}

// === MODALE MODIFICA CATEGORIA ===
function closeEditCategoryModal() {
    document.getElementById('editCategoryModal').style.display = 'none';
    currentCategory = null;
}

async function handleEditCategorySubmit(event) {
    event.preventDefault();
    
    if (!currentCategory) {
        showNotification('Nessuna categoria selezionata', 'error');
        return;
    }
    
    const formData = new FormData(event.target);
    const nuovoNome = formData.get('nuovoNome').trim();
    
    if (!nuovoNome) {
        showNotification('Inserisci il nuovo nome della categoria', 'error');
        return;
    }

    if (nuovoNome.length < 3) {
        showNotification('Il nome della categoria deve avere almeno 3 caratteri', 'error');
        return;
    }

    if (nuovoNome.length > 20) {
        showNotification('Il nome della categoria non può superare i 20 caratteri', 'error');
        return;
    }
    
    try {
        const response = await fetch(`/api/categoria/${currentCategory.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ nuovoNome: nuovoNome })
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.error || 'Errore durante la modifica della categoria');
        }
        
        closeEditCategoryModal();
        await fetchCategories();
        showNotification('Categoria modificata con successo!', 'success');
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification(error.message || 'Errore durante la modifica della categoria', 'error');
    }
}

// === MODALE ELIMINA CATEGORIA ===
function closeDeleteCategoryModal() {
    document.getElementById('deleteCategoryModal').style.display = 'none';
    currentCategory = null;
}

async function handleConfirmDelete() {
    if (!currentCategory) {
        showNotification('Nessuna categoria selezionata', 'error');
        return;
    }
    
    try {
        const response = await fetch(`/api/categoria/${currentCategory.id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (!response.ok) {
            throw new Error(result.error || 'Errore durante l\'eliminazione della categoria');
        }
        
        closeDeleteCategoryModal();
        await fetchCategories();
        showNotification('Categoria eliminata con successo!', 'success');
        
    } catch (error) {
        console.error('Errore:', error);
        showNotification(error.message || 'Errore durante l\'eliminazione della categoria', 'error');
    }
}

// === UTILITY FUNCTIONS ===

// Setup listeners per chiudere modali
function setupModalCloseListeners() {
    // Chiudi con X
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal');
            if (modal) {
                modal.style.display = 'none';
                currentCategory = null;
            }
        });
    });
    
    // Chiudi cliccando fuori
    window.addEventListener('click', (event) => {
        if (event.target.classList.contains('modal')) {
            event.target.style.display = 'none';
            currentCategory = null;
        }
    });
    
    // Chiudi con ESC
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            const modals = document.querySelectorAll('.modal');
            modals.forEach(modal => {
                if (modal.style.display === 'block') {
                    modal.style.display = 'none';
                    currentCategory = null;
                }
            });
        }
    });
}

// Funzione per mostrare notifiche
function showNotification(message, type) {
    // Rimuovi notifiche esistenti
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
    
    // Mostra la notifica
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    
    // Nascondi automaticamente dopo 5 secondi
    setTimeout(() => {
        hideNotification(notification);
    }, 5000);
    
    // Listener per chiudere manualmente
    notification.querySelector('.notification-close').addEventListener('click', () => {
        hideNotification(notification);
    });
}

// Funzione per nascondere notifiche
function hideNotification(notification) {
    notification.classList.remove('show');
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 300);
}

// Funzioni per loading
function showLoading() {
    const incomeContainer = document.getElementById("incomeCategories");
    const expenseContainer = document.getElementById("expenseCategories");
    
    incomeContainer.innerHTML = '<li class="empty-categories">Caricamento categorie di guadagno...</li>';
    expenseContainer.innerHTML = '<li class="empty-categories">Caricamento categorie di spesa...</li>';
}

function hideLoading() {
    // Il loading viene nascosto automaticamente dal render delle categorie
}

// Gestione errori globale
window.addEventListener('unhandledrejection', function(event) {
    console.error('Errore non gestito:', event.reason);
    showNotification('Si è verificato un errore imprevisto', 'error');
});

function setupSidebar() {
    const hamburgerMenu = document.getElementById('hamburgerMenu');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    const closeSidebar = document.getElementById('closeSidebar');
    
    // Apri sidebar
    hamburgerMenu.addEventListener('click', function() {
        sidebar.classList.add('open');
        sidebarOverlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    });
    
    // Chiudi sidebar con X
    closeSidebar.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });
    
    // Chiudi sidebar cliccando overlay
    sidebarOverlay.addEventListener('click', function() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    });
    
    // Chiudi sidebar con ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            sidebarOverlay.classList.remove('show');
            document.body.style.overflow = '';
        }
    });
}