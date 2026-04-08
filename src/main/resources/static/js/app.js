// =========================================
// 앱 진입점: 라우터 + 이벤트 바인딩
// =========================================
import { renderHeader, tryAutoLogin, login, signup, logout, findPassword } from './auth.js';
import { fetchPosts, viewPost, writePost, updatePost, deletePost,
         showEditForm, cancelEdit, writeComment, toggleLike,
         handleFileSelect, prepareWriteForm } from './posts.js';
import { showMyPage, toggleUpdateForm, updateUserInfo } from './mypage.js';
import { showList, showSection } from './ui.js';
import { navigate, getRoute, parsePostId } from './router.js';
import { getAuthHeaders } from './api.js';
import { showToast } from './toast.js';

// ==========================================
// Hash 라우터
// ==========================================
async function handleRoute() {
    const route = getRoute();
    const postId = parsePostId(route);

    if (postId !== null) {
        await viewPost(postId);
        return;
    }

    switch (route) {
        case 'login':
            showSection('login');
            break;
        case 'signup':
            showSection('signup');
            break;
        case 'find-password':
            showSection('find-password');
            break;
        case 'write':
            if (!getAuthHeaders().Authorization) {
                showToast('로그인이 필요합니다.', 'error');
                navigate('login');
                return;
            }
            prepareWriteForm();
            break;
        case 'mypage':
            await showMyPage();
            break;
        default:
            showList();
            fetchPosts();
            break;
    }
}

// ==========================================
// 관리자 이스터에그 (로고 5번 클릭)
// ==========================================
let logoClickCount = 0;
let logoClickTimer = null;

function handleLogoClick() {
    showList();
    fetchPosts();
    logoClickCount++;
    if (logoClickCount === 1) {
        logoClickTimer = setTimeout(() => { logoClickCount = 0; }, 2000);
    }
    if (logoClickCount >= 5) {
        clearTimeout(logoClickTimer);
        logoClickCount = 0;
        triggerEasterEgg();
    }
}

async function triggerEasterEgg() {
    const targetUsername = prompt('🔥 [Admin Mode] 관리자로 승급시킬 유저의 아이디를 입력하세요:');
    if (!targetUsername) return;
    const secretKey = prompt('🔥 [Admin Mode] 시크릿 키를 입력하세요:');
    if (!secretKey) return;
    try {
        const { API_BASE, fetchWithAuth: fwa, getAuthHeaders: gah } = await import('./api.js');
        const headers = gah();
        const response = await fetch(
            `${API_BASE}/users/promote?username=${encodeURIComponent(targetUsername)}&secretKey=${encodeURIComponent(secretKey)}`,
            { method: 'POST', headers }
        );
        if (response.ok) {
            const msg = await response.text();
            showToast('🎉 ' + msg, 'success');
            setTimeout(() => location.reload(), 1500);
        } else {
            showToast('권한이 없거나 잘못된 입력입니다.', 'error');
        }
    } catch (error) { console.error(error); }
}

// ==========================================
// window 전역 노출 (HTML inline 핸들러 지원)
// ==========================================
window.login           = login;
window.signup          = signup;
window.logout          = logout;
window.findPassword    = findPassword;
window.fetchPosts      = fetchPosts;
window.writePost       = writePost;
window.updatePost      = updatePost;
window.deletePost      = deletePost;
window.showEditForm    = showEditForm;
window.cancelEdit      = cancelEdit;
window.writeComment    = writeComment;
window.toggleLike      = toggleLike;
window.handleFileSelect = handleFileSelect;
window.showMyPage      = showMyPage;
window.toggleUpdateForm = toggleUpdateForm;
window.updateUserInfo  = updateUserInfo;
window.handleLogoClick = handleLogoClick;
window.navigate        = navigate;
window.showList        = showList;

// 뒤로가기/앞으로가기 지원
window.addEventListener('hashchange', handleRoute);

// 세션 만료 이벤트 처리
window.addEventListener('auth:expired', () => {
    renderHeader();
    navigate('login');
});

// ==========================================
// 초기화
// ==========================================
window.addEventListener('load', async () => {
    // 쿠키의 refreshToken으로 자동 로그인 시도
    await tryAutoLogin();
    renderHeader();

    // 검색창 Enter 키 지원
    const searchInput = document.getElementById('search-keyword');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') fetchPosts();
        });
    }

    // 초기 라우트 처리
    await handleRoute();
});
