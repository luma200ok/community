// =========================================
// Toast 알림 유틸리티 (alert() 대체)
// =========================================
export function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    // 다음 프레임에서 애니메이션 시작 (CSS transition 발동)
    requestAnimationFrame(() => toast.classList.add('toast-show'));

    setTimeout(() => {
        toast.classList.remove('toast-show');
        toast.addEventListener('transitionend', () => toast.remove(), { once: true });
    }, 3000);
}
