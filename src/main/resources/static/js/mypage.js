// =========================================
// 마이페이지 모듈
// =========================================
import { API_BASE, fetchWithAuth, getAuthHeaders } from './api.js';
import { showToast } from './toast.js';
import { navigate } from './router.js';
import { showSection, escapeHtml } from './ui.js';

export async function showMyPage() {
    const headers = getAuthHeaders();
    if (!headers.Authorization) {
        showToast('로그인이 필요합니다.', 'error');
        navigate('login');
        return;
    }

    try {
        // 기본 정보 + 활동 내역 병렬 조회
        const [infoRes, postsRes, commentsRes, likesRes] = await Promise.all([
            fetchWithAuth(`${API_BASE}/mypage/info`, { headers }),
            fetchWithAuth(`${API_BASE}/mypage/posts?page=0&size=5`, { headers }),
            fetchWithAuth(`${API_BASE}/mypage/comments?page=0&size=5`, { headers }),
            fetchWithAuth(`${API_BASE}/mypage/likes?page=0&size=5`, { headers })
        ]);

        if (infoRes.ok) {
            const user = await infoRes.json();
            document.getElementById('my-username').innerText = user.username;
            document.getElementById('my-email').innerText    = user.email;
            document.getElementById('my-role').innerHTML     = user.role === 'ADMIN'
                ? '<span style="color:red;font-weight:bold;">👑 관리자</span>'
                : '👤 일반 회원';
        }

        if (postsRes.ok) {
            const data = await postsRes.json();
            const el = document.getElementById('my-posts-list');
            el.innerHTML = data.content.length
                ? data.content.map(p => `<div class="link-text" data-post-id="${p.id}" style="margin-bottom:5px;cursor:pointer;">• ${escapeHtml(p.title)}</div>`).join('')
                : '작성한 게시글이 없습니다.';
            el.querySelectorAll('[data-post-id]').forEach(div => {
                div.addEventListener('click', () => navigate('posts/' + div.dataset.postId));
            });
        }

        if (commentsRes.ok) {
            const data = await commentsRes.json();
            const el = document.getElementById('my-comments-list');
            el.innerHTML = data.content.length
                ? data.content.map(c => `<div data-post-id="${c.postId}" style="margin-bottom:8px;padding:10px;background:#f8f9fa;border-radius:6px;cursor:pointer;"><strong>${escapeHtml(c.content)}</strong></div>`).join('')
                : '작성한 댓글이 없습니다.';
            el.querySelectorAll('[data-post-id]').forEach(div => {
                div.addEventListener('click', () => navigate('posts/' + div.dataset.postId));
            });
        }

        if (likesRes.ok) {
            const data = await likesRes.json();
            const el = document.getElementById('my-liked-list');
            el.innerHTML = data.content.length
                ? data.content.map(p => `<div data-post-id="${p.id}" style="margin-bottom:5px;cursor:pointer;" class="link-text">❤️ ${escapeHtml(p.title)}</div>`).join('')
                : '좋아요 표시한 게시글이 없습니다.';
            el.querySelectorAll('[data-post-id]').forEach(div => {
                div.addEventListener('click', () => navigate('posts/' + div.dataset.postId));
            });
        }

        // 수정 폼 닫기
        const updateForm = document.getElementById('update-form');
        if (updateForm) updateForm.style.display = 'none';

        showSection('mypage');

    } catch (error) {
        console.error('마이페이지 로딩 에러:', error);
        showToast('데이터를 불러오는 중 오류가 발생했습니다.', 'error');
    }
}

export function toggleUpdateForm() {
    const form = document.getElementById('update-form');
    form.style.display = form.style.display === 'none' ? 'block' : 'none';
}

export async function updateUserInfo() {
    let currentPassword = document.getElementById('upd-current-pw').value;
    const newPassword   = document.getElementById('upd-new-pw').value;
    const newHintAnswer = document.getElementById('upd-new-hint').value;

    if (!currentPassword) {
        showToast('보안을 위해 현재 비밀번호를 반드시 입력해야 합니다.', 'error');
        return;
    }

    const headers = getAuthHeaders();
    headers['Content-Type'] = 'application/json';

    try {
        if (newPassword.trim()) {
            const pwRes = await fetchWithAuth(`${API_BASE}/mypage/password`, {
                method: 'PATCH', headers,
                body: JSON.stringify({ currentPassword, newPassword })
            });
            if (!pwRes.ok) throw new Error('비밀번호 수정 실패');
            currentPassword = newPassword;
        }

        if (newHintAnswer.trim()) {
            const hintRes = await fetchWithAuth(`${API_BASE}/mypage/hint`, {
                method: 'PATCH', headers,
                body: JSON.stringify({ currentPassword, newHintAnswer })
            });
            if (!hintRes.ok) throw new Error('힌트 수정 실패');
        }

        showToast('정보가 수정되었습니다! 보안을 위해 다시 로그인해 주세요.', 'success');
        ['upd-current-pw', 'upd-new-pw', 'upd-new-hint'].forEach(id => {
            document.getElementById(id).value = '';
        });

        // 동적 import로 순환 의존 방지
        const { logout } = await import('./auth.js');
        logout();

    } catch (error) {
        console.error(error);
        showToast('수정 실패: 현재 비밀번호가 틀렸거나 통신 오류가 발생했습니다.', 'error');
    }
}
