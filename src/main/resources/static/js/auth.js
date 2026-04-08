// =========================================
// 인증 모듈: 회원가입 / 로그인 / 로그아웃 / 비밀번호 찾기
// =========================================
import { API_BASE, fetchWithAuth, getAuthHeaders, setAccessToken, clearAccessToken } from './api.js';
import { showToast } from './toast.js';
import { navigate } from './router.js';
import { showList, escapeHtml } from './ui.js';

/**
 * 헤더 우측 로그인/로그아웃 버튼 렌더링
 */
export function renderHeader() {
    const authSection = document.getElementById('auth-section');
    if (!authSection) return;

    if (getAuthHeaders().Authorization) {
        const username = escapeHtml(localStorage.getItem('username') || '회원');
        authSection.innerHTML = `
            <span style="font-size: 14px; color: #333; margin-right: 10px;"><b>${username}</b>님 환영합니다!</span>
            <button class="btn btn-outline" id="hdr-mypage" style="padding: 5px 10px; font-size: 12px;">👤 내 정보</button>
            <button class="btn btn-primary" id="hdr-write">✏️ 글쓰기</button>
            <button class="btn btn-text" id="hdr-logout">로그아웃</button>
        `;
        document.getElementById('hdr-mypage').addEventListener('click', () => navigate('mypage'));
        document.getElementById('hdr-write').addEventListener('click', () => navigate('write'));
        document.getElementById('hdr-logout').addEventListener('click', logout);
    } else {
        authSection.innerHTML = `
            <button class="btn btn-text" id="hdr-login">로그인</button>
            <button class="btn btn-primary" id="hdr-signup">회원가입</button>
        `;
        document.getElementById('hdr-login').addEventListener('click', () => navigate('login'));
        document.getElementById('hdr-signup').addEventListener('click', () => navigate('signup'));
    }
}

/**
 * 페이지 로드 시 쿠키 refreshToken으로 자동 로그인 시도
 */
export async function tryAutoLogin() {
    try {
        const res = await fetch(`${API_BASE}/users/reissue`, { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            setAccessToken(data.accessToken);
            return true;
        }
    } catch (e) { /* 쿠키 없으면 무시 */ }
    return false;
}

export async function login() {
    const btn = document.getElementById('btn-login-submit');
    if (btn) btn.disabled = true;

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showToast('아이디와 비밀번호를 입력하세요.', 'error');
        if (btn) btn.disabled = false;
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();
            setAccessToken(data.accessToken);
            localStorage.setItem('username', username);
            showToast('로그인 성공!', 'success');
            renderHeader();
            navigate('');
        } else {
            const err = await response.json();
            showToast('로그인 실패: ' + (err.message || '아이디 또는 비밀번호를 확인하세요.'), 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('서버 통신 오류가 발생했습니다.', 'error');
    } finally {
        if (btn) btn.disabled = false;
    }
}

export async function signup() {
    const username   = document.getElementById('reg-username').value.trim();
    const password   = document.getElementById('reg-password').value;
    const email      = document.getElementById('reg-email').value.trim();
    const hintAnswer = document.getElementById('reg-hint').value.trim();

    try {
        const response = await fetch(`${API_BASE}/users/signup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email, hintAnswer })
        });

        if (response.ok) {
            showToast('회원가입 성공! 로그인해 주세요.', 'success');
            ['reg-username', 'reg-password', 'reg-email', 'reg-hint']
                .forEach(id => { document.getElementById(id).value = ''; });
            navigate('login');
        } else {
            const err = await response.json();
            showToast('가입 실패: ' + (err.message || '다시 시도해 주세요.'), 'error');
        }
    } catch (error) {
        console.error(error);
    }
}

export async function logout() {
    const headers = getAuthHeaders();
    if (headers.Authorization) {
        try { await fetchWithAuth(`${API_BASE}/users/logout`, { method: 'POST' }); }
        catch (e) { /* 무시 */ }
    }
    clearAccessToken();
    localStorage.removeItem('username');
    clearAllForms();
    showToast('로그아웃 되었습니다.', 'info');
    renderHeader();
    showList();
    navigate('');
}

function clearAllForms() {
    const fields = [
        'username', 'password',                              // 로그인
        'reg-username', 'reg-password', 'reg-email', 'reg-hint', // 회원가입
        'find-username', 'find-email', 'find-hint',          // 비밀번호 찾기
        'title', 'content',                                  // 글쓰기
        'edit-title', 'edit-content',                        // 글수정
        'comment-input',                                     // 댓글
        'upd-current-pw', 'upd-new-pw', 'upd-new-hint',     // 마이페이지 수정
    ];
    fields.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
}

export async function findPassword() {
    const username   = document.getElementById('find-username').value.trim();
    const email      = document.getElementById('find-email').value.trim();
    const hintAnswer = document.getElementById('find-hint').value.trim();

    if (!username || !email || !hintAnswer) {
        showToast('모든 항목을 입력해주세요.', 'error');
        return;
    }

    showToast('이메일 발송을 요청했습니다. 잠시 기다려주세요...', 'info');

    try {
        const response = await fetch(`${API_BASE}/users/password/find`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, hintAnswer })
        });

        if (response.ok) {
            showToast('임시 비밀번호가 이메일로 발송되었습니다!', 'success');
            ['find-username', 'find-email', 'find-hint']
                .forEach(id => { document.getElementById(id).value = ''; });
            navigate('login');
        } else {
            const err = await response.json();
            showToast(err.message || '정보가 일치하지 않습니다.', 'error');
        }
    } catch (error) {
        console.error(error);
        showToast('서버 통신 중 오류가 발생했습니다.', 'error');
    }
}
