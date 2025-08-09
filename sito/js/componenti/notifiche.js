function showNotification(message, type) {
    // Rimuovi notifiche precedenti
    const existingNotifications = document.querySelectorAll('.notification');
    existingNotifications.forEach(notif => notif.remove());
    
    // Crea la notifica
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <span class="notification-icon">${type === 'success' ? '✅' : '❌'}</span>
            <span class="notification-message">${message}</span>
            <button class="notification-close">&times;</button>
        </div>
    `;
    
    // Aggiungi al body
    document.body.appendChild(notification);
    
    // Animazione di entrata
    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    
    // Rimuovi automaticamente dopo 3 secondi
    setTimeout(() => {
        hideNotification(notification);
    }, 3000);
    
    // Event listener per chiudere manualmente
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