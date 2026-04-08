// =========================================
// UI 섹션 전환 유틸리티 (순환 의존성 방지용 독립 모듈)
// =========================================

/**
 * XSS 방어: innerHTML에 삽입되는 서버/사용자 데이터를 이스케이프
 */
export function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

const ALL_SECTIONS = [
    'login', 'signup', 'write', 'find-password',
    'mypage', 'edit', 'detail', 'list'
];

export function showSection(name) {
    ALL_SECTIONS.forEach(id => {
        const el = document.getElementById(`${id}-section`);
        if (el) el.style.display = 'none';
    });

    const top = document.getElementById('top-section');
    if (top) top.style.display = ['login', 'signup', 'write', 'find-password', 'mypage', 'edit'].includes(name) ? 'block' : 'none';

    const target = document.getElementById(`${name}-section`);
    if (target) target.style.display = 'block';
}

export function showList() {
    ALL_SECTIONS.forEach(id => {
        const el = document.getElementById(`${id}-section`);
        if (el) el.style.display = 'none';
    });
    const top = document.getElementById('top-section');
    if (top) top.style.display = 'none';
    const list = document.getElementById('list-section');
    if (list) list.style.display = 'block';
}
